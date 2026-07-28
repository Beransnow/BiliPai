package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.theme.UiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SideBarRendererPolicyTest {

    @Test
    fun miuixVariantRoutesToOfficialNavigationRail() {
        assertEquals(
            SideBarRenderer.MIUIX_NAVIGATION_RAIL,
            resolveSideBarRenderer(UiStyle.MIUIX)
        )
    }

    @Test
    fun materialAndIosKeepFrostedSideBar() {
        assertEquals(
            SideBarRenderer.FROSTED,
            resolveSideBarRenderer(UiStyle.MATERIAL3)
        )
        assertEquals(
            SideBarRenderer.FROSTED,
            resolveSideBarRenderer(UiStyle.IOS)
        )
    }

    @Test
    fun expandableRailOnlyOnExpandedWidthClass() {
        assertTrue(shouldUseExpandableMiuixSideBar(isExpandedWidthClass = true))
        assertFalse(shouldUseExpandableMiuixSideBar(isExpandedWidthClass = false))
    }

    @Test
    fun officialSideBarItemRequiresNoSkinBitmap() {
        assertTrue(shouldUseMiuixOfficialSideBarItem(skinIconPath = null))
        assertFalse(shouldUseMiuixOfficialSideBarItem(skinIconPath = "/skin/home.png"))
    }

    @Test
    fun homeDoubleTapMapsFromRapidSuccessiveClicks() {
        assertEquals(
            HomeSideBarClickAction.NAVIGATE,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.HOME,
                nowMs = 1_000L,
                lastHomeClickMs = 0L
            )
        )
        assertEquals(
            HomeSideBarClickAction.HOME_DOUBLE_TAP,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.HOME,
                nowMs = 1_200L,
                lastHomeClickMs = 1_000L
            )
        )
        assertEquals(
            HomeSideBarClickAction.NAVIGATE,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.DYNAMIC,
                nowMs = 1_200L,
                lastHomeClickMs = 1_000L
            )
        )
    }

    @Test
    fun frostedSideBarSourceRoutesMiuixBranchToOfficialRail() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt")
            .takeIf { it.exists() }
            ?.readText()
            ?: File("app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt").readText()

        assertTrue(source.contains("resolveSideBarRenderer("))
        assertTrue(source.contains("MiuixNavigationRail("))
        assertTrue(source.contains("MiuixNavigationRailItem("))
        assertTrue(source.contains("rememberMiuixNavigationRailState("))
        assertTrue(source.contains("shouldUseExpandableMiuixSideBar("))
        assertTrue(source.contains("resolveHomeSideBarClickAction("))
    }
}
