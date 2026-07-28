package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class UiStyleCompatibilityPolicyTest {

    @Test
    fun allLegacyPairs_mapToEffectiveVisualStyle() {
        assertEquals(UiStyle.IOS, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MATERIAL3))
        assertEquals(UiStyle.IOS, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MIUIX))
        assertEquals(UiStyle.MATERIAL3, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MATERIAL3))
        assertEquals(UiStyle.MIUIX, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MIUIX))
    }

    @Test
    fun validNewPreference_winsOverConflictingLegacyPreferences() {
        assertEquals(
            UiStyle.MIUIX,
            resolveUiStylePreference(
                rawUiStyleValue = UiStyle.MIUIX.value,
                legacyUiPreset = UiPreset.IOS,
                legacyAndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
            ),
        )
    }

    @Test
    fun missingOrInvalidNewPreference_fallsBackToLegacyPreferences() {
        listOf(null, 99).forEach { rawValue ->
            assertEquals(
                UiStyle.MIUIX,
                resolveUiStylePreference(
                    rawUiStyleValue = rawValue,
                    legacyUiPreset = UiPreset.MD3,
                    legacyAndroidNativeVariant = AndroidNativeVariant.MIUIX,
                ),
            )
        }
    }

    @Test
    fun newStyles_writeExpectedLegacyMirrors() {
        assertEquals(
            LegacyUiStylePreferences(UiPreset.MD3, AndroidNativeVariant.MATERIAL3),
            resolveLegacyUiStylePreferences(UiStyle.MATERIAL3, AndroidNativeVariant.MIUIX),
        )
        assertEquals(
            LegacyUiStylePreferences(UiPreset.MD3, AndroidNativeVariant.MIUIX),
            resolveLegacyUiStylePreferences(UiStyle.MIUIX, AndroidNativeVariant.MATERIAL3),
        )
    }

    @Test
    fun iosWrite_preservesHiddenLegacyNativeVariant() {
        AndroidNativeVariant.entries.forEach { variant ->
            assertEquals(
                LegacyUiStylePreferences(UiPreset.IOS, variant),
                resolveLegacyUiStylePreferences(UiStyle.IOS, variant),
            )
        }
    }

    @Test
    fun rendererBridge_coversAllThreeEffectiveStyles() {
        assertEquals(
            RendererStyleBridge(UiPreset.IOS, AndroidNativeVariant.MATERIAL3),
            UiStyle.IOS.toRendererStyleBridge(),
        )
        assertEquals(
            RendererStyleBridge(UiPreset.MD3, AndroidNativeVariant.MATERIAL3),
            UiStyle.MATERIAL3.toRendererStyleBridge(),
        )
        assertEquals(
            RendererStyleBridge(UiPreset.MD3, AndroidNativeVariant.MIUIX),
            UiStyle.MIUIX.toRendererStyleBridge(),
        )
    }
}
