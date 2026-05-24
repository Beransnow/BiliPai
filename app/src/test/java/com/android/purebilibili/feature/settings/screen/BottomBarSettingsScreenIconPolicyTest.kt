package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppIconStyle
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomBarSettingsScreenIconPolicyTest {

    @Test
    fun bottomBarIconPolicy_resolvesUniqueIconsForEveryConfiguredTab() {
        AppIconStyle.entries.forEach { style ->
            val icons = resolveAllBottomBarTabs(UiPreset.IOS, style).map { it.icon.name }

            assertEquals(
                icons.size,
                icons.toSet().size,
                "${style.name} 底栏设置预览不允许复用图标"
            )
        }
    }

    @Test
    fun topTabIconPolicy_resolvesUniqueIconsForEveryConfiguredTab() {
        AppIconStyle.entries.forEach { style ->
            val icons = resolveAllTopTabs(UiPreset.IOS, style).map { it.icon.name }

            assertEquals(
                icons.size,
                icons.toSet().size,
                "${style.name} 顶部标签设置预览不允许复用图标"
            )
        }
    }

    @Test
    fun bottomAndTopTabIconPolicy_followSelectedIconStyle() {
        AppIconStyle.entries.forEach { style ->
            val bottomIcon = resolveBottomBarTabIcon("DYNAMIC", UiPreset.IOS, style)
            val topIcon = resolveTopTabIcon("POPULAR", UiPreset.IOS, style)

            assertTrue(bottomIcon.name.startsWith(style.name.lowercase()))
            assertTrue(topIcon.name.startsWith(style.name.lowercase()))
        }
    }
}
