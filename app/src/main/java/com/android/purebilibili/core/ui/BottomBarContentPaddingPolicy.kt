package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

data class BottomBarContentPaddingSpec(
    val floatingBodyHeight: Dp,
    val dockedBodyHeight: Dp,
    val floatingInset: Dp,
    val contentGap: Dp,
)

fun resolveBottomBarContentPaddingSpec(
    bottomBarLabelMode: Int,
    isTablet: Boolean,
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    hasUiSkinDecoration: Boolean,
): BottomBarContentPaddingSpec {
    // These are the actual shell extents used by the renderers. Label mode
    // changes item content, but it does not change the navigation shell's
    // occupied height.
    val floatingBodyHeight = if (hasUiSkinDecoration) 88.dp else 64.dp
    val dockedBodyHeight = when {
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX ->
            if (hasUiSkinDecoration) 88.dp else 64.dp
        else -> 80.dp
    }
    return BottomBarContentPaddingSpec(
        floatingBodyHeight = floatingBodyHeight,
        dockedBodyHeight = dockedBodyHeight,
        floatingInset = 12.dp,
        contentGap = AppSpacingTokens.Medium,
    )
}

fun resolveBottomBarContentPadding(
    navigationBarsBottom: Dp,
    reserveBottomBar: Boolean,
    isBottomBarFloating: Boolean,
    bottomBarLabelMode: Int,
    isTablet: Boolean,
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    hasUiSkinDecoration: Boolean,
    extraContentPadding: Dp = AppSpacingTokens.Small,
): Dp {
    val safeNavigationBarsBottom = navigationBarsBottom.coerceAtLeast(0.dp)
    val safeExtraContentPadding = extraContentPadding.coerceAtLeast(0.dp)
    if (!reserveBottomBar) {
        return safeNavigationBarsBottom + safeExtraContentPadding
    }

    val spec = resolveBottomBarContentPaddingSpec(
        bottomBarLabelMode = bottomBarLabelMode,
        isTablet = isTablet,
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant,
        hasUiSkinDecoration = hasUiSkinDecoration,
    )
    val barExtent = if (isBottomBarFloating) {
        spec.floatingBodyHeight + spec.floatingInset
    } else {
        spec.dockedBodyHeight
    }
    return safeNavigationBarsBottom + barExtent + spec.contentGap + safeExtraContentPadding
}
