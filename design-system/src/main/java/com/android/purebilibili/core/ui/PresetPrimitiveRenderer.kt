package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.UiPreset

/**
 * Canonical decision for which preset's renderer a shared `iOS*` primitive
 * should dispatch to. Each primitive may collapse [MATERIAL3] and [MIUIX_BRIDGED]
 * onto the same code path (e.g. AdaptiveTopAppBar) or split them (e.g. SuperDialog
 * vs AlertDialog). Use this enum at the entry point of every preset-aware primitive
 * so the dispatch is testable in plain Kotlin and consistent across primitives.
 */
enum class PresetPrimitiveRenderer {
    /** Cupertino-styled custom rendering. Legacy iOS input only. */
    IOS,
    /** Material 3 native components. MATERIAL3 style. */
    MATERIAL3,
    /** Miuix native components. MIUIX style. */
    MIUIX_BRIDGED
}

fun resolvePresetPrimitiveRenderer(
    uiStyle: AppUiStyle
): PresetPrimitiveRenderer = when (uiStyle) {
    AppUiStyle.MIUIX -> PresetPrimitiveRenderer.MIUIX_BRIDGED
    AppUiStyle.MATERIAL3 -> PresetPrimitiveRenderer.MATERIAL3
}

@Composable
@ReadOnlyComposable
fun rememberPresetPrimitiveRenderer(): PresetPrimitiveRenderer =
    resolvePresetPrimitiveRenderer(LocalAppUiStyle.current)

// 兼容桥接：旧主题 pair 输入（迁移表），批 5 后仅迁移边界与测试使用。
fun resolvePresetPrimitiveRenderer(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): PresetPrimitiveRenderer = when {
    // 单向迁移：历史 iOS 值在运行时解析为默认主题 MIUIX 渲染器。
    uiPreset == UiPreset.IOS -> PresetPrimitiveRenderer.MIUIX_BRIDGED
    uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX ->
        PresetPrimitiveRenderer.MIUIX_BRIDGED
    else -> PresetPrimitiveRenderer.MATERIAL3
}
