package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.UiStyle

enum class AppCardTone {
    STANDARD,
    MUTED,
    GLASS,
}

internal enum class AppCardRenderer {
    MATERIAL_SURFACE,
    MIUIX_CARD,
}

internal enum class AppCardContainerRole {
    CARD,
    SURFACE,
    SURFACE_VARIANT,
    SURFACE_CONTAINER,
}

internal data class AppCardVisualSpec(
    val renderer: AppCardRenderer,
    val containerRole: AppCardContainerRole,
    val containerAlpha: Float,
    val borderWidthDp: Float,
    val borderAlpha: Float,
    val tonalElevationDp: Float,
    val shadowElevationDp: Float,
)

internal fun resolveAppCardVisualSpec(
    uiStyle: UiStyle,
    tone: AppCardTone,
): AppCardVisualSpec {
    if (uiStyle == UiStyle.MIUIX) {
        return AppCardVisualSpec(
            renderer = AppCardRenderer.MIUIX_CARD,
            containerRole = AppCardContainerRole.SURFACE_CONTAINER,
            containerAlpha = if (tone == AppCardTone.GLASS) 0.92f else 1f,
            borderWidthDp = 0f,
            borderAlpha = 0f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f,
        )
    }

    return when (tone) {
        AppCardTone.STANDARD -> AppCardVisualSpec(
            renderer = AppCardRenderer.MATERIAL_SURFACE,
            containerRole = AppCardContainerRole.CARD,
            containerAlpha = 1f,
            borderWidthDp = 1f,
            borderAlpha = 1f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f,
        )

        AppCardTone.MUTED -> AppCardVisualSpec(
            renderer = AppCardRenderer.MATERIAL_SURFACE,
            containerRole = AppCardContainerRole.SURFACE_VARIANT,
            containerAlpha = 0.42f,
            borderWidthDp = 0f,
            borderAlpha = 0f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f,
        )

        AppCardTone.GLASS -> AppCardVisualSpec(
            renderer = AppCardRenderer.MATERIAL_SURFACE,
            containerRole = AppCardContainerRole.SURFACE,
            containerAlpha = 0.6f,
            borderWidthDp = 0.25f,
            borderAlpha = 0.2f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f,
        )
    }
}

/** Shared content-card decisions for feed / search / dynamic list shells. */
data class ContentCardSurfaceSpec(
    val useMiuixTokens: Boolean,
    val cornerLevel: ContainerLevel,
    val borderWidthDp: Float,
    val borderAlpha: Float,
    val tonalElevationDp: Float,
    val shadowElevationDp: Float
)

fun resolveContentCardSurfaceSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): ContentCardSurfaceSpec {
    val useMiuix = isNativeMiuixEnabled(uiPreset, androidNativeVariant)
    return if (useMiuix) {
        ContentCardSurfaceSpec(
            useMiuixTokens = true,
            cornerLevel = ContainerLevel.Card,
            borderWidthDp = 0.8f,
            borderAlpha = 0.22f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f
        )
    } else {
        ContentCardSurfaceSpec(
            useMiuixTokens = false,
            cornerLevel = ContainerLevel.Card,
            borderWidthDp = 0f,
            borderAlpha = 0f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f
        )
    }
}

fun resolveContentCardCornerDp(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Dp = AppShapes.resolveContainerCornerDp(
    level = ContainerLevel.Card,
    uiPreset = uiPreset,
    androidNativeVariant = androidNativeVariant
)
