package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRootCategoryPolicyTest {

    @Test
    fun `groups use explicit ownership and do not repeat a support heading`() {
        val groups = resolveSettingsRootGroups()
        assertEquals(listOf("界面", "播放与内容", "应用管理", null), groups.map { it.title })
        assertEquals(resolveSettingsRootCategoryOrder(), groups.flatMap { it.categories })
        assertEquals(listOf(SettingsRootCategory.SYSTEM_ABOUT), groups.last().categories)
    }

    @Test
    fun `filtered category lists retain their group ownership`() {
        val groups = resolveSettingsRootGroups(listOf(SettingsRootCategory.PLUGINS_EXTENSIONS))
        assertEquals("应用管理", groups.single().title)
        assertEquals(listOf(SettingsRootCategory.PLUGINS_EXTENSIONS), groups.single().categories)
    }

    @Test
    fun `mobile and tablet settings share eleven direct category entries`() {
        val expected = listOf(
            SettingsRootCategory.HOME_RECOMMENDATION,
            SettingsRootCategory.APPEARANCE_THEME,
            SettingsRootCategory.NAVIGATION_INTERACTION,
            SettingsRootCategory.PLAYBACK_QUALITY,
            SettingsRootCategory.FULLSCREEN_GESTURE,
            SettingsRootCategory.COMMENTS_CONTENT,
            SettingsRootCategory.MESSAGE_NOTIFICATION,
            SettingsRootCategory.PRIVACY_PERMISSION,
            SettingsRootCategory.STORAGE_BACKUP,
            SettingsRootCategory.PLUGINS_EXTENSIONS,
            SettingsRootCategory.SYSTEM_ABOUT,
        )

        assertEquals(expected, resolveSettingsRootCategoryOrder())
        assertEquals(expected, resolveTabletSettingsRootCategoryOrder())
    }

    @Test
    fun `root categories expose the agreed user facing titles`() {
        assertEquals(
            listOf(
                "首页与动态",
                "外观与动画",
                "导航布局",
                "播放设置",
                "全屏与手势",
                "评论与内容",
                "消息通知",
                "隐私与权限",
                "下载与备份",
                "插件中心",
                "帮助与关于",
            ),
            resolveSettingsRootCategoryOrder().map { it.title },
        )
    }

    @Test
    fun `search targets map back to their direct category`() {
        assertEquals(
            SettingsRootCategory.HOME_RECOMMENDATION,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.HOME_FEED),
        )
        assertEquals(
            SettingsRootCategory.FULLSCREEN_GESTURE,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.FULLSCREEN_GESTURE),
        )
        assertEquals(
            SettingsRootCategory.COMMENTS_CONTENT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.INTERACTION_COMMENT),
        )
        assertEquals(
            SettingsRootCategory.APPEARANCE_THEME,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.ANIMATION),
        )
        assertEquals(
            SettingsRootCategory.STORAGE_BACKUP,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.WEBDAV_BACKUP),
        )
        assertEquals(
            SettingsRootCategory.PLUGINS_EXTENSIONS,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.PLUGINS),
        )
        assertEquals(
            SettingsRootCategory.SYSTEM_ABOUT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.TELEGRAM),
        )
        assertEquals(
            SettingsRootCategory.SYSTEM_ABOUT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.TIPS),
        )
        assertEquals(
            SettingsRootCategory.SYSTEM_ABOUT,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.EXPORT_LOGS),
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy serialized categories canonicalize to visible entries`() {
        assertEquals(
            SettingsRootCategory.APPEARANCE_THEME,
            canonicalSettingsRootCategory(SettingsRootCategory.APPEARANCE_INTERACTION),
        )
        assertEquals(
            SettingsRootCategory.PLAYBACK_QUALITY,
            canonicalSettingsRootCategory(SettingsRootCategory.CONTENT_PLAYBACK),
        )
        assertEquals(
            SettingsRootCategory.PRIVACY_PERMISSION,
            canonicalSettingsRootCategory(SettingsRootCategory.PRIVACY_STORAGE),
        )
    }
}
