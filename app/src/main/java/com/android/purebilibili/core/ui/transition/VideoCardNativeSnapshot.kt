package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.android.purebilibili.core.util.CardPositionManager

/**
 * The flying overlay owns the clicked card. The list slot must be empty until land,
 * otherwise a frozen duplicate sits under the morph.
 */
internal fun shouldHideStationarySourceCard(
    isSharedMorphSourceCard: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    depthProgress: Float,
    isReturnGestureInProgress: Boolean,
): Boolean {
    if (!isSharedMorphSourceCard) return false
    if (isReturnGestureInProgress) return true
    return when (phase) {
        VideoCardTransitionBackgroundPhase.OPENING,
        VideoCardTransitionBackgroundPhase.HELD,
        -> true
        VideoCardTransitionBackgroundPhase.RETURNING ->
            depthProgress > 0.001f
        VideoCardTransitionBackgroundPhase.IDLE -> false
    }
}

internal fun isRecordedNativeCardSource(bvid: String): Boolean {
    val clicked = CardPositionManager.lastClickedVideoSourceKey ?: return false
    val id = bvid.trim()
    if (id.isEmpty()) return false
    return clicked == id || clicked.endsWith(":$id")
}

/**
 * Records the stationary list card into a graphics layer so a click can freeze native pixels
 * instead of reconstructing title/spacing on the flying detail entry.
 *
 * [GraphicsLayer.toImageBitmap] is suspend and cannot run from a click callback. Keep the
 * recorded layer and draw it with [androidx.compose.ui.graphics.layer.drawLayer].
 * While this card is the morph source, skip drawing at the list coordinates so the slot is empty.
 */
@Composable
internal fun Modifier.recordNativeVideoCardLayer(
    layer: GraphicsLayer,
    freeze: Boolean,
    bvid: String = "",
): Modifier {
    val bgState = LocalVideoCardTransitionBackgroundState.current
    return drawWithContent {
        if (!freeze) {
            layer.record {
                this@drawWithContent.drawContent()
            }
        }
        val hide = shouldHideStationarySourceCard(
            isSharedMorphSourceCard = isRecordedNativeCardSource(bvid),
            phase = bgState.phaseProvider(),
            depthProgress = bgState.progressProvider(),
            isReturnGestureInProgress = bgState.isReturnGestureInProgressProvider() ||
                bgState.isGestureRestoreInProgressProvider(),
        )
        if (!hide) {
            drawContent()
        }
    }
}

internal fun captureNativeVideoCardImage(
    layer: GraphicsLayer,
) {
    CardPositionManager.recordNativeCardLayer(layer)
}

internal fun captureNativeCoverOverlayLayer(
    layer: GraphicsLayer,
) {
    CardPositionManager.recordNativeCoverOverlayLayer(layer)
}

@Composable
internal fun rememberNativeVideoCardLayer() = rememberGraphicsLayer()

internal class NativeVideoCardSnapshotController(
    val modifier: Modifier,
    val coverOverlayModifier: Modifier,
    val capture: () -> Unit,
)

@Composable
internal fun rememberNativeVideoCardSnapshotController(key: Any): NativeVideoCardSnapshotController {
    val layer = rememberNativeVideoCardLayer()
    val coverOverlayLayer = rememberNativeVideoCardLayer()
    val freezeState = remember(key) { mutableStateOf(false) }
    val bvid = (key as? String).orEmpty()
    return NativeVideoCardSnapshotController(
        modifier = Modifier.recordNativeVideoCardLayer(layer, freezeState.value, bvid),
        coverOverlayModifier = Modifier.recordNativeVideoCardLayer(
            coverOverlayLayer,
            freezeState.value,
            bvid,
        ),
        capture = {
            freezeState.value = true
            captureNativeVideoCardImage(layer)
            captureNativeCoverOverlayLayer(coverOverlayLayer)
        },
    )
}
