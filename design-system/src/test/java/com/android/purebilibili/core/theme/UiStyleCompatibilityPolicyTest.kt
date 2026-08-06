package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UiStyleCompatibilityPolicyTest {

    @Test
    fun allLegacyCombinations_deriveExpectedUiStyle() {
        // 单向迁移：历史 iOS 值在运行时解析为默认主题 MIUIX，不再产生 iOS 选择。
        assertEquals(UiStyle.MIUIX, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MATERIAL3))
        assertEquals(UiStyle.MIUIX, resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MIUIX))
        assertEquals(UiStyle.MATERIAL3, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MATERIAL3))
        assertEquals(UiStyle.MIUIX, resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MIUIX))
    }

    @Test
    fun selectingIos_onlyWritesPresetAndPreservesHiddenVariant() {
        val writePlan = UiStyle.IOS.legacyWritePlan()

        assertEquals(UiPreset.IOS, writePlan.uiPreset)
        assertNull(writePlan.androidNativeVariant)
    }

    @Test
    fun selectingAndroidStyle_writesMd3PresetAndMatchingVariant() {
        assertEquals(
            LegacyUiStyleWritePlan(UiPreset.MD3, AndroidNativeVariant.MATERIAL3),
            UiStyle.MATERIAL3.legacyWritePlan()
        )
        assertEquals(
            LegacyUiStyleWritePlan(UiPreset.MD3, AndroidNativeVariant.MIUIX),
            UiStyle.MIUIX.legacyWritePlan()
        )
    }
}
