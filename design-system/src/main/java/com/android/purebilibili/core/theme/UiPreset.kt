package com.android.purebilibili.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiPreset(val value: Int, val label: String) {
    IOS(0, "iOS"),
    MD3(1, "安卓原生");

    companion object {
        fun fromValue(value: Int): UiPreset = fromValueOrNull(value) ?: MD3

        fun fromValueOrNull(value: Int): UiPreset? = entries.find { it.value == value }
    }
}

enum class AndroidNativeVariant(val value: Int, val label: String) {
    MATERIAL3(0, "Material 3"),
    MIUIX(1, "Miuix");

    companion object {
        private const val LEGACY_REMOVED_VARIANT_VALUE = 2

        fun fromValue(value: Int): AndroidNativeVariant = fromValueOrNull(value) ?: MIUIX

        /** 历史已移除的变体值 2 仍保留其原有映射为 MATERIAL3。 */
        fun fromValueOrNull(value: Int): AndroidNativeVariant? {
            if (value == LEGACY_REMOVED_VARIANT_VALUE) return MATERIAL3
            return entries.find { it.value == value }
        }
    }
}

enum class UiStyle {
    IOS,
    MATERIAL3,
    MIUIX;

    companion object {
        /**
         * 按迁移表解析历史旧键：iOS、缺失、非法值一律得到默认主题 MIUIX；
         * 仅 MD3 + 合法变体保留有效选择。
         */
        fun fromLegacyValues(
            rawUiPreset: Int?,
            rawAndroidNativeVariant: Int?
        ): UiStyle {
            val uiPreset = rawUiPreset?.let(UiPreset::fromValueOrNull)
            val androidNativeVariant = rawAndroidNativeVariant?.let(
                AndroidNativeVariant::fromValueOrNull
            )
            return when {
                // 缺失或非法（preset 或 variant 任一）→ 默认主题 MIUIX
                uiPreset == null || androidNativeVariant == null -> UiStyle.MIUIX
                else -> resolveUiStyle(uiPreset, androidNativeVariant)
            }
        }
    }

    fun legacyWritePlan(): LegacyUiStyleWritePlan = when (this) {
        IOS -> LegacyUiStyleWritePlan(UiPreset.IOS, null)
        MATERIAL3 -> LegacyUiStyleWritePlan(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        MIUIX -> LegacyUiStyleWritePlan(UiPreset.MD3, AndroidNativeVariant.MIUIX)
    }
}

fun resolveUiStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): UiStyle = when (uiPreset) {
    // 单向迁移：历史 iOS 值不再产生 iOS 运行时选择，统一迁移为默认主题 MIUIX。
    UiPreset.IOS -> UiStyle.MIUIX
    UiPreset.MD3 -> when (androidNativeVariant) {
        AndroidNativeVariant.MATERIAL3 -> UiStyle.MATERIAL3
        AndroidNativeVariant.MIUIX -> UiStyle.MIUIX
    }
}

data class LegacyUiStyleWritePlan(
    val uiPreset: UiPreset,
    // null 表示保留旧键原值，包括旧键原本不存在的情况。
    val androidNativeVariant: AndroidNativeVariant?
)

data class UiRenderingProfile(
    val useMaterialChrome: Boolean,
    val useMaterialMotion: Boolean,
    val useMaterialIcons: Boolean
)

fun resolveUiRenderingProfile(preset: UiPreset): UiRenderingProfile {
    return when (preset) {
        UiPreset.IOS -> UiRenderingProfile(
            useMaterialChrome = false,
            useMaterialMotion = false,
            useMaterialIcons = false
        )

        UiPreset.MD3 -> UiRenderingProfile(
            useMaterialChrome = true,
            useMaterialMotion = true,
            useMaterialIcons = true
        )
    }
}

val LocalUiPreset = staticCompositionLocalOf { UiPreset.MD3 }
val LocalAndroidNativeVariant = staticCompositionLocalOf { AndroidNativeVariant.MIUIX }
val LocalDynamicColorActive = staticCompositionLocalOf { false }
val LocalSettingsLiquidGlassEnabled = staticCompositionLocalOf { false }
