package com.android.purebilibili.navigation3

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition

internal fun NavEntryBuilder.biliPaiNavEntries(
    videoCardTransition: NavTransition,
    fullscreenVideoCardTransition: NavTransition,
    content: @Composable (BiliPaiNavKey) -> Unit,
) {
    entry<BiliPaiNavKey.MainHost>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Home>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.ListenVideo>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Dynamic>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Search>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SearchTrending>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.TopicDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Settings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SettingsCategory>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SettingsSearch>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.OpenSourceLicenses>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.AppearanceSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.HomeSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.IconSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.AnimationSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.PlaybackSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.PermissionSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.PluginsSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.JsPluginContent>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.ExternalMedia>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.BottomBarSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SettingsShare>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.WebDavBackup>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.TipsSettings>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Login>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Profile>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.History>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Favorite>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LikedVideos>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.WatchLater>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Onboarding>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Following>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.DownloadList>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.OfflineVideoPlayer>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LiveList>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LiveSearch>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LiveArea>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LiveAreaDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LiveFollowing>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Inbox>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.ReplyMe>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.AtMe>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.LikeMe>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SystemNotice>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Chat>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Partition>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Story>(
        transition = fullscreenVideoCardTransition,
        swipeDismiss = NavSwipeDirection.None,
        content = content,
    )
    entry<BiliPaiNavKey.AudioMode>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.SeasonSeriesDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Bangumi>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.BangumiPlayer>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.MusicDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.NativeMusic>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.VideoDetail>(
        transition = videoCardTransition,
        swipeDismiss = NavSwipeDirection.None,
        content = content,
    )
    entry<BiliPaiNavKey.ArticleDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.DynamicDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Space>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Category>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Live>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.BangumiDetail>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Web>(swipeDismiss = NavSwipeDirection.None, content = content)
    entry<BiliPaiNavKey.Unknown>(swipeDismiss = NavSwipeDirection.None, content = content)
}
