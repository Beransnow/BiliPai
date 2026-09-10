package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.LargeScreenFloatingDockPlacement
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppHingeOrientation

internal fun resolveLargeScreenFloatingDockPlacement(
    requested: LargeScreenFloatingDockPlacement,
    foldPosture: AppFoldPosture,
    hingeOrientation: AppHingeOrientation,
    hingeCenterXPx: Float?,
    lastInteractionXPx: Float?,
    isLtr: Boolean,
): LargeScreenFloatingDockPlacement {
    if (requested != LargeScreenFloatingDockPlacement.AUTO) return requested
    if (foldPosture == AppFoldPosture.Tabletop || hingeOrientation == AppHingeOrientation.Horizontal) {
        return LargeScreenFloatingDockPlacement.BOTTOM
    }
    if (
        foldPosture == AppFoldPosture.Book &&
        hingeOrientation == AppHingeOrientation.Vertical &&
        hingeCenterXPx != null &&
        lastInteractionXPx != null
    ) {
        return if (lastInteractionXPx < hingeCenterXPx) {
            LargeScreenFloatingDockPlacement.LEFT
        } else {
            LargeScreenFloatingDockPlacement.RIGHT
        }
    }
    return if (isLtr) LargeScreenFloatingDockPlacement.RIGHT
    else LargeScreenFloatingDockPlacement.LEFT
}

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
