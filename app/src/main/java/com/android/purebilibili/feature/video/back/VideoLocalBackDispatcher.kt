package com.android.purebilibili.feature.video.back

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class VideoBackMotion {
    NONE,
    BOTTOM_SHEET,
    SIDE_PANEL,
    FULLSCREEN,
}

/**
 * Local video surfaces use explicit priorities instead of relying on Compose handler order.
 * Higher values are consumed first.
 */
internal enum class VideoLocalBackTarget(
    val priority: Int,
    val motion: VideoBackMotion,
) {
    COMMENT_CONVERSATION(1_000, VideoBackMotion.SIDE_PANEL),
    COMMENT_THREAD(900, VideoBackMotion.SIDE_PANEL),
    COMMENT_SHEET(800, VideoBackMotion.BOTTOM_SHEET),
    PLAYER_END_DRAWER(700, VideoBackMotion.SIDE_PANEL),
    PLAYER_PAGE_SELECTOR(690, VideoBackMotion.SIDE_PANEL),
    PLAYER_CHAPTER_LIST(680, VideoBackMotion.SIDE_PANEL),
    PLAYER_ASPECT_RATIO(670, VideoBackMotion.SIDE_PANEL),
    DANMAKU_COMPOSER(660, VideoBackMotion.BOTTOM_SHEET),
    PORTRAIT_UP_PREVIEW(600, VideoBackMotion.BOTTOM_SHEET),
    PORTRAIT_DETAIL(590, VideoBackMotion.BOTTOM_SHEET),
    PLAYLIST_QUEUE(500, VideoBackMotion.BOTTOM_SHEET),
    EXIT_PORTRAIT_FULLSCREEN(400, VideoBackMotion.FULLSCREEN),
    EXIT_LANDSCAPE_FULLSCREEN(300, VideoBackMotion.FULLSCREEN),
    SHELL_FULLSCREEN_PLAYER(200, VideoBackMotion.FULLSCREEN),
}

internal fun interface VideoLocalBackRegistration {
    fun dispose()
}

private data class VideoBackEntry(
    val key: Any,
    val target: VideoLocalBackTarget,
    val priority: Int,
    val motion: VideoBackMotion,
    val registrationOrder: Long,
    val onCommitted: () -> Unit,
)

/**
 * State machine shared by every in-tree overlay owned by one video surface.
 *
 * A gesture snapshots its target in [beginGesture]. Later visibility changes cannot retarget the
 * in-flight gesture. Continuous progress is exposed through stable providers so callers can read
 * it from layout/draw/layer phases without invalidating the page composition on every frame.
 */
@Stable
internal class VideoLocalBackDispatcher {
    private val entries = mutableStateMapOf<Any, VideoBackEntry>()
    private var nextRegistrationOrder = 0L
    private var frozenEntry by mutableStateOf<VideoBackEntry?>(null)
    private var gestureProgress by mutableFloatStateOf(0f)
    private val progressProviders = VideoLocalBackTarget.entries.associateWith { target ->
        { progressFor(target) }
    }

    val activeTarget: VideoLocalBackTarget?
        get() = activeEntry()?.target

    val frozenTarget: VideoLocalBackTarget?
        get() = frozenEntry?.target

    val isGestureInProgress: Boolean
        get() = frozenEntry != null

    fun register(
        key: Any,
        target: VideoLocalBackTarget,
        priority: Int = target.priority,
        motion: VideoBackMotion = target.motion,
        onCommitted: () -> Unit,
    ): VideoLocalBackRegistration {
        val order = nextRegistrationOrder++
        entries[key] = VideoBackEntry(
            key = key,
            target = target,
            priority = priority,
            motion = motion,
            registrationOrder = order,
            onCommitted = onCommitted,
        )
        return VideoLocalBackRegistration {
            val current = entries[key]
            if (current?.registrationOrder == order) {
                entries.remove(key)
            }
        }
    }

    fun beginGesture(): VideoLocalBackTarget? {
        if (frozenEntry == null) {
            frozenEntry = activeEntry()
            gestureProgress = 0f
        }
        return frozenEntry?.target
    }

    fun updateGestureProgress(progress: Float) {
        if (frozenEntry == null) beginGesture()
        if (frozenEntry != null) {
            gestureProgress = progress.coerceIn(0f, 1f)
        }
    }

    fun cancelGesture(): Boolean {
        val hadFrozenTarget = frozenEntry != null
        gestureProgress = 0f
        frozenEntry = null
        return hadFrozenTarget
    }

