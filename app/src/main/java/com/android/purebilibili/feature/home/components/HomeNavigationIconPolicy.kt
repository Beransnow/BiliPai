package com.android.purebilibili.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.android.purebilibili.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.TopDownloads

private enum class HomeNavigationIconRole {
    HOME,
    DYNAMIC,
    STORY,
    HISTORY,
    LISTEN_VIDEO,
    PROFILE,
    FAVORITE,
    LIVE,
    WATCH_LATER,
    SETTINGS,
    PLUGINS,
    FOLLOW,
    POPULAR,
    ANIME,
    GAME,
    PARTITION,
    KNOWLEDGE,
    TECH,
}

internal enum class HomeNavigationIconSource {
    MIUIX,
}

private fun resolveHomeNavigationIconRole(tabId: String): HomeNavigationIconRole = when (tabId.trim().uppercase()) {
    "HOME", "RECOMMEND" -> HomeNavigationIconRole.HOME
    "DYNAMIC" -> HomeNavigationIconRole.DYNAMIC
    "STORY" -> HomeNavigationIconRole.STORY
    "HISTORY" -> HomeNavigationIconRole.HISTORY
    "LISTEN_VIDEO" -> HomeNavigationIconRole.LISTEN_VIDEO
    "PROFILE" -> HomeNavigationIconRole.PROFILE
    "FAVORITE" -> HomeNavigationIconRole.FAVORITE
    "LIVE" -> HomeNavigationIconRole.LIVE
    "WATCHLATER", "WATCH_LATER" -> HomeNavigationIconRole.WATCH_LATER
    "SETTINGS" -> HomeNavigationIconRole.SETTINGS
    "PLUGINS" -> HomeNavigationIconRole.PLUGINS
    "FOLLOW" -> HomeNavigationIconRole.FOLLOW
    "POPULAR" -> HomeNavigationIconRole.POPULAR
    "ANIME" -> HomeNavigationIconRole.ANIME
    "GAME" -> HomeNavigationIconRole.GAME
    "PARTITION" -> HomeNavigationIconRole.PARTITION
    "KNOWLEDGE" -> HomeNavigationIconRole.KNOWLEDGE
    "TECH" -> HomeNavigationIconRole.TECH
    else -> HomeNavigationIconRole.HOME
}

internal fun resolveMiuixPreferredHomeNavigationIconSource(
    tabId: String,
): HomeNavigationIconSource {
    resolveHomeNavigationIconRole(tabId)
    return HomeNavigationIconSource.MIUIX
}

/**
 * 首页底栏、侧栏和顶部分区的唯一图标入口。
 */
internal fun resolveMiuixPreferredHomeNavigationIcon(
    tabId: String,
    selected: Boolean = false,
): ImageVector {
    val role = resolveHomeNavigationIconRole(tabId)
    return resolveMiuixHomeNavigationIcon(role, selected)
}

/**
 * Miuix navigation chrome keeps its native behavior and colors while the bottom/side navigation
 * uses one optically consistent Material Symbols set for outlined and filled selection states.
 */
@Composable
internal fun resolveMiuixBottomNavigationIcon(
    item: BottomNavItem,
    selected: Boolean,
): ImageVector = ImageVector.vectorResource(
    when (item) {
        BottomNavItem.HOME -> if (selected) R.drawable.ms_home_fill_24 else R.drawable.ms_home_24
        BottomNavItem.DYNAMIC -> if (selected) R.drawable.ms_notifications_fill_24 else R.drawable.ms_notifications_none_24
        BottomNavItem.STORY -> if (selected) R.drawable.ms_play_circle_fill_24 else R.drawable.ms_play_circle_outline_24
        BottomNavItem.HISTORY -> if (selected) R.drawable.ms_history_fill_24 else R.drawable.ms_history_24
        BottomNavItem.LISTEN_VIDEO -> if (selected) R.drawable.ms_library_music_fill_24 else R.drawable.ms_library_music_24
        BottomNavItem.PROFILE -> if (selected) R.drawable.ms_person_fill_24 else R.drawable.ms_person_24
        BottomNavItem.FAVORITE -> if (selected) R.drawable.ms_collections_bookmark_fill_24 else R.drawable.ms_collections_bookmark_24
        BottomNavItem.LIVE -> if (selected) R.drawable.ms_live_tv_fill_24 else R.drawable.ms_live_tv_24
        BottomNavItem.WATCHLATER -> if (selected) R.drawable.ms_watch_later_fill_24 else R.drawable.ms_watch_later_24
        BottomNavItem.SETTINGS -> if (selected) R.drawable.ms_settings_fill_24 else R.drawable.ms_settings_24
        BottomNavItem.PLUGINS -> if (selected) R.drawable.ms_extension_fill_24 else R.drawable.ms_extension_24
    }
)

private fun resolveMiuixHomeNavigationIcon(
    role: HomeNavigationIconRole,
    selected: Boolean,
): ImageVector = when (role) {
    HomeNavigationIconRole.HOME -> if (selected) MiuixIcons.Medium.Home else MiuixIcons.Light.Home
    HomeNavigationIconRole.DYNAMIC -> if (selected) MiuixIcons.Medium.Community else MiuixIcons.Light.Community
    HomeNavigationIconRole.STORY -> if (selected) MiuixIcons.Medium.Play else MiuixIcons.Light.Play
    HomeNavigationIconRole.HISTORY -> if (selected) MiuixIcons.Medium.Recent else MiuixIcons.Light.Recent
    HomeNavigationIconRole.LISTEN_VIDEO -> if (selected) MiuixIcons.Medium.Music else MiuixIcons.Light.Music
    HomeNavigationIconRole.PROFILE -> if (selected) MiuixIcons.Medium.ContactsCircle else MiuixIcons.Light.ContactsCircle
    HomeNavigationIconRole.FAVORITE -> if (selected) MiuixIcons.Medium.Favorites else MiuixIcons.Light.Favorites
    HomeNavigationIconRole.LIVE -> if (selected) MiuixIcons.Medium.Recording else MiuixIcons.Light.Recording
    HomeNavigationIconRole.WATCH_LATER -> if (selected) MiuixIcons.Medium.Stopwatch else MiuixIcons.Light.Stopwatch
    HomeNavigationIconRole.SETTINGS -> if (selected) MiuixIcons.Medium.Settings else MiuixIcons.Light.Settings
    HomeNavigationIconRole.PLUGINS -> if (selected) MiuixIcons.Medium.Folder else MiuixIcons.Light.Folder
    HomeNavigationIconRole.FOLLOW -> if (selected) MiuixIcons.Medium.Contacts else MiuixIcons.Light.Contacts
    HomeNavigationIconRole.POPULAR -> if (selected) MiuixIcons.Medium.TopDownloads else MiuixIcons.Light.TopDownloads
    HomeNavigationIconRole.ANIME -> if (selected) MiuixIcons.Medium.Play else MiuixIcons.Light.Play
    HomeNavigationIconRole.GAME -> if (selected) MiuixIcons.Medium.Store else MiuixIcons.Light.Store
    HomeNavigationIconRole.PARTITION -> if (selected) MiuixIcons.Medium.GridView else MiuixIcons.Light.GridView
    HomeNavigationIconRole.KNOWLEDGE -> if (selected) MiuixIcons.Medium.Notes else MiuixIcons.Light.Notes
    HomeNavigationIconRole.TECH -> if (selected) MiuixIcons.Medium.Theme else MiuixIcons.Light.Theme
}
