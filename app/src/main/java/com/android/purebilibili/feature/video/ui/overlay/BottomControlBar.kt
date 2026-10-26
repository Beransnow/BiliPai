package com.android.purebilibili.feature.video.ui.overlay

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.player.resolveProgressFraction as resolveSharedProgressFraction
import com.android.purebilibili.core.store.PlayerProgressPlacement
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.SponsorProgressMarker

import com.android.purebilibili.feature.video.playback.policy.resolveDisplayedPlaybackTransitionPosition
import com.android.purebilibili.feature.video.progress.PbpRidgeSample
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import com.android.purebilibili.feature.video.ui.components.NativeDanmakuToggleButton
import com.android.purebilibili.feature.video.ui.components.PiliPlusDanmakuSendIcon
import com.android.purebilibili.feature.video.ui.components.PiliPlusDanmakuSettingsIcon
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.components.shouldShowCompactDanmakuSendAction
import kotlin.math.roundToInt

data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

internal fun resolveSeekableDurationMs(
    playbackDurationMs: Long,
    fallbackDurationMs: Long
): Long {
    return if (playbackDurationMs > 0L) {
        playbackDurationMs
    } else {
        fallbackDurationMs.coerceAtLeast(0L)
    }
}

internal fun resolveDisplayedPlayerProgress(
    progress: PlayerProgress,
    previewPositionMs: Long?,
    previewActive: Boolean,
    playbackTransitionPositionMs: Long? = null
): PlayerProgress {
    val safeDuration = progress.duration.coerceAtLeast(0L)
    if (previewActive && previewPositionMs != null) {
        val resolvedCurrent = if (safeDuration > 0L) {
            previewPositionMs.coerceIn(0L, safeDuration)
        } else {
            previewPositionMs.coerceAtLeast(0L)
        }
        return progress.copy(current = resolvedCurrent)
    }

    val heldCurrent = resolveDisplayedPlaybackTransitionPosition(
        playerPositionMs = progress.current,
        transitionPositionMs = playbackTransitionPositionMs
    )
    val resolvedCurrent = if (safeDuration > 0L) {
        heldCurrent.coerceIn(0L, safeDuration)
    } else {
        heldCurrent.coerceAtLeast(0L)
    }
    return progress.copy(current = resolvedCurrent)
}

internal fun resolveDisplayedPlayerProgressWithOverride(
    progress: PlayerProgress,
    overridePositionMs: Long?
): PlayerProgress {
    val resolvedCurrent = overridePositionMs ?: return progress
    val safeDuration = progress.duration.coerceAtLeast(0L)
    val clampedCurrent = if (safeDuration > 0L) {
        resolvedCurrent.coerceIn(0L, safeDuration)
    } else {
        resolvedCurrent.coerceAtLeast(0L)
    }
    return progress.copy(current = clampedCurrent)
}

internal fun resolveSeekPreviewTargetPositionMs(
    displayPositionMs: Long,
    dragTargetPositionMs: Long,
    isSeekScrubbing: Boolean
): Long {
    return if (isSeekScrubbing) {
        dragTargetPositionMs.coerceAtLeast(0L)
    } else {
        displayPositionMs.coerceAtLeast(0L)
    }
}

internal fun resolveSeekDragCommitPositionMs(
    dragStartPositionMs: Long,
    latestDragPositionMs: Long
): Long {
    return if (latestDragPositionMs >= 0L) {
        latestDragPositionMs
    } else {
        dragStartPositionMs.coerceAtLeast(0L)
    }
}

internal fun resolveProgressFraction(
    positionMs: Long,
    durationMs: Long
): Float {
    return resolveSharedProgressFraction(positionMs, durationMs)
}

internal fun resolveSeekPositionFromTouch(
    touchX: Float,
    containerWidthPx: Float,
    durationMs: Long
): Long {
    if (durationMs <= 0L || containerWidthPx <= 0f) return 0L
    val fraction = (touchX / containerWidthPx).coerceIn(0f, 1f)
    return (durationMs.toFloat() * fraction).roundToInt().toLong().coerceIn(0L, durationMs)
}

internal fun shouldCancelSeekDragOnPointerInputCompletion(
    dragInProgress: Boolean
): Boolean = dragInProgress