    fun completeGesture(): Boolean {
        val completedEntry = frozenEntry ?: return false
        // Clear first: callbacks commonly hide their own overlay and unregister synchronously.
        gestureProgress = 0f
        frozenEntry = null
        completedEntry.onCommitted()
        return true
    }

    fun requestBack(expectedTarget: VideoLocalBackTarget? = null): Boolean {
        val entry = activeEntry() ?: return false
        if (expectedTarget != null && entry.target != expectedTarget) return false
        frozenEntry = entry
        return completeGesture()
    }

    fun progressProvider(target: VideoLocalBackTarget): () -> Float =
        checkNotNull(progressProviders[target])

    fun progressFor(target: VideoLocalBackTarget): Float =
        if (frozenEntry?.target == target) gestureProgress else 0f

    private fun activeEntry(): VideoBackEntry? = entries.values.maxWithOrNull(
        compareBy<VideoBackEntry> { it.priority }
            .thenBy { it.registrationOrder },
    )
}

internal val LocalVideoBackDispatcher = staticCompositionLocalOf<VideoLocalBackDispatcher?> { null }

@Composable
internal fun rememberVideoLocalBackDispatcher(): VideoLocalBackDispatcher =
    remember { VideoLocalBackDispatcher() }

/**
 * Provides one NavigationEvent handler for all in-tree overlays in [content]. The handler is
 * composed after the content so it wins over the route handler while a local target is active.
 */
@Composable
internal fun VideoLocalBackHost(
    dispatcher: VideoLocalBackDispatcher = rememberVideoLocalBackDispatcher(),
    content: @Composable () -> Unit,
) {
    val backEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackEnabled = LocalPredictiveBackGestureEnabled.current

    CompositionLocalProvider(LocalVideoBackDispatcher provides dispatcher) {
        content()

        val activeTarget = dispatcher.activeTarget
        DisposableEffect(dispatcher) {
            onDispose { dispatcher.cancelGesture() }
        }
        androidx.compose.runtime.LaunchedEffect(backEventState, predictiveBackEnabled) {
            snapshotFlow {
                if (!predictiveBackEnabled) {
                    null
                } else {
                    (backEventState.transitionState as? NavigationEventTransitionState.InProgress)
                        ?.latestEvent
                        ?.progress
                }
            }
                .distinctUntilChanged()
                .collect { progress ->
                    if (progress != null) {
                        dispatcher.updateGestureProgress(progress)
                    }
                }
        }
        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = activeTarget != null,
            reportPredictiveProgress = predictiveBackEnabled,
            onBackCancelled = { commitTransition: () -> Unit ->
                dispatcher.cancelGesture()
                commitTransition()
            },
            onBackCompleted = { commitTransition: () -> Unit ->
                try {
                    dispatcher.beginGesture()
                    dispatcher.completeGesture()
                } finally {
                    commitTransition()
                }
            },
        )
    }
}

/** Registers a low-frequency visibility target without exposing frame values to composition. */
@Composable
internal fun VideoLocalBackTargetEffect(
    key: Any,
    target: VideoLocalBackTarget,
    enabled: Boolean,
    priority: Int = target.priority,
    motion: VideoBackMotion = target.motion,
    onCommitted: () -> Unit,
) {
    val dispatcher = LocalVideoBackDispatcher.current
    val currentOnCommitted by rememberUpdatedState(onCommitted)
    // Standalone previews and isolated component hosts do not install VideoLocalBackHost. Keep
    // their legacy back behavior without adding a second handler to the real video-detail tree.
    BackHandler(enabled = dispatcher == null && enabled) {
        currentOnCommitted()
    }
    DisposableEffect(dispatcher, key, target, enabled, priority, motion) {
        val registration = if (dispatcher != null && enabled) {
            dispatcher.register(
                key = key,
                target = target,
                priority = priority,
                motion = motion,
                onCommitted = { currentOnCommitted() },
            )
        } else {
            null
        }
        onDispose { registration?.dispose() }
    }
}

/**
 * Returns the action used by close buttons, top bars, and scrims for [target]. When the unified
 * host is present, a stale control cannot close a lower-priority surface: [requestBack] only
 * commits when [target] is still the active target. Isolated previews retain their direct action.
 */
@Composable
internal fun rememberVideoLocalBackAction(
    target: VideoLocalBackTarget,
    onCommitted: () -> Unit,
): () -> Unit {
    val dispatcher = LocalVideoBackDispatcher.current
    val currentOnCommitted by rememberUpdatedState(onCommitted)
    return remember(dispatcher, target) {
        {
            if (dispatcher == null) {
                currentOnCommitted()
            } else {
                dispatcher.requestBack(expectedTarget = target)
            }
        }
    }
}
