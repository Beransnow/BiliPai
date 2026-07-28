package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.android.purebilibili.core.ui.performance.PerformanceDebugCounter
import com.android.purebilibili.core.ui.performance.PerformanceObservability
import com.android.purebilibili.core.ui.performance.PerformanceTraceSection
import com.android.purebilibili.feature.video.danmaku.AdvancedDanmakuData
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuItem
import com.android.purebilibili.feature.video.danmaku.DanmakuPlaybackClock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

@Composable
internal fun rememberDanmakuPlaybackClock(
    advancedItems: List<AdvancedDanmakuData>,
    commandItems: List<CommandDanmakuItem>,
): DanmakuPlaybackClock = remember(advancedItems, commandItems) {
    DanmakuPlaybackClock(
        advancedItems = advancedItems,
        commandItems = commandItems,
    )
}

/**
 * Owns the sole Player.Listener for both Compose danmaku overlays.
 *
 * The frame loop exists only while an advanced item is continuously moving. Command-only periods
 * sleep until their next start/end boundary.
 */
@Composable
internal fun DanmakuPlaybackClockEffect(
    clock: DanmakuPlaybackClock,
    player: Player,
    overlayVisible: Boolean,
    routeResumed: Boolean,
) {
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var playbackSpeed by remember(player) {
        mutableFloatStateOf(player.playbackParameters.speed.coerceAtLeast(MIN_PLAYBACK_SPEED))
    }
    var discontinuityGeneration by remember(player) { mutableIntStateOf(0) }

    DisposableEffect(player, clock) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                clock.seekTo(player.currentPosition)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.isPlaying
                clock.seekTo(player.currentPosition)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                playbackSpeed = playbackParameters.speed.coerceAtLeast(MIN_PLAYBACK_SPEED)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                clock.seekTo(newPosition.positionMs)
                discontinuityGeneration += 1
            }
        }
        clock.seekTo(player.currentPosition)
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(
        clock,
        player,
        overlayVisible,
        routeResumed,
        isPlaying,
        playbackSpeed,
        discontinuityGeneration,
        clock.hasContinuousAdvancedMotion,
    ) {
        clock.seekTo(player.currentPosition)
        if (!overlayVisible || !routeResumed || !isPlaying) return@LaunchedEffect
        PerformanceObservability.trace(PerformanceTraceSection.DANMAKU_CLOCK) {
            PerformanceObservability.increment(PerformanceDebugCounter.DANMAKU_CLOCK_STARTED)
            clock.advanceTo(player.currentPosition)
        }
        try {
            while (true) {
                clock.advanceTo(player.currentPosition)
                if (clock.hasContinuousAdvancedMotion) {
                    withFrameNanos { /* The player remains the authoritative media clock. */ }
                    continue
                }

                val currentPositionMs = player.currentPosition
                val nextBoundaryMs = clock.nextBoundaryAfter(currentPositionMs) ?: awaitCancellation()
                val mediaDelayMs = (nextBoundaryMs - currentPositionMs).coerceAtLeast(1L)
                val wallDelayMs = (mediaDelayMs / playbackSpeed)
                    .toLong()
                    .coerceIn(1L, MAX_BOUNDARY_SLEEP_MS)
                delay(wallDelayMs)
            }
        } finally {
            PerformanceObservability.trace(PerformanceTraceSection.DANMAKU_CLOCK) {
                PerformanceObservability.increment(PerformanceDebugCounter.DANMAKU_CLOCK_STOPPED)
            }
        }
    }
}

private const val MIN_PLAYBACK_SPEED = 0.05f
private const val MAX_BOUNDARY_SLEEP_MS = 10_000L
