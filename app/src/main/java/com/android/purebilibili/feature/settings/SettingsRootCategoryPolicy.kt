package com.android.purebilibili.feature.settings

import kotlinx.serialization.Serializable

@Serializable
enum class SettingsRootCategory(
    val title: String,
    val subtitle: String,
    val searchTarget: SettingsSearchTarget,
) {
    APPEARANCE_THEME(
        title = "外观与主题",
        subtitle = "界面风格、主题、字体与图标",
        searchTarget = SettingsSearchTarget.INTERFACE_THEME,
    ),
    PLAYBACK_QUALITY(
        title = "播放与画质",
        subtitle = "解码、清晰度、字幕与播放行为",
        searchTarget = SettingsSearchTarget.PLAYBACK_QUALITY,
    ),
    HOME_RECOMMENDATION(
        title = "首页与推荐",
        subtitle = "首页展示、推荐流与动态内容",
        searchTarget = SettingsSearchTarget.HOME_FEED,
    ),
    NAVIGATION_INTERACTION(
        title = "导航与交互",
        subtitle = "底栏、顶部标签、动效与手势",
        searchTarget = SettingsSearchTarget.NAVIGATION,
    ),
    PRIVACY_PERMISSION(
        title = "隐私与权限",
        subtitle = "隐私模式、系统权限与黑名单",
        searchTarget = SettingsSearchTarget.PRIVACY_PERMISSION,
    ),
    STORAGE_BACKUP(
        title = "存储与备份",
        subtitle = "下载路径、缓存、导入导出与同步",
        searchTarget = SettingsSearchTarget.DATA_BACKUP,
    ),
    PLUGINS_EXTENSIONS(
        title = "插件与扩展",
        subtitle = "插件、扩展能力与相关数据采集",
        searchTarget = SettingsSearchTarget.PLUGINS,
    ),
    SYSTEM_ABOUT(
        title = "系统与关于",
        subtitle = "诊断、版本、更新、开源与支持",
        searchTarget = SettingsSearchTarget.DIAGNOSTICS,
    ),

    @Deprecated("仅用于恢复旧版本保存的设置导航状态")
    APPEARANCE_INTERACTION(
        title = "外观与主题",
        subtitle = "界面风格、主题、字体与图标",
        searchTarget = SettingsSearchTarget.INTERFACE_THEME,
    ),

    @Deprecated("仅用于恢复旧版本保存的设置导航状态")
    CONTENT_PLAYBACK(
        title = "播放与画质",
        subtitle = "解码、清晰度、字幕与播放行为",
        searchTarget = SettingsSearchTarget.PLAYBACK_QUALITY,
    ),

    @Deprecated("仅用于恢复旧版本保存的设置导航状态")
    PRIVACY_STORAGE(
        title = "隐私与权限",
        subtitle = "隐私模式、系统权限与黑名单",
        searchTarget = SettingsSearchTarget.PRIVACY_PERMISSION,
    ),
}

internal fun canonicalSettingsRootCategory(category: SettingsRootCategory): SettingsRootCategory =
    when (category) {
        SettingsRootCategory.APPEARANCE_INTERACTION -> SettingsRootCategory.APPEARANCE_THEME
        SettingsRootCategory.CONTENT_PLAYBACK -> SettingsRootCategory.PLAYBACK_QUALITY
        SettingsRootCategory.PRIVACY_STORAGE -> SettingsRootCategory.PRIVACY_PERMISSION
        else -> category
    }

internal fun resolveSettingsRootCategoryOrder(): List<SettingsRootCategory> = listOf(
    SettingsRootCategory.APPEARANCE_THEME,
    SettingsRootCategory.PLAYBACK_QUALITY,
    SettingsRootCategory.HOME_RECOMMENDATION,
    SettingsRootCategory.NAVIGATION_INTERACTION,
    SettingsRootCategory.PRIVACY_PERMISSION,
    SettingsRootCategory.STORAGE_BACKUP,
    SettingsRootCategory.PLUGINS_EXTENSIONS,
    SettingsRootCategory.SYSTEM_ABOUT,
)

internal fun resolveTabletSettingsRootCategoryOrder(): List<SettingsRootCategory> =
    resolveSettingsRootCategoryOrder()

internal fun resolveSettingsRootCategoryForSearchTarget(
    target: SettingsSearchTarget,
): SettingsRootCategory? = when (target) {
    SettingsSearchTarget.INTERFACE_THEME,
    SettingsSearchTarget.APPEARANCE -> SettingsRootCategory.APPEARANCE_THEME

    SettingsSearchTarget.PLAYBACK_QUALITY,
    SettingsSearchTarget.PLAYBACK,
    SettingsSearchTarget.FULLSCREEN_GESTURE -> SettingsRootCategory.PLAYBACK_QUALITY

    SettingsSearchTarget.HOME_FEED,
    SettingsSearchTarget.INTERACTION_COMMENT -> SettingsRootCategory.HOME_RECOMMENDATION

    SettingsSearchTarget.ANIMATION,
    SettingsSearchTarget.NAVIGATION,
    SettingsSearchTarget.BOTTOM_BAR -> SettingsRootCategory.NAVIGATION_INTERACTION

    SettingsSearchTarget.PRIVACY_PERMISSION,
    SettingsSearchTarget.PERMISSION,
    SettingsSearchTarget.BLOCKED_LIST -> SettingsRootCategory.PRIVACY_PERMISSION

    SettingsSearchTarget.DATA_BACKUP,
    SettingsSearchTarget.SETTINGS_SHARE,
    SettingsSearchTarget.WEBDAV_BACKUP,
    SettingsSearchTarget.DOWNLOAD_PATH,
    SettingsSearchTarget.IMAGE_SAVE_PATH,
    SettingsSearchTarget.CLEAR_CACHE -> SettingsRootCategory.STORAGE_BACKUP

    SettingsSearchTarget.PLUGINS,
    SettingsSearchTarget.TIPS,
    SettingsSearchTarget.OPEN_LINKS -> SettingsRootCategory.PLUGINS_EXTENSIONS

    SettingsSearchTarget.DIAGNOSTICS,
    SettingsSearchTarget.EXPORT_LOGS,
    SettingsSearchTarget.ABOUT_SUPPORT,
    SettingsSearchTarget.OPEN_SOURCE_LICENSES,
    SettingsSearchTarget.OPEN_SOURCE_HOME,
    SettingsSearchTarget.CHECK_UPDATE,
    SettingsSearchTarget.VIEW_RELEASE_NOTES,
    SettingsSearchTarget.REPLAY_ONBOARDING,
    SettingsSearchTarget.DISCLAIMER,
    SettingsSearchTarget.TELEGRAM,
    SettingsSearchTarget.TWITTER,
    SettingsSearchTarget.DONATE -> SettingsRootCategory.SYSTEM_ABOUT
}

internal fun isSceneSettingsSearchTarget(target: SettingsSearchTarget): Boolean = target in setOf(
    SettingsSearchTarget.INTERFACE_THEME,
    SettingsSearchTarget.PLAYBACK_QUALITY,
    SettingsSearchTarget.HOME_FEED,
    SettingsSearchTarget.NAVIGATION,
    SettingsSearchTarget.PRIVACY_PERMISSION,
    SettingsSearchTarget.DATA_BACKUP,
    SettingsSearchTarget.DIAGNOSTICS,
)
