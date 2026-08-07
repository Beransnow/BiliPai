package com.android.purebilibili.core.theme

import androidx.compose.material3.MotionScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AndroidNativeVariantThemePolicyTest {

    @Test
    fun miuixVariant_usesMiuixAlignedTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(BiliMiuixTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliMiuixTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun material3Variant_usesMd3TypographyInsteadOfIosScale() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(Md3Typography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(Md3Typography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
        assertFalse(typography.bodyMedium.fontSize == BiliTypography.bodyMedium.fontSize)
    }

    @Test
    fun miuixVariant_enablesSmoothRoundingAndLargerCornerScale() {
        assertTrue(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            MIUIX_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun material3Variant_keepsCompactCornerScaleWithoutSmoothRounding() {
        assertFalse(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            MD3_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
    }

    @Test
    fun material3Variant_usesExpressiveMotionScheme() {
        val motionScheme = resolveMaterialMotionScheme(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertSame(MotionScheme.expressive(), motionScheme)
        assertNotSame(MotionScheme.standard(), motionScheme)
    }

    @Test
    fun miuixAndIosVariants_keepStandardMotionScheme() {
        val miuix = resolveMaterialMotionScheme(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        val ios = resolveMaterialMotionScheme(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertSame(MotionScheme.standard(), miuix)
        assertSame(MotionScheme.standard(), ios)
    }

    @Test
    fun androidNativeVariants_exposeMaterialAndMiuixChromeTokens() {
        val material = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val miuix = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(24, material.containerCornerRadiusDp)
        assertEquals(20, miuix.containerCornerRadiusDp)
        assertTrue(material.pillCornerRadiusDp > miuix.pillCornerRadiusDp)
        assertTrue(material.selectedContainerAlpha < miuix.selectedContainerAlpha)
        assertEquals(1f, material.motionScale)
        assertEquals(1f, miuix.motionScale)
    }

    @Test
    fun dualValueStyles_resolveChromeTokensDirectly() {
        val miuix = resolveAndroidNativeChromeTokens(AppUiStyle.MIUIX)
        val material = resolveAndroidNativeChromeTokens(AppUiStyle.MATERIAL3)

        assertEquals(20, miuix.containerCornerRadiusDp)
        assertEquals(24, material.containerCornerRadiusDp)
        assertEquals(22, miuix.pillCornerRadiusDp)
        assertEquals(28, material.pillCornerRadiusDp)
        assertEquals(0.18f, miuix.selectedContainerAlpha)
        assertEquals(0.14f, material.selectedContainerAlpha)
        assertEquals(3, material.tonalSurfaceElevationDp)
        assertEquals(0, miuix.tonalSurfaceElevationDp)
        assertEquals(240, miuix.motionEmphasizedMillis)
        assertEquals(300, material.motionEmphasizedMillis)
        assertEquals(180, miuix.motionStandardMillis)
        assertEquals(200, material.motionStandardMillis)
    }

    @Test
    fun dualValueStyles_resolveRoundingAndSmoothRounding() {
        assertTrue(shouldUseMiuixSmoothRounding(AppUiStyle.MIUIX))
        assertFalse(shouldUseMiuixSmoothRounding(AppUiStyle.MATERIAL3))
        assertEquals(MIUIX_CORNER_RADIUS_SCALE, resolveCornerRadiusScale(AppUiStyle.MIUIX))
        assertEquals(MD3_CORNER_RADIUS_SCALE, resolveCornerRadiusScale(AppUiStyle.MATERIAL3))
    }
}
