package com.android.purebilibili.feature.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*

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
 * One icon vocabulary shared by home navigation and its settings preview.
 * Selected tabs use the filled member of the same symbol family whenever one is available.
 */
internal fun resolveHomeNavigationIcon(
    tabId: String,
    iconFamily: AppSemanticIconFamily,
    selected: Boolean = false,
): ImageVector {
    val role = when (tabId.trim().uppercase()) {
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
