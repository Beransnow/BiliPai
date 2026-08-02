package com.android.purebilibili.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.WorldClock

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

/**
 * 首页导航统一优先采用 Miuix 图标；用户补充的 SVG 作为项目内矢量资源；
 * Miuix 0.9.3 没有等价图标时，才保留当前主题的图标作为最后回退。
 */
internal enum class HomeNavigationIconSource {
    MIUIX,
    LOCAL_DYNAMIC,
    LOCAL_STORY,
    LOCAL_LIVE,
    LOCAL_GAME,
    THEME_FALLBACK,
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
): HomeNavigationIconSource = when (resolveHomeNavigationIconRole(tabId)) {
    HomeNavigationIconRole.DYNAMIC -> HomeNavigationIconSource.LOCAL_DYNAMIC
    HomeNavigationIconRole.STORY -> HomeNavigationIconSource.LOCAL_STORY
    HomeNavigationIconRole.LIVE -> HomeNavigationIconSource.LOCAL_LIVE
    HomeNavigationIconRole.GAME -> HomeNavigationIconSource.LOCAL_GAME
    HomeNavigationIconRole.HOME,
    HomeNavigationIconRole.HISTORY,
    HomeNavigationIconRole.LISTEN_VIDEO,
    HomeNavigationIconRole.FAVORITE,
    HomeNavigationIconRole.WATCH_LATER,
    HomeNavigationIconRole.SETTINGS,
    HomeNavigationIconRole.PARTITION -> HomeNavigationIconSource.MIUIX
    HomeNavigationIconRole.PROFILE,
    HomeNavigationIconRole.PLUGINS,
    HomeNavigationIconRole.FOLLOW,
    HomeNavigationIconRole.POPULAR,
    HomeNavigationIconRole.ANIME,
    HomeNavigationIconRole.KNOWLEDGE,
    HomeNavigationIconRole.TECH -> HomeNavigationIconSource.THEME_FALLBACK
}

/**
 * 首页底栏、侧栏和顶部分区的唯一图标入口。
 *
 * 设置页预览和分区页仍使用 [resolveHomeNavigationIcon]，避免改变本阶段以外的界面。
 */
@Composable
internal fun resolveMiuixPreferredHomeNavigationIcon(
    tabId: String,
    fallbackIconFamily: AppSemanticIconFamily,
    selected: Boolean = false,
): ImageVector {
    val role = resolveHomeNavigationIconRole(tabId)
    return when (resolveMiuixPreferredHomeNavigationIconSource(tabId)) {
        HomeNavigationIconSource.MIUIX -> resolveMiuixHomeNavigationIcon(role, selected)
        HomeNavigationIconSource.LOCAL_DYNAMIC -> ImageVector.vectorResource(R.drawable.ic_home_nav_dynamic)
        HomeNavigationIconSource.LOCAL_STORY -> ImageVector.vectorResource(R.drawable.ic_home_nav_story)
        HomeNavigationIconSource.LOCAL_LIVE -> ImageVector.vectorResource(R.drawable.ic_home_nav_live)
        HomeNavigationIconSource.LOCAL_GAME -> ImageVector.vectorResource(R.drawable.ic_home_nav_game)
        HomeNavigationIconSource.THEME_FALLBACK -> resolveHomeNavigationIcon(
            tabId = tabId,
            iconFamily = fallbackIconFamily,
            selected = selected,
        )
    }
}

private fun resolveMiuixHomeNavigationIcon(
    role: HomeNavigationIconRole,
    selected: Boolean,
): ImageVector = when (role) {
    HomeNavigationIconRole.HOME -> if (selected) MiuixIcons.Medium.Home else MiuixIcons.Home
    HomeNavigationIconRole.HISTORY,
    HomeNavigationIconRole.WATCH_LATER -> if (selected) MiuixIcons.Medium.WorldClock else MiuixIcons.WorldClock
    HomeNavigationIconRole.LISTEN_VIDEO -> if (selected) MiuixIcons.Medium.Music else MiuixIcons.Music
    HomeNavigationIconRole.FAVORITE -> if (selected) MiuixIcons.FavoritesFill else MiuixIcons.Favorites
    HomeNavigationIconRole.SETTINGS -> if (selected) MiuixIcons.Medium.Settings else MiuixIcons.Settings
    HomeNavigationIconRole.PARTITION -> if (selected) MiuixIcons.Medium.GridView else MiuixIcons.GridView
    else -> error("Miuix icon requested for unsupported role: $role")
}

/**
 * One icon vocabulary shared by home navigation and its settings preview.
 * Selected tabs use the filled member of the same symbol family whenever one is available.
 */
