package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultCardAppearancePolicyTest {

    @Test
    fun searchCardBlur_enabledWhenEitherHomeBlurToggleIsOn() {
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = true,
                bottomBarBlurEnabled = false
            )
        )
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = true
            )
        )
        assertFalse(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = false
            )
        )
    }

    @Test
    fun videoSearchAppearance_usesFlatCoverStatsForDenseGrid() {
        val appearance = resolveSearchVideoCardAppearance(
            liquidGlassEnabled = false,
            blurEnabled = true,
            showHomeCoverGlassBadges = true,
            showHomeInfoGlassBadges = true
        )

        assertFalse(appearance.glassEnabled)
        assertTrue(appearance.blurEnabled)
        assertFalse(appearance.showCoverGlassBadges)
        assertFalse(appearance.showInfoGlassBadges)
    }

    @Test
    fun genericSearchResultCard_switchesBetweenGlassAndPlainStyles() {
        val glass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true,
            uiPreset = UiPreset.IOS
        )
        val plain = resolveSearchResultCardAppearance(
            liquidGlassEnabled = false,
            uiPreset = UiPreset.IOS
        )

        assertEquals(SearchResultCardSurfaceStyle.GLASS, glass.surfaceStyle)
        assertEquals(0.92f, glass.containerAlpha)
        assertEquals(0.12f, glass.borderAlpha)
        assertEquals(0, glass.tonalElevationDp)

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, plain.surfaceStyle)
        assertEquals(1f, plain.containerAlpha)
        assertEquals(0f, plain.borderAlpha)
        assertEquals(1, plain.shadowElevationDp)
    }

    @Test
    fun md3SearchResultCard_staysPlainUnlessAndroidNativeLiquidGlassIsEnabled() {
        val md3RequestedGlass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true,
            uiPreset = UiPreset.MD3,
            androidNativeLiquidGlassEnabled = false
        )

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, md3RequestedGlass.surfaceStyle)
        assertEquals(1f, md3RequestedGlass.containerAlpha)
        assertEquals(0f, md3RequestedGlass.borderAlpha)
        assertEquals(3, md3RequestedGlass.tonalElevationDp)
    }

    @Test
    fun md3SearchResultCard_usesMoreMaterialSurfaceTuningWhenNativeLiquidGlassIsEnabled() {
        val md3Glass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true,
            uiPreset = UiPreset.MD3,
            androidNativeLiquidGlassEnabled = true
        )

        assertEquals(SearchResultCardSurfaceStyle.GLASS, md3Glass.surfaceStyle)
        assertEquals(0.96f, md3Glass.containerAlpha)
        assertEquals(0f, md3Glass.borderAlpha)
        assertEquals(3, md3Glass.tonalElevationDp)
        assertEquals(0, md3Glass.shadowElevationDp)
    }

    @Test
    fun md3VideoSearchAppearance_respectsEffectiveLiquidGlassGate() {
        val gatedOff = resolveSearchVideoCardAppearance(
            liquidGlassEnabled = true,
            blurEnabled = true,
            showHomeCoverGlassBadges = true,
            showHomeInfoGlassBadges = true,
            uiPreset = UiPreset.MD3,
            androidNativeLiquidGlassEnabled = false
        )
        val gatedOn = resolveSearchVideoCardAppearance(
            liquidGlassEnabled = true,
            blurEnabled = true,
            showHomeCoverGlassBadges = true,
            showHomeInfoGlassBadges = true,
            uiPreset = UiPreset.MD3,
            androidNativeLiquidGlassEnabled = true
        )

        assertFalse(gatedOff.glassEnabled)
        assertTrue(gatedOn.glassEnabled)
    }

    @Test
    fun md3PlainSearchResultCard_staysFlatAndMaterialWhenGlassIsDisabled() {
        val md3Plain = resolveSearchResultCardAppearance(
            liquidGlassEnabled = false,
            uiPreset = UiPreset.MD3
        )

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, md3Plain.surfaceStyle)
        assertEquals(1f, md3Plain.containerAlpha)
        assertEquals(0f, md3Plain.borderAlpha)
        assertEquals(3, md3Plain.tonalElevationDp)
        assertEquals(0, md3Plain.shadowElevationDp)
    }
}
