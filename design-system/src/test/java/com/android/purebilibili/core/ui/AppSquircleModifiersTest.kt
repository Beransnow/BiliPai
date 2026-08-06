package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSquircleModifiersTest {

    @Test
    fun squircleBackground_appliesOnlyOnMiuixVariant() {
        assertTrue(
            shouldApplyMiuixSquircleBackground(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertFalse(
            shouldApplyMiuixSquircleBackground(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertFalse(
            shouldApplyMiuixSquircleBackground(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun continuousRoundingPolicy_isNeuteredDuringMigration() {
        // 2B 迁移中：iOS 连续圆角已随单向迁移废除，批 5 删除本函数及调用方。
        assertFalse(shouldUseIosContinuousRounding(UiPreset.IOS))
        assertFalse(shouldUseIosContinuousRounding(UiPreset.MD3))
    }
}