package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.LargeScreenFloatingDockPlacement
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppHingeOrientation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeScreenFloatingDockPolicyTest {
    @Test
    fun smartBookDockFollowsTheLastInteractedPane() {
        assertTrue(
            resolveLargeScreenFloatingDockPlacement(
                requested = LargeScreenFloatingDockPlacement.AUTO,
                foldPosture = AppFoldPosture.Book,
                hingeOrientation = AppHingeOrientation.Vertical,
                hingeCenterXPx = 900f,
                lastInteractionXPx = 420f,
                isLtr = true,
            ) == LargeScreenFloatingDockPlacement.LEFT
        )
        assertTrue(
            resolveLargeScreenFloatingDockPlacement(
                requested = LargeScreenFloatingDockPlacement.AUTO,
                foldPosture = AppFoldPosture.Book,
                hingeOrientation = AppHingeOrientation.Vertical,
                hingeCenterXPx = 900f,
                lastInteractionXPx = 1200f,
                isLtr = true,
            ) == LargeScreenFloatingDockPlacement.RIGHT
        )
    }

    @Test
    fun smartTabletopDockFallsBackToBottomToAvoidHorizontalHinge() {
        assertTrue(
            resolveLargeScreenFloatingDockPlacement(
                requested = LargeScreenFloatingDockPlacement.AUTO,
                foldPosture = AppFoldPosture.Tabletop,
                hingeOrientation = AppHingeOrientation.Horizontal,
                hingeCenterXPx = null,
                lastInteractionXPx = null,
                isLtr = true,
            ) == LargeScreenFloatingDockPlacement.BOTTOM
        )
    }

    @Test
    fun sideDockRequiresLargeScreenFloatingLiquidGlassAndNoSidebar() {
        assertTrue(
            shouldUseLargeScreenFloatingSideDock(
                isLargeScreen = true,
                isBottomBarFloating = true,
                liquidGlassEnabled = true,
                useSideNavigation = false,
                placement = LargeScreenFloatingDockPlacement.LEFT,
            )
        )
        assertFalse(
            shouldUseLargeScreenFloatingSideDock(
                isLargeScreen = false,
                isBottomBarFloating = true,
                liquidGlassEnabled = true,
                useSideNavigation = false,
                placement = LargeScreenFloatingDockPlacement.LEFT,
            )
        )
        assertFalse(
            shouldUseLargeScreenFloatingSideDock(
                isLargeScreen = true,
                isBottomBarFloating = true,
                liquidGlassEnabled = true,
                useSideNavigation = true,
                placement = LargeScreenFloatingDockPlacement.RIGHT,
            )
        )
        assertFalse(
            shouldUseLargeScreenFloatingSideDock(
                isLargeScreen = true,
                isBottomBarFloating = true,
                liquidGlassEnabled = true,
                useSideNavigation = false,
                placement = LargeScreenFloatingDockPlacement.BOTTOM,
            )
        )
    }
}
