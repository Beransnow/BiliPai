package com.android.purebilibili.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Business-facing visual style contract.
 *
 * The legacy [UiPreset] and [AndroidNativeVariant] values remain persisted as compatibility
 * mirrors until older app versions no longer need to read them.
 */
enum class UiStyle(val value: Int) {
    IOS(0),
    MATERIAL3(1),
    MIUIX(2);

    companion object {
        fun fromValueOrNull(value: Int?): UiStyle? = entries.find { it.value == value }
    }
}

data class LegacyUiStylePreferences(
    val uiPreset: UiPreset,
    val androidNativeVariant: AndroidNativeVariant,
)

/**
 * Temporary bridge for adaptive renderers that still accept the legacy two-part style model.
 * Feature code may pass this through, but style selection remains centralized here.
 */
data class RendererStyleBridge(
    val preset: UiPreset,
    val variant: AndroidNativeVariant,
)

fun UiStyle.toRendererStyleBridge(): RendererStyleBridge = when (this) {
    UiStyle.IOS -> RendererStyleBridge(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    UiStyle.MATERIAL3 -> RendererStyleBridge(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
    UiStyle.MIUIX -> RendererStyleBridge(UiPreset.MD3, AndroidNativeVariant.MIUIX)
}

fun resolveUiStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): UiStyle = when (uiPreset) {
    UiPreset.IOS -> UiStyle.IOS
    UiPreset.MD3 -> when (androidNativeVariant) {
        AndroidNativeVariant.MATERIAL3 -> UiStyle.MATERIAL3
        AndroidNativeVariant.MIUIX -> UiStyle.MIUIX
    }
}

fun resolveUiStylePreference(
    rawUiStyleValue: Int?,
    legacyUiPreset: UiPreset,
    legacyAndroidNativeVariant: AndroidNativeVariant,
): UiStyle = UiStyle.fromValueOrNull(rawUiStyleValue)
    ?: resolveUiStyle(legacyUiPreset, legacyAndroidNativeVariant)

/**
 * Converts the new style back to the legacy two-key representation.
 *
 * Selecting iOS intentionally preserves the hidden Android-native variant. This makes an
 * iOS -> older app rollback lossless even when the user previously selected MIUIX.
 */
fun resolveLegacyUiStylePreferences(
    uiStyle: UiStyle,
    currentAndroidNativeVariant: AndroidNativeVariant,
): LegacyUiStylePreferences = when (uiStyle) {
    UiStyle.IOS -> LegacyUiStylePreferences(
        uiPreset = UiPreset.IOS,
        androidNativeVariant = currentAndroidNativeVariant,
    )

    UiStyle.MATERIAL3 -> LegacyUiStylePreferences(
        uiPreset = UiPreset.MD3,
        androidNativeVariant = AndroidNativeVariant.MATERIAL3,
    )

    UiStyle.MIUIX -> LegacyUiStylePreferences(
        uiPreset = UiPreset.MD3,
        androidNativeVariant = AndroidNativeVariant.MIUIX,
    )
}

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MATERIAL3 }
