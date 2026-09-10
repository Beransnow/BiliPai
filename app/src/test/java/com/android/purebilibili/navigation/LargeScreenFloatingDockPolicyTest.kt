package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.LargeScreenFloatingDockPlacement
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeScreenFloatingDockPolicyTest {
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
