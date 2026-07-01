package com.android.purebilibili.feature.search

import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens

internal enum class SearchResultCardSurfaceStyle {
    GLASS,
    PLAIN
}

internal data class SearchVideoCardAppearance(
    val glassEnabled: Boolean,
    val blurEnabled: Boolean,
    val showCoverGlassBadges: Boolean,
    val showInfoGlassBadges: Boolean
)

internal data class SearchResultCardAppearance(
    val surfaceStyle: SearchResultCardSurfaceStyle,
    val containerAlpha: Float,
    val borderAlpha: Float,
    val tonalElevationDp: Int,
    val shadowElevationDp: Int
)

internal fun resolveSearchCardBlurEnabled(
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean
): Boolean = headerBlurEnabled || bottomBarBlurEnabled

internal fun resolveSearchVideoCardAppearance(
    liquidGlassEnabled: Boolean,
    blurEnabled: Boolean,
    showHomeCoverGlassBadges: Boolean,
    showHomeInfoGlassBadges: Boolean,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeLiquidGlassEnabled: Boolean = false
): SearchVideoCardAppearance {
    val effectiveLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = liquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    )
    return SearchVideoCardAppearance(
        glassEnabled = effectiveLiquidGlassEnabled,
        blurEnabled = blurEnabled,
        showCoverGlassBadges = false,
        showInfoGlassBadges = false
    )
}

internal fun resolveSearchResultCardAppearance(
    liquidGlassEnabled: Boolean,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeLiquidGlassEnabled: Boolean = false,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): SearchResultCardAppearance {
    val effectiveLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = liquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    )
    val chromeTokens = resolveAndroidNativeChromeTokens(uiPreset, androidNativeVariant)
    val md3TonalElevationDp = chromeTokens.tonalSurfaceElevationDp
    return if (effectiveLiquidGlassEnabled && uiPreset == UiPreset.MD3) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.96f,
            borderAlpha = 0f,
            tonalElevationDp = md3TonalElevationDp,
            shadowElevationDp = 0
        )
    } else if (effectiveLiquidGlassEnabled) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.92f,
            borderAlpha = 0.12f,
            tonalElevationDp = 0,
            shadowElevationDp = 0
        )
    } else if (uiPreset == UiPreset.MD3) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = md3TonalElevationDp,
            shadowElevationDp = 0
        )
    } else {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = chromeTokens.tonalSurfaceElevationDp,
            shadowElevationDp = 1
        )
    }
}
