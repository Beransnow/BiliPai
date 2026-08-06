package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Asserts every shared iOS* primitive exposes a preset-aware renderer decision
 * so feature screens get the right look on iOS / MD3 / Miuix without primitive
 * call sites changing. Compose UI tests would assert actual rendered nodes;
 * here we assert the policy layer that drives the dispatch.
 */
class PrimitivePresetCoverageTest {

    @Test
    fun unifiedRenderer_matches_uiPresetMatrix() {
        // 两值模型：历史 iOS 输入在运行时解析为 MIUIX 渲染器。
        assertEquals(
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
            resolvePresetPrimitiveRenderer(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            PresetPrimitiveRenderer.MATERIAL3,
            resolvePresetPrimitiveRenderer(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
            resolvePresetPrimitiveRenderer(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        )
    }

    @Test
    fun legacyLargeTitleBar_isRemovedAfterFeatureMigration() {
        val legacySource = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/iOSLargeTitleBar.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/iOSLargeTitleBar.kt"),
        )
        assertEquals(false, legacySource.any { it.exists() })
    }

    @Test
    fun dialogActionLayoutPolicy_isConstantAfterIosMigration() {
        // 2B 迁移：iOS 全宽铺满操作区行为已随单向迁移删除，布局政策收敛为常量。
        assertEquals(false, resolveDialogActionLayoutPolicy().expandToContainer)
    }

    @Test
    fun adaptiveBottomSheetVisual_branchesByLegacyCornerLevel() {
        // AppSheetComponents 仍按旧输入分支圆角等级，圆角本身走两值风格。
        val ios = resolveAdaptiveBottomSheetVisualSpec(UiPreset.IOS)
        val md3 = resolveAdaptiveBottomSheetVisualSpec(UiPreset.MD3)
        // 2B 迁移：iOS 输入并入 MIUIX（Dialog 14 * 1.15 = 16.1 → 16）。
        assertEquals(16, ios.cornerRadiusDp)
        assertEquals(28, md3.cornerRadiusDp)
        assertEquals(false, ios.useMaterialDragHandle)
        assertEquals(true, md3.useMaterialDragHandle)
    }
}
