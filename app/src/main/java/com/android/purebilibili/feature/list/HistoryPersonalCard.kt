package com.android.purebilibili.feature.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.FeedTitleHierarchy
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppLinearProgressIndicator
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.feature.home.components.cards.resolveVideoCardCoverOverlayTextShadow
import com.android.purebilibili.feature.personal.PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO
import com.android.purebilibili.feature.personal.PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP

internal fun resolveHistoryKindLabel(business: HistoryBusiness): String = when (business) {
    HistoryBusiness.ARCHIVE -> "视频"
    HistoryBusiness.PGC -> "番剧"
    HistoryBusiness.LIVE -> "直播"
    HistoryBusiness.ARTICLE -> "专栏"
    HistoryBusiness.UNKNOWN -> "未知"
}

internal fun resolveHistoryProgressLabel(progress: Int, duration: Int): String = when {
    progress == -1 -> "已看完"
    duration <= 0 -> "已看"
    progress <= 0 -> FormatUtils.formatDuration(duration)
    else -> "${FormatUtils.formatDuration(progress)}/${FormatUtils.formatDuration(duration)}"
}

internal fun canAddHistoryToWatchLater(item: HistoryItem): Boolean =
    item.business == HistoryBusiness.ARCHIVE && item.videoItem.id > 0L

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HistoryPersonalCard(
    item: HistoryItem,
    selected: Boolean,
    batchMode: Boolean,
    transitionEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUpClick: (() -> Unit)?,
    onAddToWatchLater: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val video = item.videoItem
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val sourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val speedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val transitionAdaptiveInfo = com.android.purebilibili.core.ui.transition
        .LocalVideoTransitionAdaptiveInfo.current
    val sharedElementReady = transitionEnabled &&
        video.bvid.isNotBlank() &&
        sourceRoute != null &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val motionSpec = remember(sourceRoute, transitionEnabled, speedSettings, transitionAdaptiveInfo) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = transitionEnabled,
            speedSettings = speedSettings,
            adaptiveInfo = transitionAdaptiveInfo,
        )
    }
    val useSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = sharedElementReady,
    )
    val cardBounds = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val coverBounds = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val screenWidthPx = configuration.screenWidthDp * density.density
    val screenHeightPx = configuration.screenHeightDp * density.density
    val progressState = remember(item.progress, video.duration, video.view_at) {
        resolveVideoDisplayProgressState(
            serverProgressSec = item.progress,
            durationSec = video.duration,
            viewAt = video.view_at,
        )
    }
    val stationaryCoverUrl = remember(video.pic) { FormatUtils.fixImageUrl(video.pic) }
    val stationaryCoverRequest = remember(stationaryCoverUrl) {
        ImageRequest.Builder(context)
            .data(stationaryCoverUrl)
            .crossfade(false)
            .memoryCacheKey(stationaryCoverUrl)
            .diskCacheKey(stationaryCoverUrl)
            .build()
    }
    val triggerClick = {
        if (!batchMode) {
            cardBounds.value?.let { bounds ->
                CardPositionManager.recordVideoCardPosition(
                    bvid = video.bvid,
                    sourceRoute = sourceRoute,
                    bounds = bounds,
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx,
                    sourceCornerDp = 12,
                    coverBounds = coverBounds.value,
                    sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
                    sourceChromeSnapshot = VideoCardSourceChromeSnapshot(
                        title = video.title,
                        ownerName = video.owner.name.takeIf { it.isNotBlank() }
                            ?: if (item.business == HistoryBusiness.PGC) "番剧" else "未知作者",
                        ownerFaceUrl = video.owner.face,
                        viewText = FormatUtils.formatStat(video.stat.view.toLong()),
                        danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                        durationText = FormatUtils.formatDuration(video.duration),
                        infoPresentation = com.android.purebilibili.core.ui.transition
                            .resolveVideoCardSourceInfoPresentation(
                                publishTimeText = "",
                                showStatsInInfo = true,
                            ),
                        coverUrl = stationaryCoverUrl,
                        coverCacheKey = stationaryCoverUrl,
                    ),
                )
            }
        }
        onClick()
    }

    val coverOverlayTextStyle = remember {
        TextStyle(shadow = resolveVideoCardCoverOverlayTextShadow())
    }
    val contentTypography = feedContentTypography(FeedTitleHierarchy.Standard)
    val coverWidth = PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP.dp
    val coverHeight =
        (PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP / PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO).dp
    val coverShape = AppShapes.container(ContainerLevel.Card)
    val owner = video.owner.name.takeIf { it.isNotBlank() }
        ?: if (item.business == HistoryBusiness.PGC) "番剧" else "未知作者"
    val viewedAt = FormatUtils.formatPublishTime(video.view_at)
    val titleMaxLines = if (item.page > 1) 1 else 2

    Row(
        modifier = modifier
            .fillMaxWidth()
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                motionSpec = motionSpec,
                clipShape = coverShape,
                crossfadeSourceContent = true,
            )
            .combinedClickable(
                onClick = triggerClick,
                onLongClick = onLongClick,
            )
            .onGloballyPositioned { cardBounds.value = it.boundsInRoot() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .then(
                if (selected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    )
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .height(coverHeight)
                .clip(coverShape)
                .onGloballyPositioned { coverBounds.value = it.boundsInRoot() },
        ) {
            AsyncImage(
                model = stationaryCoverRequest,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            AppText(
                text = resolveHistoryProgressLabel(progressState.progressSec, video.duration),
                color = MediaContrastPalette.Foreground,
                style = feedContentTypography().coverBadge
                    .copy(fontWeight = FontWeight.Medium)
                    .merge(coverOverlayTextStyle),
                maxLines = 1,
                tapToCopyEnabled = false,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            )
            if (progressState.showProgressBar) {
                AppLinearProgressIndicator(
                    progress = { progressState.progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .height(coverHeight)
                .padding(end = 4.dp),
        ) {
            AppText(
                text = video.title,
                style = contentTypography.title,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                tapToCopyEnabled = false,
            )
            Spacer(modifier = Modifier.weight(1f))
            AppText(
                text = owner,
                style = contentTypography.author,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                tapToCopyEnabled = false,
            )
            if (viewedAt.isNotBlank()) {
                AppText(
                    text = viewedAt,
                    style = contentTypography.author,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    tapToCopyEnabled = false,
                )
            }
        }
        if (!batchMode) {
            Box(
                modifier = Modifier
                    .height(coverHeight)
                    .width(29.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AppIconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(29.dp),
                ) {
                    AppIcon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "历史记录操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                AppDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (onUpClick != null) {
                        AppDropdownMenuItem(
                            text = { AppText("访问UP主") },
                            onClick = {
                                menuExpanded = false
                                onUpClick()
                            },
                        )
                    }
                    if (onAddToWatchLater != null) {
                        AppDropdownMenuItem(
                            text = { AppText("加入稍后再看") },
                            onClick = {
                                menuExpanded = false
                                onAddToWatchLater()
                            },
                        )
                    }
                    AppDropdownMenuItem(
                        text = { AppText("删除记录", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