/**
 * Seek tap/drag must live on a shared outer container that wraps both the chapter label and the
 * track. Placing gestures on a track sibling loses pointer DOWN to the chapter [clickable].
 */
enum class VideoProgressBarSeekGestureHost {
    SharedOuterContainer
}

internal fun resolveVideoProgressBarSeekGestureHost(
    @Suppress("UNUSED_PARAMETER") hasChapterLabel: Boolean
): VideoProgressBarSeekGestureHost {
    // Always use a shared outer host. Chapter videos previously broke scrubbing when seek
    // gestures lived on a track sibling under the chapter label's clickable hit target.
    return VideoProgressBarSeekGestureHost.SharedOuterContainer
}

internal fun resolveSubtitlePanelTrackOptions(trackOptions: List<SubtitleTrackOption>): List<SubtitleTrackOption> =
    trackOptions.filter { it.trackKey.isNotBlank() && it.label.isNotBlank() }.distinctBy { it.trackKey }

/** Main's original glass-on geometry, shared by both materials; the host owns selection menus. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomControlBar(
    progress: PlayerProgress = PlayerProgress(),
    isFullscreen: Boolean,
    onSeek: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewportWidthDpOverride: Int? = null,
    viewportIsLandscape: Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
    currentSpeed: Float = 1f,
    currentRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    currentQualityLabel: String = "",
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    onQualityClick: () -> Unit = {},
    onActionBoundsChanged: (PlayerControlAnchor, IntRect) -> Unit = { _, _ -> },

    heatmapEnabled: Boolean = true,
    showViewPoints: Boolean = true,
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    sponsorMarkers: List<SponsorProgressMarker> = emptyList(),
    pbpRidgeSamples: List<PbpRidgeSample> = emptyList(),
    onChapterClick: () -> Unit = {},
    onSeekStart: () -> Unit = {},
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    seekPositionMs: Long = progress.current,
    isSeekScrubbing: Boolean = false,
    seekPositionProvider: (() -> Long)? = null,
    progressProvider: () -> PlayerProgress = { progress },
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    onDanmakuInputClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPortraitFullscreen: (() -> Unit)? = null,
    subtitleTrackAvailable: Boolean = false,
    subtitleEnabled: Boolean = false,
    isLoggedIn: Boolean = true,
    progressPlacement: PlayerProgressPlacement = PlayerProgressPlacement.ABOVE_CONTROLS,
    compactPlayerChrome: Boolean = false,
    fullscreenToggleEndPaddingDp: Int? = null,
    currentChapter: String? = null,
    currentChapterProvider: () -> String? = { currentChapter },
) {
    val configuration = LocalConfiguration.current
    val widthDp = (viewportWidthDpOverride ?: configuration.screenWidthDp).coerceAtLeast(1)
    val layoutPolicy = remember(widthDp, compactPlayerChrome, isFullscreen) {
        resolvePlayerBottomControlBarLayoutPolicy(widthDp, compactPlayerChrome, isFullscreen)
    }
    val progressLayout = remember(widthDp, isFullscreen) {
        val base = resolveVideoProgressBarLayoutPolicy(widthDp)
        if (isFullscreen) base.copy(
            baseHeightWithoutChapterDp = PlayerChromeLayout.SeekRowHeightDp,
            baseHeightWithChapterDp = PlayerChromeLayout.SeekRowHeightDp +
                base.baseHeightWithChapterDp - base.touchContainerHeightDp,
            touchContainerHeightDp = PlayerChromeLayout.SeekRowHeightDp,
            trackHeightDp = 3f,
        ) else base
    }
    val effectiveProgressPlacement = resolveVideoDetailProgressPlacement(progressPlacement, isFullscreen)
    val bottomPaddingDp = resolveBottomControlBarBottomPaddingDp(
        layoutPolicy.bottomPaddingDp, effectiveProgressPlacement,
    )
    val showDanmakuToggle = isFullscreen || widthDp >= 600
    val showCompactDanmakuSend = !isFullscreen && shouldShowCompactDanmakuSendAction(isFullscreen, widthDp)
    val displayPosition = { (seekPositionProvider?.invoke() ?: seekPositionMs).coerceAtLeast(0L) }
    val visibleViewPoints = if (showViewPoints) viewPoints else emptyList()
    val actionSpacing = if (isFullscreen && viewportIsLandscape) 12.dp else 4.dp
    val glassMaterial = LocalAppThemeConfig.current.liquidGlassEnabled
    // Inset the gesture host itself to Android's edge-back boundary, without extra margin.
    val progressEdgeModifier = if (isFullscreen) {
        Modifier.padding(horizontal = layoutPolicy.horizontalPaddingDp.dp)
    } else {
        Modifier.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
    }
    fun anchor(action: PlayerControlAnchor, tag: String): Modifier =
        Modifier.testTag(tag).playerActionBounds { onActionBoundsChanged(action, it) }
    val chromeDensity = LocalDensity.current
    val controlRowDensity = remember(chromeDensity) {
        Density(chromeDensity.density, fontScale = 1f)
    }
    val progressContent: @Composable () -> Unit = {
        val renderProgress: @Composable (Modifier) -> Unit = { progressModifier ->
            PlayerControlProgressBar(
                progressProvider = progressProvider,
                displayPositionProvider = displayPosition,
                isSeekScrubbing = isSeekScrubbing,
                layoutPolicy = progressLayout,
                neutralGlassTrack = isFullscreen && glassMaterial,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekDragStart = onSeekDragStart,
                onSeekDragUpdate = onSeekDragUpdate,
                onSeekDragCancel = onSeekDragCancel,
                videoshotData = videoshotData,
                viewPoints = visibleViewPoints,
                sponsorMarkers = sponsorMarkers,
                pbpRidgeSamples = if (heatmapEnabled) pbpRidgeSamples else emptyList(),
                currentChapterProvider = { if (showViewPoints) currentChapterProvider() else null },
                onChapterClick = onChapterClick,
                modifier = progressModifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("player_progress"),
            )
        }
        if (isFullscreen) {
            Box(
                Modifier.fillMaxWidth()
                    .padding(horizontal = layoutPolicy.horizontalPaddingDp.dp)
                    .heightIn(min = PlayerChromeLayout.SeekRowHeightDp.dp),
            ) {
                renderProgress(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
        } else {
            renderProgress(progressEdgeModifier)
        }
    }
    val qualityAction: @Composable () -> Unit = {
        if (isFullscreen || currentQualityLabel.isNotEmpty()) {
            BottomDockTextAction(
                text = currentQualityLabel.ifBlank { "画质" },
                fontSp = layoutPolicy.actionTextFontSp,
                onClick = onQualityClick,
                modifier = Modifier
                    .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                    .then(anchor(PlayerControlAnchor.QUALITY, "player_quality_action")),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val speedAction: @Composable () -> Unit = {
        BottomDockTextAction(
            text = if (currentSpeed == 1f) "倍速" else "${currentSpeed}x",
            color = if (currentSpeed == 1f) Color.White else AppContentColors.accent(),
            fontSp = layoutPolicy.actionTextFontSp,
            onClick = onSpeedClick,
            modifier = Modifier
                .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                .then(anchor(PlayerControlAnchor.SPEED, "player_speed_action")),
        )
    }
    val subtitleAction: @Composable () -> Unit = {
        AppIconButton(
            onClick = onSubtitleClick,
            enabled = subtitleTrackAvailable,
            modifier = Modifier.size(40.dp)
                .then(anchor(PlayerControlAnchor.SUBTITLE, "player_subtitle_action")),
        ) {
            AppIcon(
                imageVector = Icons.Outlined.Subtitles,
                contentDescription = "字幕",
                tint = when {
                    !subtitleTrackAvailable -> Color.White.copy(alpha = 0.45f)
                    subtitleEnabled -> AppContentColors.accent()
                    else -> Color.White
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
    val ratioAction: @Composable () -> Unit = {
        AppIconButton(
            onClick = onRatioClick,
            modifier = Modifier.size(40.dp)
                .then(anchor(PlayerControlAnchor.RATIO, "player_ratio_action"))
                .semantics { contentDescription = "画面比例 · ${currentRatio.displayName}" },
        ) {
            AppIcon(
                imageVector = Icons.Outlined.AspectRatio,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    val portraitAction: @Composable () -> Unit = {
        if (onPortraitFullscreen != null && (!isFullscreen || viewportIsLandscape)) {
            AppIconButton(
                onClick = onPortraitFullscreen,
                modifier = Modifier.size(40.dp).testTag("player_portrait_fullscreen_action"),
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.StayCurrentPortrait,
                    contentDescription = "竖屏全屏",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .windowInsetsPadding(playerControlHorizontalInsets(viewportIsLandscape, isFullscreen))
            .padding(bottom = bottomPaddingDp.dp + if (isFullscreen) 4.dp else 0.dp),
    ) {
        if (effectiveProgressPlacement == PlayerProgressPlacement.ABOVE_CONTROLS) {
            progressContent()
            PlayerControlViewPointSegments(
                progressProvider = progressProvider,
                viewPoints = visibleViewPoints,
                onSeek = onSeek,
                spacing = layoutPolicy.progressSpacingDp.dp,
                modifier = progressEdgeModifier
                    .testTag("player_viewpoint_segments"),
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides layoutPolicy.playButtonSizeDp.dp,
            LocalDensity provides controlRowDensity,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("player_control_row")
                    .padding(horizontal = layoutPolicy.horizontalPaddingDp.dp)
                    .then(
                        if (isFullscreen && fullscreenToggleEndPaddingDp != null) {
                            val cutoutInsets = playerControlHorizontalInsets(viewportIsLandscape, isFullscreen)
                            Modifier.layout { measurable, constraints ->
                                val cutoutEnd = if (layoutDirection == LayoutDirection.Ltr) {
                                    cutoutInsets.getRight(this, layoutDirection)
                                } else {
                                    cutoutInsets.getLeft(this, layoutDirection)
                                }
                                val endInset = (fullscreenToggleEndPaddingDp.dp.roundToPx() -
                                    cutoutEnd - layoutPolicy.horizontalPaddingDp.dp.roundToPx())
                                    .coerceIn(0, constraints.maxWidth)
                                val placeable = measurable.measure(constraints.offset(horizontal = -endInset))
                                layout(placeable.width + endInset, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFullscreen) {
                    PlayerBottomControlGroups(
                        modifier = Modifier.weight(1f),
                        playback = {
                            PlayerControlGlassDock(
                                enabled = true,
                                modifier = Modifier.height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                    .testTag("player_playback_dock"),
                            ) {
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState())
                                        .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        Modifier.testTag("player_time_viewport"),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        ProgressTimeText(displayPosition, { progressProvider().duration }, layoutPolicy.timeFontSp)
                                    }
                                    Spacer(Modifier.width(layoutPolicy.afterTimeSpacingDp.dp))
                                    NativeDanmakuToggleButton(
                                        enabled = danmakuEnabled,
                                        onToggle = onDanmakuToggle,
                                        activeTint = AppContentColors.selectionAccent(glassMaterial),
                                        inactiveTint = Color.White.copy(alpha = 0.74f),
                                        iconSize = layoutPolicy.danmakuIconSizeDp.dp,
                                        modifier = Modifier.width(40.dp)
                                            .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                            .testTag("player_danmaku_toggle"),
                                    )
                                    AppIconButton(
                                        onClick = onDanmakuSettingsClick,
                                        modifier = Modifier.width(40.dp)
                                            .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                            .testTag("player_danmaku_settings"),
                                    ) {
                                        PiliPlusDanmakuSettingsIcon(tint = Color.White.copy(alpha = 0.9f), size = 22.dp)
                                    }
                                    AppIconButton(
                                        onClick = onDanmakuInputClick,
                                        modifier = Modifier.width(40.dp)
                                            .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                            .semantics { contentDescription = if (isLoggedIn) "发弹幕" else "登录发弹幕" }
                                            .testTag("player_danmaku_send"),
                                    ) {
                                        PiliPlusDanmakuSendIcon(
                                            tint = Color.White.copy(alpha = if (isLoggedIn) 1f else 0.6f),
                                            size = layoutPolicy.danmakuIconSizeDp.dp,
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            PlayerControlGlassDock(
                                enabled = true,
                                modifier = Modifier.height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                    .testTag("player_tools_dock"),
                            ) {
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState())
                                        .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                        .padding(start = if (viewportIsLandscape) 10.dp else 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                                ) {
                                    qualityAction()
                                    speedAction()
                                    subtitleAction()
                                    ratioAction()
                                    portraitAction()
                                }
                            }
                        },
                    )
                } else {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        PlayerControlGlassDock(
                            enabled = true,
                            modifier = Modifier.height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                .testTag("player_playback_dock"),
                        ) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState())
                                    .height(PlayerChromeLayout.CircularButtonSizeDp.dp).padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProgressTimeText(displayPosition, { progressProvider().duration }, layoutPolicy.timeFontSp)
                                if (showDanmakuToggle) {
                                    Spacer(Modifier.width(layoutPolicy.afterTimeSpacingDp.dp))
                                    NativeDanmakuToggleButton(
                                        enabled = danmakuEnabled,
                                        onToggle = onDanmakuToggle,
                                        activeTint = AppContentColors.selectionAccent(glassMaterial),
                                        inactiveTint = Color.White.copy(alpha = 0.74f),
                                        iconSize = layoutPolicy.danmakuIconSizeDp.dp,
                                        modifier = Modifier.size(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                            .testTag("player_danmaku_toggle"),
                                    )
                                    AppIconButton(
                                        onClick = onDanmakuSettingsClick,
                                        modifier = Modifier.size(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                            .testTag("player_danmaku_settings")
                                            .semantics { contentDescription = "弹幕设置" },
                                    ) {
                                        PiliPlusDanmakuSettingsIcon(
                                            tint = Color.White.copy(alpha = 0.9f),
                                            size = layoutPolicy.danmakuIconSizeDp.dp,
                                        )
                                    }
                                }
                            }
                        }
                        if (showDanmakuToggle && showCompactDanmakuSend) {
                            Spacer(Modifier.width(layoutPolicy.danmakuSwitchToInputSpacingDp.dp))
                            Box(
                                Modifier.height(PlayerChromeLayout.CircularButtonSizeDp.dp).clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable(onClick = onDanmakuInputClick)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppText(
                                    text = if (isLoggedIn) "发弹幕" else "登录发弹幕",
                                    color = Color.White.copy(alpha = if (isLoggedIn) 0.62f else 0.44f),
                                    fontSize = layoutPolicy.actionTextFontSp.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    tapToCopyEnabled = false,
                                )
                            }
                            Spacer(Modifier.width(layoutPolicy.afterInputSpacingDp.dp))
                            Spacer(Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        PlayerControlGlassDock(
                            enabled = true,
                            modifier = Modifier.height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                .testTag("player_tools_dock"),
                        ) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                    .height(PlayerChromeLayout.CircularButtonSizeDp.dp)
                                    .padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                            ) {
                                subtitleAction()
                                qualityAction()
                                speedAction()
                                ratioAction()
                                portraitAction()
                            }
                        }
                    }
                }
                Spacer(Modifier.width(actionSpacing))
                PlayerControlGlassDock(enabled = true, shape = CircleShape) {
                    Box(
                        Modifier.size(PlayerChromeLayout.CircularButtonSizeDp.dp)
                            .testTag("player_fullscreen_toggle").clickable(onClick = onToggleFullscreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                            contentDescription = if (isFullscreen) "退出横屏" else "横屏",
                            tint = Color.White,
                            modifier = Modifier.size(layoutPolicy.fullscreenIconSizeDp.dp),
                        )
                    }
                }
            }
        }
        if (effectiveProgressPlacement == PlayerProgressPlacement.BOTTOM_EDGE) {
            Spacer(Modifier.height(layoutPolicy.progressSpacingDp.dp))
            progressContent()
        }
    }
}

@Composable
private fun BottomDockTextAction(
    text: String,
    fontSp: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    enabled: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    AppText(
        text = text,
        color = color,
        fontSize = fontSp.sp,
        lineHeight = (fontSp + 2).sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        overflow = overflow,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick).wrapContentSize(Alignment.Center),
        tapToCopyEnabled = false,
    )
}
