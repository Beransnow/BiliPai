package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 覆盖 FRONTEND_ARCHITECTURE_THEME_SIMPLIFICATION_PLAN.md §5.2 的单向迁移表。
 * 目标：历史 iOS、缺失、非法值一律解析为 MIUIX；MD3 组合保留有效选择。
 */
class UiPresetMigrationPolicyTest {

    private fun resolveLegacy(
        rawUiPreset: Int?,
        rawAndroidNativeVariant: Int?,
    ): UiStyle = UiStyle.fromLegacyValues(rawUiPreset, rawAndroidNativeVariant)

    // --- 迁移表：旧 iOS 值 → MIUIX ---

    @Test
    fun legacyIos_withMaterial3Variant_migratesToMiuix() {
        assertEquals(
            UiStyle.MIUIX,
            resolveLegacy(UiPreset.IOS.value, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    @Test
    fun legacyIos_withMiuixVariant_migratesToMiuix() {
        assertEquals(
            UiStyle.MIUIX,
            resolveLegacy(UiPreset.IOS.value, AndroidNativeVariant.MIUIX.value)
        )
    }

    @Test
    fun legacyIos_withMissingVariant_migratesToMiuix() {
        assertEquals(UiStyle.MIUIX, resolveLegacy(UiPreset.IOS.value, null))
    }

    // --- 迁移表：MD3 组合保留有效选择 ---

    @Test
    fun legacyMd3_withMiuixVariant_keepsMiuix() {
        assertEquals(
            UiStyle.MIUIX,
            resolveLegacy(UiPreset.MD3.value, AndroidNativeVariant.MIUIX.value)
        )
    }

    @Test
    fun legacyMd3_withMaterial3Variant_keepsMaterial3() {
        assertEquals(
            UiStyle.MATERIAL3,
            resolveLegacy(UiPreset.MD3.value, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    // --- 迁移表：缺失 / 非法值 → MIUIX ---

    @Test
    fun missingBothKeys_migratesToMiuix() {
        assertEquals(UiStyle.MIUIX, resolveLegacy(null, null))
    }

    @Test
    fun missingPreset_withMaterial3Variant_migratesToMiuix() {
        assertEquals(
            UiStyle.MIUIX,
            resolveLegacy(null, AndroidNativeVariant.MATERIAL3.value)
        )
    }

    @Test
    fun invalidPresetValue_migratesToMiuix() {
        assertEquals(UiStyle.MIUIX, resolveLegacy(99, AndroidNativeVariant.MATERIAL3.value))
    }

    @Test
    fun invalidVariantValue_migratesToMiuix() {
        assertEquals(UiStyle.MIUIX, resolveLegacy(UiPreset.MD3.value, 99))
    }

    @Test
    fun removedLegacyVariantValue2_keepsMaterial3Mapping() {
        // 历史已移除的变体值 2 保留原有映射为 MATERIAL3。
        assertEquals(
            UiStyle.MATERIAL3,
            resolveLegacy(UiPreset.MD3.value, 2)
        )
    }

    // --- 运行时选择结果不得产生 iOS ---

    @Test
    fun noLegacyCombinationProducesIos() {
        val combos = listOf(
            Triple(UiPreset.IOS, AndroidNativeVariant.MATERIAL3, UiStyle.MIUIX),
            Triple(UiPreset.IOS, AndroidNativeVariant.MIUIX, UiStyle.MIUIX),
            Triple(UiPreset.MD3, AndroidNativeVariant.MATERIAL3, UiStyle.MATERIAL3),
            Triple(UiPreset.MD3, AndroidNativeVariant.MIUIX, UiStyle.MIUIX),
        )
        combos.forEach { (preset, variant, expected) ->
            assertEquals(expected, resolveUiStyle(preset, variant))
        }
        // 旧枚举仍保留用于解析历史数据，但任何组合都不再产生 iOS 运行时选择。
        assertTrue(UiStyle.entries.contains(UiStyle.IOS))
        assertEquals(0, combos.count { it.third == UiStyle.IOS })
    }

    // --- 默认值：缺失/非法兜底均指向 MIUIX 组合 ---

    @Test
    fun enumFallbacks_resolveToMiuixDefault() {
        assertEquals(UiPreset.MD3, UiPreset.fromValue(99))
        assertEquals(AndroidNativeVariant.MIUIX, AndroidNativeVariant.fromValue(99))
        assertEquals(
            UiStyle.MIUIX,
            resolveUiStyle(UiPreset.fromValue(99), AndroidNativeVariant.fromValue(99))
        )
    }
}
