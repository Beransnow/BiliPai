package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconStylePolicyTest {

    @Test
    fun `auto style keeps ios and miuix presets unchanged and uses md3 standard for material3`() {
        // iOS/MIUIX 预设保持现状(设置图标多彩色等),不引入容器化/单色化
        assertEquals(
            AppIconStyle.AUTO,
            resolveAppIconStyle(AppIconStyle.AUTO, UiPreset.IOS, AndroidNativeVariant.MATERIAL3),
        )
        assertEquals(
            AppIconStyle.AUTO,
            resolveAppIconStyle(AppIconStyle.AUTO, UiPreset.MD3, AndroidNativeVariant.MIUIX),
        )
        // MD3 预设是本次优化对象:默认解析为官方推荐样式
        assertEquals(
            AppIconStyle.MD3_STANDARD,
            resolveAppIconStyle(AppIconStyle.AUTO, UiPreset.MD3, AndroidNativeVariant.MATERIAL3),
        )
    }

    @Test
    fun `explicit style wins over preset`() {
        assertEquals(
            AppIconStyle.MD3_STANDARD,
            resolveAppIconStyle(AppIconStyle.MD3_STANDARD, UiPreset.IOS, AndroidNativeVariant.MATERIAL3),
        )
        assertEquals(
            AppIconStyle.THEME_CONTAINER,
            resolveAppIconStyle(AppIconStyle.THEME_CONTAINER, UiPreset.MD3, AndroidNativeVariant.MATERIAL3),
        )
    }

    @Test
    fun `preference parsing falls back to auto`() {
        assertEquals(AppIconStyle.THEME_CONTAINER, resolveAppIconStylePreference("THEME_CONTAINER"))
        assertEquals(AppIconStyle.MD3_STANDARD, resolveAppIconStylePreference("MD3_STANDARD"))
        assertEquals(AppIconStyle.AUTO, resolveAppIconStylePreference(null))
        assertEquals(AppIconStyle.AUTO, resolveAppIconStylePreference("UNKNOWN_VALUE"))
    }

    @Test
    fun `md3 standard style forces material glyph family`() {
        val iosPolicy = AppSemanticVisualPolicy.Cupertino.copy(iconStyle = AppIconStyle.MD3_STANDARD)
        assertEquals(AppSemanticIconFamily.MATERIAL, iosPolicy.effectiveIconFamily)

        val themeContainerPolicy = AppSemanticVisualPolicy.Cupertino.copy(
            iconStyle = AppIconStyle.THEME_CONTAINER
        )
        assertEquals(AppSemanticIconFamily.CUPERTINO, themeContainerPolicy.effectiveIconFamily)
        assertEquals(AppSemanticIconFamily.CUPERTINO, AppSemanticVisualPolicy.Cupertino.effectiveIconFamily)
    }

    @Test
    fun `top chrome policy forces material glyph family for md3 standard`() {
        val iosChrome = resolveAppTopChromePolicy(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            iconStyle = AppIconStyle.MD3_STANDARD,
        )
        assertEquals(AppSemanticIconFamily.MATERIAL, iosChrome.effectiveIconFamily)
        assertEquals(AppIconStyle.MD3_STANDARD, iosChrome.iconStyle)

        val autoChrome = resolveAppTopChromePolicy(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
        )
        assertEquals(AppSemanticIconFamily.CUPERTINO, autoChrome.effectiveIconFamily)
    }
}
