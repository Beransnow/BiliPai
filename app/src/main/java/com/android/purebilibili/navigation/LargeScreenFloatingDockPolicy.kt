package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.LargeScreenFloatingDockPlacement

internal fun shouldUseLargeScreenFloatingSideDock(
    isLargeScreen: Boolean,
    isBottomBarFloating: Boolean,
    liquidGlassEnabled: Boolean,
    useSideNavigation: Boolean,
    placement: LargeScreenFloatingDockPlacement,
): Boolean = isLargeScreen &&
    isBottomBarFloating &&
    liquidGlassEnabled &&
    !useSideNavigation &&
    placement != LargeScreenFloatingDockPlacement.BOTTOM
