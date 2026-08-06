package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveTooltipPolicyTest {

    @Test
    fun miuixUsesOfficialTooltipBoxRenderer() {
        assertEquals(
            AdaptiveTooltipRenderer.MIUIX_TOOLTIP_BOX,
            resolveAdaptiveTooltipRenderer(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        )
    }

    @Test
    fun materialPassesThroughTooltip_andLegacyIosMigratesToMiuix() {
        assertEquals(
            AdaptiveTooltipRenderer.PASSTHROUGH,
            resolveAdaptiveTooltipRenderer(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        )
        // 单向迁移：历史 iOS 在运行时解析为默认主题 MIUIX。
        assertEquals(
            AdaptiveTooltipRenderer.MIUIX_TOOLTIP_BOX,
            resolveAdaptiveTooltipRenderer(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        )
    }
}
