package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import kotlin.math.min

data class RoundedControlVisualGeometry(
    val height: Dp,
    val cornerRadius: Dp,
)

/**
 * Resolves visual geometry without treating the accessibility touch target as component height.
 *
 * [nativeMinimumHeight] is the requested visible height. A theme corner that does not fit is
 * clamped to that height instead of making the whole control taller. Touch expansion is
 * intentionally outside this policy.
 */
fun resolveRoundedControlVisualGeometry(
    preferredCornerRadius: Dp,
    nativeMinimumHeight: Dp,
    maxCornerRatio: Float = 0.3f,
): RoundedControlVisualGeometry {
    val safeCorner = preferredCornerRadius.coerceAtLeast(0.dp)
    val safeMinimumHeight = nativeMinimumHeight.coerceAtLeast(0.dp)
    val safeRatio = maxCornerRatio.coerceIn(0.15f, 0.45f)
    return RoundedControlVisualGeometry(
        height = safeMinimumHeight,
        cornerRadius = min(safeCorner.value, safeMinimumHeight.value * safeRatio).dp,
    )
}

data class AppSegmentedControlPolicy(
    val usesEmphasizedTitle: Boolean,
    val usesMaterialFallback: Boolean,
    val usesNativeTabRow: Boolean,
    val usesMaterialColorTokens: Boolean,
    /** Semantic item corner; each native renderer resolves compatible visual geometry. */
    val preferredCornerRadius: Dp,
)

internal fun resolveAppSegmentedControlPolicy(
    uiStyle: AppUiStyle,
): AppSegmentedControlPolicy {
    // Native MIUIX controls are compact chrome, not cards. Card-level corners become
    // disproportionately round once liquid glass is disabled, especially on 32-36dp rows.
    val preferred = AppShapes.resolveContainerCornerDp(
        level = ContainerLevel.Chip,
        uiStyle = uiStyle,
    )
    return when (uiStyle) {
        AppUiStyle.MIUIX -> AppSegmentedControlPolicy(
            usesEmphasizedTitle = true,
            usesMaterialFallback = true,
            usesNativeTabRow = true,
            usesMaterialColorTokens = false,
            preferredCornerRadius = preferred,
        )
        AppUiStyle.MATERIAL3 -> AppSegmentedControlPolicy(
            usesEmphasizedTitle = true,
            usesMaterialFallback = true,
            usesNativeTabRow = false,
            usesMaterialColorTokens = true,
            preferredCornerRadius = preferred,
        )
    }
}

@Composable
fun rememberAppSegmentedControlPolicy(): AppSegmentedControlPolicy {
    val uiStyle = LocalAppUiStyle.current
    return remember(uiStyle) {
        resolveAppSegmentedControlPolicy(uiStyle)
    }
}
