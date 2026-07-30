package com.android.purebilibili.core.ui.transition

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalView
import com.android.purebilibili.core.ui.adaptive.MotionTier

/**
 * Host 持有的会话级冻结景深层：画在 [NavDisplay] 下方。
 *
 * 与来源 Scene 的 dispose 解耦——SinglePane 在 HELD 卸掉首页后，预测返回仍可
 * 立刻从满糊跟手消到清晰，不必等 previousScene 重新挂 effect 再补录。
 */
@Composable
internal fun VideoCardTransitionHostDepthLayer(
    enabled: Boolean,
    snapshotHandle: VideoCardTransitionSnapshotHandle,
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    exposureProvider: () -> VideoCardTransitionExposure,
    isGestureRestoreInProgressProvider: () -> Boolean = { false },
    motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    isLightBackgroundProvider: () -> Boolean = { false },
    realtimeBlurEnabledProvider: () -> Boolean = { true },
    scaleReductionProvider: () -> Float = { VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION },
    modifier: Modifier = Modifier,
) {
    if (!enabled) return
    val view = LocalView.current
    var deviceCornerRadiusPx by remember { mutableFloatStateOf(0f) }
    SideEffect {
        deviceCornerRadiusPx = resolveDeviceDisplayCornerRadiusPx(view.rootWindowInsets)
    }
    val contentLayer = snapshotHandle.contentLayer
    val snapshotState = snapshotHandle.state

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                // Host 层无子内容；只绘制冻结快照。
                val exposure = exposureProvider()
                val motionTier = motionTierProvider()
                val realtimeBlur = realtimeBlurEnabledProvider()
                if (
                    !shouldPaintHostOwnedDepthLayer(
                        exposure = exposure,
                        hasRecordedContent = snapshotState.hasRecordedContent,
                        displayListStale = snapshotState.displayListStale,
                        motionTier = motionTier,
                        realtimeBlurEnabled = realtimeBlur,
                    )
                ) {
                    return@drawWithContent
                }
                val phase = phaseProvider()
                // SettledHidden：详情盖住时仍保持满糊，手势首帧无「先清晰再糊」跳变。
                val progress = resolveHostOwnedDepthProgress(
                    exposure = exposure,
                    liveProgress = progressProvider(),
                )
                val frame = snapshotState.frameCache.resolve(
                    progress = progress,
                    phase = phase,
                    motionTier = motionTier,
                    isLightBackground = isLightBackgroundProvider(),
                    isGestureRestoreInProgress = isGestureRestoreInProgressProvider(),
                    density = density,
                    deviceCornerRadiusPx = deviceCornerRadiusPx,
                    scaleReduction = scaleReductionProvider(),
                )
                applyVideoCardTransitionSnapshotFrame(
                    contentLayer = contentLayer,
                    snapshotState = snapshotState,
                    frame = frame,
                    canvasSize = size,
                )
                if (shouldDrawVideoCardTransitionScaleGapFill(frame.contentScale)) {
                    drawRect(
                        resolveVideoCardTransitionScaleGapFillColor(
                            isLightBackground = frame.useLightScrimTint,
                            scrimAlpha = frame.scrimAlpha,
                        )
                    )
                }
                drawLayer(contentLayer)
                VideoCardTransitionDiagnostics.onSourceLayerDrawn()
                if (frame.scrimAlpha > 0.001f) {
                    val scrimColor = if (frame.useLightScrimTint) {
                        VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                    } else {
                        Color.Black
                    }
                    drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
                }
            },
    )
}

/**
 * Host 层何时绘制：有**可用**冻结内容，且**源页当前不会画同一 GraphicsLayer**。
 *
 * - [SettledHidden] / [Restoring]：SinglePane 通常只 compose 详情，Host 在 NavDisplay
 *   下独画满糊（或回弹）层。
 * - display list 在源 dispose 后会 stale：此时禁止 Host 画空层（全黑），等源页重录。
 * - [BackPreview] / [Returning] / [Opening]：源页 effect 自己 drawLayer；Host 让位。
 */
internal fun shouldPaintHostOwnedDepthLayer(
    exposure: VideoCardTransitionExposure,
    hasRecordedContent: Boolean,
    displayListStale: Boolean = false,
    motionTier: MotionTier,
    realtimeBlurEnabled: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean {
    if (
        !isVideoCardTransitionSnapshotDrawable(
            hasRecordedContent = hasRecordedContent,
            displayListStale = displayListStale,
        )
    ) {
        return false
    }
    if (motionTier == MotionTier.Reduced) return false
    if (!realtimeBlurEnabled) return false
    if (sdkInt < Build.VERSION_CODES.S) return false
    return when (exposure) {
        VideoCardTransitionExposure.SettledHidden,
        VideoCardTransitionExposure.Restoring,
        -> true
        VideoCardTransitionExposure.BackPreview,
        VideoCardTransitionExposure.Returning,
        VideoCardTransitionExposure.Opening,
        VideoCardTransitionExposure.Idle,
        -> false
    }
}

/** SettledHidden 强制满糊；其余跟 live progress（含预测手势 1→0）。 */
internal fun resolveHostOwnedDepthProgress(
    exposure: VideoCardTransitionExposure,
    liveProgress: Float,
): Float {
    return when (exposure) {
        VideoCardTransitionExposure.SettledHidden -> 1f
        else -> liveProgress.coerceIn(0f, 1f)
    }
}

/**
 * 来源 Scene 的 DisposableEffect 是否可销毁 Host 快照。
 * Host 共享 handle 时禁止 dispose 清层，否则预测返回无糊可用。
 */
internal fun shouldInvalidateSnapshotOnSourceDispose(
    isHostOwnedSnapshot: Boolean,
): Boolean = !isHostOwnedSnapshot

/**
 * 仅 IDLE 才释放 Host 快照 / BlurEffect；SettledHidden 必须保留满糊层。
 */
internal fun shouldReleaseHostOwnedDepthLayer(
    exposure: VideoCardTransitionExposure,
): Boolean = exposure == VideoCardTransitionExposure.Idle
