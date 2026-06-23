package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRootCategoryPolicyTest {

    @Test
    fun `mobile and tablet settings share scene based root category order`() {
        val expected = listOf(
            SettingsRootCategory.APPEARANCE_INTERACTION,
            SettingsRootCategory.CONTENT_PLAYBACK,
            SettingsRootCategory.PRIVACY_STORAGE,
            SettingsRootCategory.SYSTEM_ABOUT
        )

        assertEquals(expected, resolveSettingsRootCategoryOrder())
        assertEquals(resolveSettingsRootCategoryOrder(), resolveTabletSettingsRootCategoryOrder())
    }

    @Test
    fun `scene based root categories expose user facing titles`() {
        assertEquals(
            listOf(
                "外观与交互",
                "内容与播放",
                "隐私与存储",
                "系统与关于"
            ),
            resolveSettingsRootCategoryOrder().map { it.title }
        )
    }

    @Test
    fun `scene search targets map back to root categories`() {
        assertEquals(
            SettingsRootCategory.CONTENT_PLAYBACK,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.HOME_FEED)
        )
        assertEquals(
            SettingsRootCategory.APPEARANCE_INTERACTION,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.FULLSCREEN_GESTURE)
        )
        assertEquals(
            SettingsRootCategory.SYSTEM_ABOUT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.DIAGNOSTICS)
        )
        assertEquals(
            SettingsRootCategory.SYSTEM_ABOUT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.TELEGRAM)
        )
    }

}
