package com.android.purebilibili.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.AndroidNativeVariant
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Tv
import io.github.alexzhirkevich.cupertino.icons.filled.Paperplane
import io.github.alexzhirkevich.cupertino.icons.filled.PlayCircle as FilledPlayCircleIcon
import io.github.alexzhirkevich.cupertino.icons.filled.Star as FilledStarIcon
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowUpArrowDown
import io.github.alexzhirkevich.cupertino.icons.outlined.Bolt as OutlinedBoltIcon
import io.github.alexzhirkevich.cupertino.icons.outlined.Bookmark
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronUp
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.outlined.Envelope
import io.github.alexzhirkevich.cupertino.icons.outlined.Eye
import io.github.alexzhirkevich.cupertino.icons.outlined.EyeSlash
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.Grid
import io.github.alexzhirkevich.cupertino.icons.outlined.Ipad
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Link as OutlinedLinkIcon
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBullet
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.icons.outlined.Message
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircleBadgePlus
import io.github.alexzhirkevich.cupertino.icons.outlined.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.PlayCircle as OutlinedPlayCircleIcon
import io.github.alexzhirkevich.cupertino.icons.outlined.BellBadge
import io.github.alexzhirkevich.cupertino.icons.outlined.ChartBar
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.RectangleStack
import io.github.alexzhirkevich.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTurnUpRight
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import io.github.alexzhirkevich.cupertino.icons.outlined.Star as OutlinedStarIcon
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconsPresetPolicyTest {

    @Test
    fun `md3 preset should map key chrome icons to material vectors`() {
        assertEquals(Icons.AutoMirrored.Filled.ArrowBack, resolveAppBackIcon(UiPreset.MD3))
        assertEquals(Icons.Filled.Search, resolveAppSearchIcon(UiPreset.MD3))
        assertEquals(Icons.Filled.Clear, resolveAppClearIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Close, resolveAppCloseIcon(UiPreset.MD3))
        assertEquals(Icons.AutoMirrored.Outlined.KeyboardArrowRight, resolveAppChevronForwardIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.KeyboardArrowDown, resolveAppChevronDownIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.KeyboardArrowUp, resolveAppChevronUpIcon(UiPreset.MD3))
    }

    @Test
    fun `md3 preset should map service and panel icons to material vectors`() {
        assertEquals(Icons.Outlined.History, resolveAppHistoryIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.BookmarkBorder, resolveAppBookmarkIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.MailOutline, resolveAppInboxIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.LiveTv, resolveAppTvIcon(UiPreset.MD3))
        assertEquals(Icons.AutoMirrored.Outlined.ExitToApp, resolveAppLogoutIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Timer, resolveAppTimerIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.MusicNote, resolveAppMusicIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.SwapHoriz, resolveAppFlipHorizontalIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.SwapVert, resolveAppFlipVerticalIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Headphones, resolveAppHeadphonesIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PlayCircleOutline, resolveAppQualityIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Memory, resolveAppCodecIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Speed, resolveAppSpeedIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.TouchApp, resolveAppGestureTapIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Wifi, resolveAppWifiIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PersonAddAlt1, resolveAppProfileAddIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Lock, resolveAppLockIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.BarChart, resolveAppAnalyticsIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Info, resolveAppInfoIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.NotificationsNone, resolveAppNotificationIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.AutoAwesome, resolveAppSparklesIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.WatchLater, resolveAppWatchLaterIcon(UiPreset.MD3))
        assertEquals(AppIcons.BiliCoin, resolveAppCoinIcon(UiPreset.MD3))
    }

    @Test
    fun `md3 preset should map navigation and interaction icons to material vectors`() {
        assertEquals(Icons.Outlined.Home, resolveAppHomeIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.DynamicFeed, resolveAppDynamicIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PlayArrow, resolveAppPlayIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.PlayCircleOutline, resolveAppPlayCircleIcon(UiPreset.MD3))
        assertEquals(Icons.Filled.PlayCircle, resolveAppPlayCircleFilledIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.FolderCopy, resolveAppCollectionIcon(UiPreset.MD3))
        assertEquals(Icons.AutoMirrored.Outlined.Comment, resolveAppCommentIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.ThumbUpOffAlt, resolveAppLikeIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Share, resolveAppShareIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.Visibility, resolveAppVisibilityOnIcon(UiPreset.MD3))
        assertEquals(Icons.Outlined.VisibilityOff, resolveAppVisibilityOffIcon(UiPreset.MD3))
    }

    @Test
    fun `ios preset should preserve cupertino mappings`() {
        assertEquals(CupertinoIcons.Outlined.ChevronBackward, resolveAppBackIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.MagnifyingGlass, resolveAppSearchIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.XmarkCircle, resolveAppClearIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Xmark, resolveAppCloseIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Clock, resolveAppHistoryIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Bookmark, resolveAppBookmarkIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Envelope, resolveAppInboxIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Filled.Tv, resolveAppTvIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.RectanglePortraitAndArrowForward, resolveAppLogoutIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.PersonCropCircleBadgePlus, resolveAppProfileAddIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Lock, resolveAppLockIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChevronForward, resolveAppChevronForwardIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChevronDown, resolveAppChevronDownIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChevronUp, resolveAppChevronUpIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.House, resolveAppHomeIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.RectangleStack, resolveAppDynamicIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Play, resolveAppPlayIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.OutlinedPlayCircleIcon, resolveAppPlayCircleIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Filled.FilledPlayCircleIcon, resolveAppPlayCircleFilledIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Bookmark, resolveAppBookmarkIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Message, resolveAppCommentIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.HandThumbsup, resolveAppLikeIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ArrowTurnUpRight, resolveAppShareIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Eye, resolveAppVisibilityOnIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.EyeSlash, resolveAppVisibilityOffIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.ChartBar, resolveAppAnalyticsIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.InfoCircle, resolveAppInfoIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.BellBadge, resolveAppNotificationIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Sparkles, resolveAppSparklesIcon(UiPreset.IOS))
        assertEquals(CupertinoIcons.Outlined.Clock, resolveAppWatchLaterIcon(UiPreset.IOS))
        assertEquals(AppIcons.BiliCoin, resolveAppCoinIcon(UiPreset.IOS))
    }

    @Test
    fun `phase three semantic icons should map across all renderer combinations`() {
        val cases = listOf(
            SemanticIconCase("close", ::resolveAppCloseIcon, Icons.Outlined.Close, CupertinoIcons.Outlined.Xmark),
            SemanticIconCase("play circle", ::resolveAppPlayCircleIcon, Icons.Outlined.PlayCircleOutline, CupertinoIcons.Outlined.OutlinedPlayCircleIcon),
            SemanticIconCase("play circle filled", ::resolveAppPlayCircleFilledIcon, Icons.Filled.PlayCircle, CupertinoIcons.Filled.FilledPlayCircleIcon),
            SemanticIconCase("delete", ::resolveAppDeleteIcon, Icons.Outlined.Delete, CupertinoIcons.Outlined.Trash),
            SemanticIconCase("link", ::resolveAppLinkIcon, Icons.Outlined.Link, CupertinoIcons.Outlined.OutlinedLinkIcon),
            SemanticIconCase("copy", ::resolveAppCopyIcon, Icons.Outlined.ContentCopy, CupertinoIcons.Outlined.DocOnDoc),
            SemanticIconCase("send", ::resolveAppSendIcon, Icons.AutoMirrored.Filled.Send, CupertinoIcons.Filled.Paperplane),
            SemanticIconCase("favorite", ::resolveAppFavoriteIcon, Icons.Outlined.StarBorder, CupertinoIcons.Outlined.OutlinedStarIcon),
            SemanticIconCase("favorite filled", ::resolveAppFavoriteFilledIcon, Icons.Filled.Star, CupertinoIcons.Filled.FilledStarIcon),
            SemanticIconCase("list layout", ::resolveAppListLayoutIcon, Icons.AutoMirrored.Outlined.FormatListBulleted, CupertinoIcons.Outlined.ListBullet),
            SemanticIconCase("stack layout", ::resolveAppStackLayoutIcon, Icons.Outlined.ViewAgenda, CupertinoIcons.Outlined.RectangleStack),
            SemanticIconCase("grid layout", ::resolveAppGridLayoutIcon, Icons.Outlined.GridView, CupertinoIcons.Outlined.Grid),
            SemanticIconCase("sort", ::resolveAppSortIcon, Icons.AutoMirrored.Outlined.Sort, CupertinoIcons.Outlined.ArrowUpArrowDown),
            SemanticIconCase("selection checked", ::resolveAppSelectionCheckedIcon, Icons.Outlined.CheckCircle, CupertinoIcons.Outlined.CheckmarkCircle),
            SemanticIconCase("phone device", ::resolveAppPhoneDeviceIcon, Icons.Outlined.PhoneAndroid, CupertinoIcons.Outlined.Iphone),
            SemanticIconCase("tablet device", ::resolveAppTabletDeviceIcon, Icons.Outlined.TabletAndroid, CupertinoIcons.Outlined.Ipad),
            SemanticIconCase("live status", ::resolveAppLiveStatusIcon, Icons.Outlined.Bolt, CupertinoIcons.Outlined.OutlinedBoltIcon),
        )

        cases.forEach { case ->
            AndroidNativeVariant.entries.forEach { variant ->
                assertEquals("${case.name} material/$variant", case.material, case.resolver(UiPreset.MD3, variant))
                assertEquals("${case.name} ios/$variant", case.ios, case.resolver(UiPreset.IOS, variant))
            }
        }
    }

    private data class SemanticIconCase(
        val name: String,
        val resolver: (UiPreset, AndroidNativeVariant) -> ImageVector,
        val material: ImageVector,
        val ios: ImageVector,
    )
}
