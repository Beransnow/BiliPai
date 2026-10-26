package com.android.purebilibili.feature.video.ui.overlay.portrait

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.playback.audio.resolveAudioQualityControlPresentation
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.overlay.PlayerChromePresentation
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayActions
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayState

/** Shared portrait chrome; the player overlay owns menus, tools and the danmaku composer. */
@Composable
internal fun PortraitPagerControls(
    state: VideoPlayerOverlayState,
    actions: VideoPlayerOverlayActions,
    presentation: PlayerChromePresentation,
    info: ViewInfo?,
    stat: Stat,
    progress: PlayerProgress,
    videoAspectRatio: Float,
    commentExpansionProgress: Float,
    onDetailClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onStaffMemberClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showVideoDeclarations by com.android.purebilibili.core.store.SettingsManager
        .getVideoArgueMsgShown(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val audio = resolveAudioQualityControlPresentation(
        selectedAudioQuality = state.selectedAudioQuality,
        options = state.availableAudioQualities,
    )
    PortraitFullscreenOverlay(
        modifier = modifier,
        title = state.title,
        declarations = if (showVideoDeclarations) buildList {
            info?.argueInfo?.argueMsg?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            if (info?.rights?.noReprint == 1) add("未经作者授权，请勿转载")
        }.distinct() else emptyList(),
        ugcSeason = info?.ugc_season,
        currentBvid = state.bvid,
        currentCid = state.cid,
        authorName = state.videoOwnerName,
        authorFace = state.videoOwnerFace,
        staff = info?.staff.orEmpty(),
        ownerMid = info?.owner?.mid ?: 0L,
        onStaffMemberClick = onStaffMemberClick,
        isPlaying = presentation.isPlaying,
        progress = progress,
        statView = stat.view,
        statLike = state.likeCount.toInt(),
        statCoin = stat.coin,
        statDanmaku = stat.danmaku,
        statReply = stat.reply,
        statFavorite = state.favoriteCount.toInt(),
        statShare = stat.share,
        isLiked = state.isLiked,
        likeStatusKnown = state.likeStatusKnown,
        isCoined = state.isCoined,
        isFavorited = state.isFavorited,
        onLikeClick = actions.onToggleLike,
        onLikeLongClick = actions.onTripleAction,
        onCoinClick = actions.onCoin,
        onFavoriteClick = actions.onToggleFavorite,
        onFavoriteLongClick = actions.onQuickFavorite,
        onCommentClick = actions.onLandscapeCommentClick,
        onShareClick = { actions.onShare?.invoke() },
        isFollowing = state.isFollowed,
        followStatusKnown = state.followStatusKnown,
        onFollowClick = actions.onToggleFollow,
        onDetailClick = onDetailClick,
        onTitleClick = onDetailClick,
        onAuthorClick = onAuthorClick,
        currentSpeed = presentation.currentSpeed,
        currentQualityLabel = state.currentQualityLabel,
        currentAudioQualityLabel = audio.label,
        isHiResAudioSelected = audio.showHiResBadge,
        isDolbyAudioSelected = audio.showDolbyBadge,
        currentRatio = state.currentAspectRatio,
        danmakuEnabled = state.danmakuEnabled,
        showControls = presentation.controlsVisible,
        commentExpansionProgress = commentExpansionProgress,
        videoshotData = state.videoshotData,
        videoAspectRatio = videoAspectRatio,
        onBack = actions.onBack,
        onHomeClick = actions.onHomeClick,
        onSeek = { actions.onSeekTo?.invoke(it) },
        onSeekStart = actions.onSeekStart,
        seekPositionMs = state.progressDisplayOverridePositionMs ?: progress.current,
        isSeekScrubbing = state.isSeekScrubbing,
        onSeekDragStart = actions.onSeekDragStart,
        onSeekDragUpdate = actions.onSeekDragUpdate,
        onSeekDragCancel = actions.onSeekDragCancel,
        onSpeedClick = presentation.openSpeed,
        onQualityClick = presentation.openQuality,
        onAudioQualityClick = presentation.openAudio,
        onRatioClick = presentation.openRatio,
        showSubtitleChip = state.subtitleControlState.trackAvailable,
        subtitleEnabled = state.subtitleControlState.enabled,
        onSubtitleClick = presentation.openSubtitle,
        onDanmakuToggle = actions.onDanmakuToggle,
        onDanmakuInputClick = actions.onDanmakuInputClick,
        onRotateToLandscape = actions.onToggleFullscreen,
        onSearchClick = onSearchClick,
        onMoreClick = presentation.openSettings,
    )
}
