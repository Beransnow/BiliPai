package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun resolveSettingsSearchNavigation(result: SettingsSearchResult): BiliPaiNavKey? {
    result.page?.let { return resolveSettingsCategoryNavKey(it) }
    if (result.target == SettingsSearchTarget.DIAGNOSTICS || result.target == SettingsSearchTarget.EXPORT_LOGS) {
        return BiliPaiNavKey.SettingsCategory(SettingsRootCategory.PLAYER_DIAGNOSTICS)
    }
    if (result.target == SettingsSearchTarget.PLAYBACK) {
        val category = when (result.focusId) {
            SettingsSearchFocusIds.PLAYBACK_DECODER -> SettingsRootCategory.VIDEO_DECODER
            SettingsSearchFocusIds.PLAYBACK_DEBUG -> SettingsRootCategory.PLAYER_DIAGNOSTICS
            SettingsSearchFocusIds.PLAYBACK_FULLSCREEN,
            SettingsSearchFocusIds.PLAYBACK_GESTURE -> SettingsRootCategory.FULLSCREEN_GESTURE
            SettingsSearchFocusIds.PLAYBACK_INTERACTION -> SettingsRootCategory.COMMENTS_CONTENT
            else -> SettingsRootCategory.PLAYBACK_QUALITY
        }
        return resolveSettingsCategoryNavKey(category)
    }
    if (result.target == SettingsSearchTarget.FULLSCREEN_GESTURE) return BiliPaiNavKey.SettingsCategory(SettingsRootCategory.FULLSCREEN_GESTURE)
    if (result.target == SettingsSearchTarget.INTERACTION_COMMENT) return BiliPaiNavKey.SettingsCategory(SettingsRootCategory.COMMENTS_CONTENT)
    resolveSettingsSceneDetailFocus(result.target)?.let { detailFocus ->
        return when (detailFocus.target) {
            SettingsSearchTarget.APPEARANCE -> BiliPaiNavKey.AppearanceSettings
            SettingsSearchTarget.HOME_FEED -> BiliPaiNavKey.HomeSettings
            SettingsSearchTarget.ANIMATION -> BiliPaiNavKey.AnimationSettings
            SettingsSearchTarget.PLAYBACK -> BiliPaiNavKey.SettingsCategory(SettingsRootCategory.PLAYBACK_QUALITY)
            SettingsSearchTarget.BOTTOM_BAR -> BiliPaiNavKey.BottomBarSettings
            else -> null
        }
    }
    if (isSceneSettingsSearchTarget(result.target)) {
        return resolveSettingsRootCategoryForSearchTarget(result.target)?.let(::resolveSettingsCategoryNavKey)
    }
    return when (result.target) {
        SettingsSearchTarget.APPEARANCE -> BiliPaiNavKey.AppearanceSettings
        SettingsSearchTarget.ANIMATION -> BiliPaiNavKey.AnimationSettings
        SettingsSearchTarget.PLAYBACK -> BiliPaiNavKey.PlaybackSettings
        SettingsSearchTarget.BOTTOM_BAR -> BiliPaiNavKey.BottomBarSettings
        SettingsSearchTarget.PERMISSION -> BiliPaiNavKey.PermissionSettings
        SettingsSearchTarget.MESSAGE_NOTIFICATION -> BiliPaiNavKey.MessageNotificationSettings
        SettingsSearchTarget.PLUGINS -> BiliPaiNavKey.PluginsSettings()
        SettingsSearchTarget.SETTINGS_SHARE -> BiliPaiNavKey.SettingsShare
        SettingsSearchTarget.WEBDAV_BACKUP -> BiliPaiNavKey.WebDavBackup
        SettingsSearchTarget.OPEN_SOURCE_LICENSES -> BiliPaiNavKey.OpenSourceLicenses
        SettingsSearchTarget.TIPS -> BiliPaiNavKey.TipsSettings
        // Leaf entries such as GitHub, update check and Telegram are actions hosted by their
        // root category. They used to resolve to null, so tapping a valid search result did
        // nothing. Open the owning category; the normal settings callbacks remain authoritative.
        else -> resolveSettingsRootCategoryForSearchTarget(result.target)
            ?.let { category -> BiliPaiNavKey.SettingsCategory(category) }
    }
}
