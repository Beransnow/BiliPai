package com.android.purebilibili.core.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.iOSSystemGray4
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import com.android.purebilibili.core.theme.toLegacyThemePair
import com.android.purebilibili.core.ui.motion.AppMotionTokens

data class AdaptiveBottomSheetVisualSpec(
    val cornerRadiusDp: Int,
    val useMaterialDragHandle: Boolean
)

internal data class AdaptiveBottomSheetMotionSpec(
    val scrimEnterDurationMillis: Int,
    val scrimExitDurationMillis: Int,
    val contentEnterFadeDurationMillis: Int,
    val contentExitFadeDurationMillis: Int
)

fun resolveAdaptiveBottomSheetVisualSpec(
    uiStyle: AppUiStyle,
): AdaptiveBottomSheetVisualSpec {
    val cornerRadiusDp = AppShapes.resolveContainerCornerDp(
        level = ContainerLevel.Pill,
        uiStyle = uiStyle,
    ).value.toInt()
    return AdaptiveBottomSheetVisualSpec(
        cornerRadiusDp = cornerRadiusDp,
        useMaterialDragHandle = true,
    )
}

internal fun resolveAdaptiveBottomSheetMotionSpec(
    uiStyle: AppUiStyle,
): AdaptiveBottomSheetMotionSpec {
    // 兼容桥接：批 5 迁移 AndroidNativeVariantThemePolicy 后删除。
    val (uiPreset, androidNativeVariant) = uiStyle.toLegacyThemePair()
    val tokens = resolveAndroidNativeChromeTokens(uiPreset, androidNativeVariant)
    return AdaptiveBottomSheetMotionSpec(
        scrimEnterDurationMillis = tokens.motionEmphasizedMillis,
        scrimExitDurationMillis = tokens.expressiveMotionDurationMillis,
        contentEnterFadeDurationMillis = tokens.motionEmphasizedMillis,
        contentExitFadeDurationMillis = tokens.expressiveMotionDurationMillis
    )
}

internal fun bottomSheetScrimEnterTransition(
    uiStyle: AppUiStyle,
): EnterTransition = fadeIn(
    AppMotionTokens.resolveBottomSheetFadeEnterSpec(uiStyle)
)

internal fun bottomSheetScrimExitTransition(
    uiStyle: AppUiStyle,
): ExitTransition = fadeOut(
    AppMotionTokens.resolveBottomSheetFadeExitSpec(uiStyle)
)

internal fun bottomSheetContentEnterTransition(
    uiStyle: AppUiStyle,
): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = AppMotionTokens.resolveBottomSheetSlideSpec(uiStyle)
    ) + fadeIn(
        AppMotionTokens.resolveBottomSheetFadeEnterSpec(uiStyle)
    )
}

internal fun bottomSheetContentExitTransition(
    uiStyle: AppUiStyle,
): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = AppMotionTokens.resolveBottomSheetSlideExitSpec(uiStyle)
    ) + fadeOut(
        AppMotionTokens.resolveBottomSheetFadeExitSpec<Float>(uiStyle)
    )
}

/**
 * iOS-style Modal Bottom Sheet wrapper.
 * Uses Material3 ModalBottomSheet but styled to match iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    presentationProgress: Float = 1f,
    dragHandle: @Composable (() -> Unit)? = { AppBottomSheetDragHandle() },
    windowInsets: androidx.compose.foundation.layout.WindowInsets = androidx.compose.material3.BottomSheetDefaults.modalWindowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    val uiStyle = LocalAppUiStyle.current
    val visualSpec = remember(uiStyle) {
        resolveAdaptiveBottomSheetVisualSpec(uiStyle)
    }
    val adaptiveSheetShape = remember(visualSpec) {
        RoundedCornerShape(
            topStart = visualSpec.cornerRadiusDp.dp,
            topEnd = visualSpec.cornerRadiusDp.dp,
        )
    }
    val sheetShape = shape ?: adaptiveSheetShape
    val progressVisual = resolveInteractiveOverlayProgressVisual(
        presentationProgress = presentationProgress,
        surfaceType = InteractiveOverlaySurfaceType.BOTTOM_SHEET,
        blurActive = true,
        maxScrimAlpha = scrimColor.alpha
    )
    val resolvedContainerColor = when (uiStyle) {
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.surfaceContainer
        AppUiStyle.MATERIAL3 -> MaterialTheme.colorScheme.surfaceContainerLow
    }.let { color ->
        color.copy(alpha = color.alpha * progressVisual.surfaceAlphaMultiplier)
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = resolvedContainerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor.copy(alpha = progressVisual.scrimAlpha),
        dragHandle = when (uiStyle) {
            AppUiStyle.MIUIX -> { { AppBottomSheetDragHandle() } }
            AppUiStyle.MATERIAL3 -> { { BottomSheetDefaults.DragHandle() } }
        },
        contentWindowInsets = { windowInsets },
        content = {
            content()
        }
    )
}

data class AppBottomSheetMotion(
    val scrimEnter: EnterTransition,
    val scrimExit: ExitTransition,
    val contentEnter: EnterTransition,
    val contentExit: ExitTransition,
    val scrimEnterDurationMillis: Int,
    val scrimExitDurationMillis: Int,
    val contentEnterFadeDurationMillis: Int,
    val contentExitFadeDurationMillis: Int,
)

@Composable
fun rememberAppBottomSheetMotion(): AppBottomSheetMotion {
    val uiStyle = LocalAppUiStyle.current
    return remember(uiStyle) {
        val motionSpec = resolveAdaptiveBottomSheetMotionSpec(uiStyle)
        AppBottomSheetMotion(
            scrimEnter = bottomSheetScrimEnterTransition(uiStyle),
            scrimExit = bottomSheetScrimExitTransition(uiStyle),
            contentEnter = bottomSheetContentEnterTransition(uiStyle),
            contentExit = bottomSheetContentExitTransition(uiStyle),
            scrimEnterDurationMillis = motionSpec.scrimEnterDurationMillis,
            scrimExitDurationMillis = motionSpec.scrimExitDurationMillis,
            contentEnterFadeDurationMillis = motionSpec.contentEnterFadeDurationMillis,
            contentExitFadeDurationMillis = motionSpec.contentExitFadeDurationMillis,
        )
    }
}

@Composable
fun AppBottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(iOSSystemGray4.copy(alpha = 0.4f))
        )
    }
}