internal fun resolveHomeNavigationIcon(
    tabId: String,
    iconFamily: AppSemanticIconFamily,
    selected: Boolean = false,
): ImageVector {
    val role = resolveHomeNavigationIconRole(tabId)

    return when (iconFamily) {
        AppSemanticIconFamily.MATERIAL -> resolveMaterialNavigationIcon(role, selected)
        AppSemanticIconFamily.CUPERTINO -> resolveCupertinoNavigationIcon(role, selected)
    }
}

private fun resolveMaterialNavigationIcon(
    role: HomeNavigationIconRole,
    selected: Boolean,
): ImageVector = when (role) {
    HomeNavigationIconRole.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    HomeNavigationIconRole.DYNAMIC -> if (selected) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone
    HomeNavigationIconRole.STORY -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircleOutline
    HomeNavigationIconRole.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
    HomeNavigationIconRole.LISTEN_VIDEO -> if (selected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic
    HomeNavigationIconRole.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    HomeNavigationIconRole.FAVORITE -> if (selected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
    HomeNavigationIconRole.LIVE -> if (selected) Icons.Filled.LiveTv else Icons.Outlined.LiveTv
    HomeNavigationIconRole.WATCH_LATER -> if (selected) Icons.Filled.WatchLater else Icons.Outlined.WatchLater
    HomeNavigationIconRole.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    HomeNavigationIconRole.PLUGINS -> if (selected) Icons.Filled.Extension else Icons.Outlined.Extension
    HomeNavigationIconRole.FOLLOW -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    HomeNavigationIconRole.POPULAR -> if (selected) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Outlined.TrendingUp
    HomeNavigationIconRole.ANIME -> if (selected) Icons.Filled.Tv else Icons.Outlined.Tv
    HomeNavigationIconRole.GAME -> if (selected) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports
    HomeNavigationIconRole.PARTITION -> if (selected) Icons.Filled.GridView else Icons.Outlined.GridView
    HomeNavigationIconRole.KNOWLEDGE -> if (selected) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb
    HomeNavigationIconRole.TECH -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
}

private fun resolveCupertinoNavigationIcon(
    role: HomeNavigationIconRole,
    selected: Boolean,
): ImageVector = when (role) {
    HomeNavigationIconRole.HOME -> if (selected) CupertinoIcons.Filled.House else CupertinoIcons.Outlined.House
    HomeNavigationIconRole.DYNAMIC -> if (selected) CupertinoIcons.Filled.Bell else CupertinoIcons.Outlined.Bell
    HomeNavigationIconRole.STORY -> if (selected) CupertinoIcons.Filled.PlayCircle else CupertinoIcons.Outlined.PlayCircle
    HomeNavigationIconRole.HISTORY -> if (selected) CupertinoIcons.Filled.Clock else CupertinoIcons.Outlined.Clock
    HomeNavigationIconRole.LISTEN_VIDEO -> if (selected) CupertinoIcons.Default.MusicNote else CupertinoIcons.Outlined.MusicNote
    HomeNavigationIconRole.PROFILE -> if (selected) CupertinoIcons.Filled.Person else CupertinoIcons.Outlined.Person
    HomeNavigationIconRole.FAVORITE -> if (selected) CupertinoIcons.Filled.Star else CupertinoIcons.Outlined.Star
    HomeNavigationIconRole.LIVE -> if (selected) CupertinoIcons.Filled.Video else CupertinoIcons.Outlined.Video
    HomeNavigationIconRole.WATCH_LATER -> if (selected) CupertinoIcons.Filled.Clock else CupertinoIcons.Outlined.Clock
    HomeNavigationIconRole.SETTINGS -> if (selected) CupertinoIcons.Filled.Gearshape else CupertinoIcons.Default.Gearshape
    HomeNavigationIconRole.PLUGINS -> if (selected) CupertinoIcons.Default.Puzzlepiece else CupertinoIcons.Outlined.PuzzlepieceExtension
    HomeNavigationIconRole.FOLLOW -> if (selected) {
        CupertinoIcons.Filled.PersonCropCircleBadgePlus
    } else {
        CupertinoIcons.Outlined.PersonCropCircleBadgePlus
    }
    HomeNavigationIconRole.POPULAR -> if (selected) CupertinoIcons.Filled.ChartBar else CupertinoIcons.Outlined.ChartBar
    HomeNavigationIconRole.ANIME -> if (selected) CupertinoIcons.Filled.Tv else CupertinoIcons.Outlined.Tv
    HomeNavigationIconRole.GAME -> if (selected) CupertinoIcons.Filled.Gamecontroller else CupertinoIcons.Outlined.Gamecontroller
    HomeNavigationIconRole.PARTITION -> CupertinoIcons.Outlined.Grid
    HomeNavigationIconRole.KNOWLEDGE -> if (selected) CupertinoIcons.Filled.Lightbulb else CupertinoIcons.Outlined.Lightbulb
    HomeNavigationIconRole.TECH -> if (selected) CupertinoIcons.Filled.Cpu else CupertinoIcons.Outlined.Cpu
}
