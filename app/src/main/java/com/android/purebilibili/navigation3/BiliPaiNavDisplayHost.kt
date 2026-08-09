package com.android.purebilibili.navigation3

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.VideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.resolveVideoCardTimelineSpec
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.biliPaiMiuixNavTransition
import com.android.purebilibili.navigation3.predictiveback.miuixVideoCardNavTransition
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal class BiliPaiProgrammaticBackDispatcher {
    private var callback: (() -> Unit)? = null

    fun register(callback: () -> Unit) {
        this.callback = callback
    }

    fun unregister(callback: () -> Unit) {
        if (this.callback === callback) this.callback = null
    }

    fun dispatch(): Boolean {
        val action = callback ?: return false
        action()
        return true
    }
}

@Composable
internal fun BiliPaiNavDisplayHost(
    backStack: SnapshotStateList<BiliPaiNavKey>,
    cardTransitionEnabled: Boolean = true,
    reduceMotion: Boolean = false,
    videoSharedTransitionDurationMillis: Int,
    videoCardClock: VideoCardTransitionClock,
    predictiveBackAnimationStyle: BiliPaiPredictiveBackAnimationStyle =
        BiliPaiPredictiveBackAnimationStyle.MIUIX,
    predictiveBackExitDirection: BiliPaiPredictiveBackExitDirection =
        BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
    sourceMetadata: BiliPaiNavSourceMetadata,
    programmaticBackDispatcher: BiliPaiProgrammaticBackDispatcher,
    onBack: () -> Unit,
    onPrepareVideoCardSharedReturn: () -> Boolean = { false },
    onRelatedVideoDetailReturned: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (BiliPaiNavKey) -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val stackSnapshot = backStack.toList()
    val currentKey = stackSnapshot.lastOrNull()
    val latestOnBack by rememberUpdatedState(onBack)
    val latestPrepareReturn by rememberUpdatedState(onPrepareVideoCardSharedReturn)
    val latestRelatedReturn by rememberUpdatedState(onRelatedVideoDetailReturned)
    val performBack = remember(backStack) {
        {
            val leavingKey = backStack.lastOrNull()
            if (leavingKey is BiliPaiNavKey.VideoDetail) {
                latestPrepareReturn()
            }
            val returningFromRelated = (leavingKey as? BiliPaiNavKey.VideoDetail)
                ?.sourceRoute
                ?.substringBefore('?')
                ?.startsWith("video/") == true
            latestOnBack()
            if (returningFromRelated) latestRelatedReturn()
        }
    }

    DisposableEffect(programmaticBackDispatcher, performBack) {
        programmaticBackDispatcher.register(performBack)
        onDispose { programmaticBackDispatcher.unregister(performBack) }
    }

    val style = if (reduceMotion) {
        BiliPaiPredictiveBackAnimationStyle.NONE
    } else {
        predictiveBackAnimationStyle
    }
    val globalTransition = remember(style, predictiveBackExitDirection) {
        biliPaiMiuixNavTransition(
            animation = style,
            exitDirection = predictiveBackExitDirection,
        )
    }
    val videoCardTransition = remember(
        cardTransitionEnabled,
        reduceMotion,
        sourceMetadata.sourceBounds,
        sourceMetadata.sourceCornerDp,
        videoSharedTransitionDurationMillis,
        globalTransition,
    ) {
        if (cardTransitionEnabled && !reduceMotion) {
            miuixVideoCardNavTransition(
                sourceBounds = sourceMetadata.sourceBounds,
                sourceCornerDp = sourceMetadata.sourceCornerDp,
                durationMillis = videoSharedTransitionDurationMillis,
                fallback = globalTransition,
            )
        } else {
            globalTransition
        }
    }

    val timeline = remember(videoSharedTransitionDurationMillis) {
        resolveVideoCardTimelineSpec(videoSharedTransitionDurationMillis)
    }
    var previousStack by remember { mutableStateOf(stackSnapshot) }
    LaunchedEffect(stackSnapshot, cardTransitionEnabled, reduceMotion, timeline) {
        val previous = previousStack
        previousStack = stackSnapshot
        if (!cardTransitionEnabled || reduceMotion) {
            videoCardClock.snapClearAndIdle()
            return@LaunchedEffect
        }
        val previousTop = previous.lastOrNull()
        val openedCardDestination = isCardMorphDestinationNavKey(currentKey) &&
            stackSnapshot.size > previous.size
        val returnedFromCardDestination = isCardMorphDestinationNavKey(previousTop) &&
            stackSnapshot.size < previous.size
        when {
            openedCardDestination -> {
                videoCardClock.beginOpening(sourceMetadata.sourceRoute)
                videoCardClock.snapFallback(0f)
                videoCardClock.animateFallbackTo(
                    target = 1f,
                    durationMillis = timeline.durationMillis,
                    easing = timeline.enterEasing,
                )
                videoCardClock.markHeld()
            }
            returnedFromCardDestination -> {
                videoCardClock.beginReturning(sourceMetadata.sourceRoute, startDepth = 1f)
                videoCardClock.snapFallback(1f)
                videoCardClock.animateFallbackTo(
                    target = 0f,
                    durationMillis = timeline.durationMillis,
                    easing = timeline.returnEasing,
                )
                videoCardClock.markIdle()
            }
        }
    }

    val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
    val roundAllCorners = style == BiliPaiPredictiveBackAnimationStyle.AOSP ||
        style == BiliPaiPredictiveBackAnimationStyle.SCALE ||
        style == BiliPaiPredictiveBackAnimationStyle.CLASSIC
    val backdropColor = MiuixTheme.colorScheme.surface
    val effects = remember(navCornerRadius, roundAllCorners, backdropColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
            cornerClipMode = if (roundAllCorners) {
                NavCornerClipMode.All
            } else {
                NavCornerClipMode.Leading
            },
            dimAmount = 0.5f,
            backdropColor = backdropColor,
            blockInputDuringTransition = false,
        )
    }
    val swipeBackDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
    }
    val interceptPredictiveBack =
        style == BiliPaiPredictiveBackAnimationStyle.NONE && backStack.size > 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppSurfaceTokens.groupedListContainer()),
    ) {
        @Suppress("UNCHECKED_CAST")
        NavDisplay(
            backStack = backStack as NavBackStack,
            onBack = performBack,
            transition = globalTransition,
            effects = effects,
        ) {
            biliPaiNavEntries(
                swipeBackDirection = swipeBackDirection,
                videoCardTransition = videoCardTransition,
            ) { key ->
                BiliPaiMiuixNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = performBack,
                ) {
                    CompositionLocalProvider(
                        LocalVideoCardSharedElementSourceRoute provides key.toLegacyRoute(),
                        LocalVideoCardTransitionClock provides videoCardClock,
                    ) {
                        ProvideMiuixNavViewModelApplicationExtras(application) {
                            content(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiliPaiMiuixNavEntry(
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}

@Composable
private fun ProvideMiuixNavViewModelApplicationExtras(
    application: Application,
    content: @Composable () -> Unit,
) {
    val navEntryOwner = LocalViewModelStoreOwner.current
    if (navEntryOwner == null) {
        content()
        return
    }
    val patchedOwner = remember(navEntryOwner, application) {
        buildMiuixNavViewModelStoreOwner(navEntryOwner, application)
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides patchedOwner) {
        content()
    }
}

private fun buildMiuixNavViewModelStoreOwner(
    navEntryOwner: ViewModelStoreOwner,
    application: Application,
): ViewModelStoreOwner {
    val defaultFactoryOwner = navEntryOwner as? HasDefaultViewModelProviderFactory
    val defaultCreationExtras = defaultFactoryOwner?.defaultViewModelCreationExtras
        ?: CreationExtras.Empty
    val patchedCreationExtras = MutableCreationExtras(defaultCreationExtras).apply {
        set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application)
    }
    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = navEntryOwner.viewModelStore
        override val defaultViewModelProviderFactory =
            defaultFactoryOwner?.defaultViewModelProviderFactory
                ?: ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        override val defaultViewModelCreationExtras: CreationExtras = patchedCreationExtras
    }
}
