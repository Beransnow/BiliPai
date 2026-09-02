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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionExposure
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.LocalVideoCardMorphProgressReporter
import com.android.purebilibili.core.ui.transition.LocalVideoCardSourceMediaSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardMorphProgressReporter
import com.android.purebilibili.core.ui.transition.LocalPredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.PredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.VideoCardTransitionHostDepthLayer
import com.android.purebilibili.core.ui.transition.VideoCardTransitionNavBackdrop
import com.android.purebilibili.core.ui.transition.rememberVideoCardTransitionSnapshotHandle
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionExposure
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionDiagnostics
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackGestureBlurProgress
import com.android.purebilibili.core.ui.transition.shouldReleaseHostOwnedDepthLayer
import com.android.purebilibili.core.ui.transition.shouldShowVideoCardTransitionNavBackdrop
import com.android.purebilibili.core.ui.transition.shouldUseHostOwnedVideoCardTransitionSnapshot
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.MIUIX_PREDICTIVE_BACK_DEFAULT_MAX_PROGRESS_PERCENT
import com.android.purebilibili.navigation3.predictiveback.biliPaiMiuixNavTransition
import com.android.purebilibili.navigation3.predictiveback.miuixSharedElementNavTransition
import com.android.purebilibili.navigation3.predictiveback.MiuixVideoCardTransitionProgress
import com.android.purebilibili.navigation3.predictiveback.shouldUseMiuixPredictiveBackProgress
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
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
    videoTransitionRealtimeBlurEnabled: Boolean = false,
    isLightBackground: Boolean = false,
    reduceMotion: Boolean = false,
    videoSharedTransitionDurationMillis: Int,
    videoCardClock: VideoCardTransitionClock,
    predictiveBackAnimationStyle: BiliPaiPredictiveBackAnimationStyle =
        BiliPaiPredictiveBackAnimationStyle.MIUIX,
    predictiveBackExitDirection: BiliPaiPredictiveBackExitDirection =
        BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
    miuixTransitionBlurEnabled: Boolean = true,
    miuixPredictiveBackMaxProgressPercent: Int =
        MIUIX_PREDICTIVE_BACK_DEFAULT_MAX_PROGRESS_PERCENT,
    videoSharedReturnGestureFollowEnabled: Boolean = true,
    sourceMetadata: BiliPaiNavSourceMetadata,
    programmaticBackDispatcher: BiliPaiProgrammaticBackDispatcher,
    preferWholeCardReturn: Boolean = false,
    onBack: () -> Unit,
    onPrepareVideoCardSharedReturn: () -> Boolean = { false },
    onRelatedVideoDetailReturned: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (BiliPaiNavKey) -> Unit,
) = Box(modifier = modifier) {
    val heroMotion = remember(videoSharedTransitionDurationMillis, reduceMotion) {
        resolveVideoHeroMotionSpec(
            baseDurationMillis = videoSharedTransitionDurationMillis,
            reducedMotion = reduceMotion,
        )
    }
    val application = LocalContext.current.applicationContext as Application
    val speedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val diagnosticConfiguration by rememberUpdatedState(
        "speed=${speedSettings.speed} custom_duration=${speedSettings.customDurationMillis} " +
            "realtime_blur=$videoTransitionRealtimeBlurEnabled gesture_follow=$videoSharedReturnGestureFollowEnabled " +
            "predictive_style=$predictiveBackAnimationStyle reduced_motion=$reduceMotion",
    )
    val stackSnapshot = backStack.toList()
    val currentKey = stackSnapshot.lastOrNull()
    // The destination key already carries its source route. Use it as a synchronous fallback for
    // the click frame so a delayed return-session recomposition cannot select the generic Miuix
    // transition before the shared source metadata arrives.
    val cardTransitionSourceRoute = sourceMetadata.sourceRoute
        ?: resolveCardMorphDestinationSourceRoute(currentKey)
    val latestOnBack by rememberUpdatedState(onBack)
    val latestPrepareReturn by rememberUpdatedState(onPrepareVideoCardSharedReturn)
    val latestRelatedReturn by rememberUpdatedState(onRelatedVideoDetailReturned)
    val latestPreferWholeCardReturn by rememberUpdatedState(preferWholeCardReturn)
    val cardMorphMode = resolveBiliPaiVideoCardTransitionMode(
        cardTransitionEnabled = cardTransitionEnabled,
        reduceMotion = reduceMotion,
        sourceRoute = cardTransitionSourceRoute,
    )
    val cardMorphAvailable = cardMorphMode != BiliPaiVideoCardTransitionMode.NONE
    var relatedReturnRestorePending by remember { mutableStateOf(false) }
    var relatedReturnTransitionObserved by remember { mutableStateOf(false) }
    val performBack = remember(
        backStack,
        cardMorphAvailable,
        cardTransitionSourceRoute,
    ) {
        {
            val leavingKey = backStack.lastOrNull()
            if (leavingKey is BiliPaiNavKey.VideoDetail) {
                latestPrepareReturn()
                if (cardMorphAvailable) {
                    videoCardClock.beginReturning(cardTransitionSourceRoute, videoCardClock.depthProgress())
                }
            }
            val returningFromRelated = (leavingKey as? BiliPaiNavKey.VideoDetail)
                ?.sourceRoute
                ?.substringBefore('?')
                ?.startsWith("video/") == true
            latestOnBack()
            if (returningFromRelated) {
                if (cardMorphAvailable) {
                    relatedReturnTransitionObserved = false
                    relatedReturnRestorePending = true
                } else {
                    latestRelatedReturn()
                }
            }
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
    val globalTransition = remember(
        style,
        predictiveBackExitDirection,
        isLightBackground,
        miuixTransitionBlurEnabled,
        miuixPredictiveBackMaxProgressPercent,
    ) {
        biliPaiMiuixNavTransition(
            animation = style,
            exitDirection = predictiveBackExitDirection,
            isLightBackground = isLightBackground,
            miuixTransitionBlurEnabled = miuixTransitionBlurEnabled,
            miuixPredictiveBackMaxProgressPercent =
                miuixPredictiveBackMaxProgressPercent,
        )
    }
    val predictiveBackExcludedTransition = remember(
        globalTransition,
        style,
        predictiveBackExitDirection,
        isLightBackground,
        miuixTransitionBlurEnabled,
    ) {
        if (shouldUseMiuixPredictiveBackProgress(style, enabled = true)) {
            biliPaiMiuixNavTransition(
                animation = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                exitDirection = predictiveBackExitDirection,
                isLightBackground = isLightBackground,
                miuixTransitionBlurEnabled = miuixTransitionBlurEnabled,
                miuixPredictiveBackProgressEnabled = false,
            )
        } else {
            globalTransition
        }
    }
    // A restored parent session must not keep the departed child's scope at depth -1.
    val videoCardTransitionProgress = remember(sourceMetadata.sourceKey) { MiuixVideoCardTransitionProgress() }
    val returningProvider = remember(videoCardClock) {
        { videoCardClock.phase != VideoCardTransitionBackgroundPhase.OPENING }
    }
    val videoCardTransition = remember(
        cardMorphAvailable,
        videoSharedTransitionDurationMillis,
        heroMotion,
        videoCardTransitionProgress,
        predictiveBackExcludedTransition,
    ) {
        if (cardMorphAvailable) {
            miuixSharedElementNavTransition(
                durationMillis = videoSharedTransitionDurationMillis,
                fallback = predictiveBackExcludedTransition,
                progress = videoCardTransitionProgress,
                heroMotionSpec = heroMotion,
                returningProvider = returningProvider,
            )
        } else {
            predictiveBackExcludedTransition
        }
    }
    val fullscreenVideoCardTransition = videoCardTransition

    val videoCardMorphProgressReporter = remember(videoCardClock) {
        VideoCardMorphProgressReporter(videoCardClock::reportSharedMorphProgress)
    }
    var previousStack by remember { mutableStateOf(stackSnapshot) }
    LaunchedEffect(stackSnapshot, cardMorphAvailable) {
        val previous = previousStack
        previousStack = stackSnapshot
        if (!cardMorphAvailable) {
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
                videoCardClock.beginOpeningIfNeeded(cardTransitionSourceRoute)
            }
            returnedFromCardDestination -> {
                videoCardClock.beginReturning(cardTransitionSourceRoute,
                    startDepth = videoCardClock.depthProgress())
            }
        }
    }
    LaunchedEffect(
        currentKey,
        cardMorphAvailable,
        videoSharedTransitionDurationMillis,
        videoCardClock.phase,
    ) {
        if (!cardMorphAvailable) {
            return@LaunchedEffect
        }
        when {
            isCardMorphDestinationNavKey(currentKey) &&
                videoCardClock.phase == VideoCardTransitionBackgroundPhase.OPENING -> {
                // If an unmatched source never starts shared bounds, detail still settles to the
                // normal held depth instead of leaving the clock in an opening limbo.
                delay(videoSharedTransitionDurationMillis.coerceAtLeast(0).toLong() + 64L)
                if (
                    isCardMorphDestinationNavKey(backStack.lastOrNull()) &&
                    videoCardClock.phase == VideoCardTransitionBackgroundPhase.OPENING
                ) {
                    videoCardClock.markHeld()
                }
            }
            !isCardMorphDestinationNavKey(currentKey) &&
                videoCardClock.phase != VideoCardTransitionBackgroundPhase.IDLE -> {
                // A missing shared target or interrupted NavDisplay retention must never strand
                // the returned source page in the blurred depth state.
                delay(videoSharedTransitionDurationMillis.coerceAtLeast(0).toLong() + 64L)
                if (
                    !isCardMorphDestinationNavKey(backStack.lastOrNull()) &&
                    videoCardClock.phase != VideoCardTransitionBackgroundPhase.IDLE
                ) {
                    videoCardClock.snapClearAndIdle()
                }
            }
        }
    }
    LaunchedEffect(cardMorphAvailable, videoCardTransitionProgress, heroMotion, sourceMetadata.sourceKey) {
        if (!cardMorphAvailable) return@LaunchedEffect
        // Coarse states only: no frame-rate composition reads or competing fallback jobs.
        snapshotFlow { videoCardTransitionProgress.settleStateOrNull() }.collect { state ->
            if (state != null) {
                videoCardClock.followMiuixNavigationLifecycle(
                    state,
                    videoCardTransitionProgress.releaseVelocity(),
                )
                VideoCardTransitionDiagnostics.onMotionPhase(
                    state, heroMotion, sourceMetadata.sourceLayout, diagnosticConfiguration,
                )
            }
        }
    }

    val videoCardSnapshotHandle = rememberVideoCardTransitionSnapshotHandle()
    val transitionMotionTier = if (reduceMotion) MotionTier.Reduced else MotionTier.Normal
    val videoCardProgressProvider = remember(
        videoCardClock,
    ) {
        { videoCardClock.depthProgress() }
    }
    val videoCardGestureProvider = remember(cardMorphAvailable, videoCardTransitionProgress) {
        { cardMorphAvailable && videoCardTransitionProgress.isGestureInProgress() }
    }
    val videoCardExposureProvider = remember(videoCardClock, videoCardGestureProvider) {
        {
            resolveVideoCardTransitionExposure(
                phase = videoCardClock.phase,
                predictiveBackInProgress = videoCardGestureProvider(),
                gestureRestoreInProgress = videoCardClock.gestureRestoreInProgress,
            )
        }
    }
    val effectiveVideoCardExposure = videoCardExposureProvider()
    LaunchedEffect(
        relatedReturnRestorePending,
        effectiveVideoCardExposure,
        cardMorphAvailable,
    ) {
        val restoreDecision = resolveRelatedReturnSourceRestoreDecision(
            restorePending = relatedReturnRestorePending,
            transitionObserved = relatedReturnTransitionObserved,
            cardMorphAvailable = cardMorphAvailable,
            exposure = effectiveVideoCardExposure,
        )
        relatedReturnTransitionObserved = restoreDecision.transitionObserved
        if (restoreDecision.shouldRestore) {
            // The nested source geometry remains immutable through the complete predictive
            // settle. Only arm the parent's older session after the navigation driver is idle.
            relatedReturnRestorePending = false
            relatedReturnTransitionObserved = false
            latestRelatedReturn()
        }
    }
    LaunchedEffect(effectiveVideoCardExposure) {
        if (shouldReleaseHostOwnedDepthLayer(effectiveVideoCardExposure)) {
            videoCardSnapshotHandle.releaseSession()
        }
    }
    val currentBackTarget = stackSnapshot.getOrNull(stackSnapshot.lastIndex - 1)
    val showVideoCardNavBackdrop = shouldShowVideoCardTransitionNavBackdrop(
        cardTransitionEnabled = cardMorphAvailable,
        exposure = effectiveVideoCardExposure,
        isVideoDetailOnStack = isCardMorphDestinationNavKey(currentKey),
        isReturningToVideoDetail = isCardMorphDestinationNavKey(currentBackTarget),
    )
    val transitionBackgroundState = remember(
        cardTransitionSourceRoute,
        sourceMetadata.sourceCornerDp,
        videoCardProgressProvider,
        videoCardExposureProvider,
        videoCardSnapshotHandle,
        transitionMotionTier,
        isLightBackground,
    ) {
        VideoCardTransitionBackgroundState(
            progressProvider = videoCardProgressProvider,
            sourceRouteProvider = { cardTransitionSourceRoute },
            phaseProvider = { videoCardClock.phase },
            exposureProvider = videoCardExposureProvider,
            sourceCornerDpProvider = { sourceMetadata.sourceCornerDp },
            snapshotHandle = videoCardSnapshotHandle,
            isReturnGestureInProgressProvider = videoCardGestureProvider,
            isGestureRestoreInProgressProvider = { videoCardClock.gestureRestoreInProgress },
            preferWholeCardReturnProvider = { latestPreferWholeCardReturn },
            motionTierProvider = { transitionMotionTier },
            isLightBackgroundProvider = { isLightBackground },
        )
    }
    // 恢复 0.2.2 的预测返回背景链路：目标返回页（栈前一 key）在预测返回手势中
    // 随手势进度模糊/消退，迁移到 Miuix 导航时该 provide 曾丢失。
    val predictiveBackBackgroundState = remember(
        cardMorphAvailable,
        videoCardTransitionProgress,
        currentBackTarget,
        transitionMotionTier,
        isLightBackground,
    ) {
        PredictiveBackBackgroundState(
            progressProvider = {
                videoCardTransitionProgress.gestureBackProgress()
                    ?.takeIf { cardMorphAvailable }
                    ?.let { resolvePredictiveBackGestureBlurProgress(it) }
                    ?: 0f
            },
            targetKeyProvider = { currentBackTarget },
            motionTierProvider = { transitionMotionTier },
            isLightBackgroundProvider = { isLightBackground },
        )
    }

    val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
    val roundAllCorners = style == BiliPaiPredictiveBackAnimationStyle.AOSP ||
        style == BiliPaiPredictiveBackAnimationStyle.SCALE ||
        style == BiliPaiPredictiveBackAnimationStyle.CLASSIC
    // Keep normal Miuix page-preview effects. Card clipping belongs only to the shared overlay.
    val enableHostCornerClip = true
    val hostDimAmount = 0.5f
    val backdropColor = MiuixTheme.colorScheme.surface
    val effects = remember(
        navCornerRadius,
        roundAllCorners,
        enableHostCornerClip,
        hostDimAmount,
        backdropColor,
    ) {
        NavDisplayEffects(
            enableCornerClip = enableHostCornerClip,
            cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
            cornerClipMode = if (roundAllCorners) {
                NavCornerClipMode.All
            } else {
                NavCornerClipMode.Leading
            },
            dimAmount = hostDimAmount,
            backdropColor = backdropColor,
            blockInputDuringTransition = false,
        )
    }
    // 全屏滑动返回默认关闭（仅系统边缘预测返回），可在设置中开启。
    // 开启后仅对列表/设置等纵向页面生效，播放器、详情、WebView 等
    // 横滑冲突页面始终禁用（见 BiliPaiNavEntryProvider）。
    val fullScreenSwipeBackEnabled by
        com.android.purebilibili.core.store.SettingsManager
            .getFullScreenSwipeBackEnabled(LocalContext.current)
            .collectAsStateWithLifecycle(initialValue = false)
    val swipeBackDirection = if (fullScreenSwipeBackEnabled) {
        when (LocalLayoutDirection.current) {
            LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
            LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        }
    } else {
        NavSwipeDirection.None
    }
    val interceptPredictiveBack =
        style == BiliPaiPredictiveBackAnimationStyle.NONE && backStack.size > 1
    val globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (globalWallpaperVisible) {
                    Color.Transparent
                } else {
                    AppSurfaceTokens.groupedListContainer()
                }
            ),
    ) {
        VideoCardTransitionHostDepthLayer(
            enabled = cardMorphAvailable &&
                shouldUseHostOwnedVideoCardTransitionSnapshot(cardTransitionSourceRoute),
            snapshotHandle = videoCardSnapshotHandle,
            progressProvider = videoCardProgressProvider,
            phaseProvider = { videoCardClock.phase },
            exposureProvider = videoCardExposureProvider,
            isGestureRestoreInProgressProvider = { videoCardClock.gestureRestoreInProgress },
            motionTierProvider = { transitionMotionTier },
            isLightBackgroundProvider = { isLightBackground },
            realtimeBlurEnabledProvider = { videoTransitionRealtimeBlurEnabled },
        )
        VideoCardTransitionNavBackdrop(
            visible = showVideoCardNavBackdrop,
            progressProvider = videoCardProgressProvider,
            phase = videoCardClock.phase,
            isLightBackground = isLightBackground,
        )
        @Suppress("UNCHECKED_CAST")
        NavDisplay(
            backStack = backStack as NavBackStack,
            onBack = performBack,
            transition = globalTransition,
            effects = effects,
        ) {
            biliPaiNavEntries(
                swipeBackDirection = swipeBackDirection,
                predictiveBackExcludedTransition = predictiveBackExcludedTransition,
                videoCardTransition = videoCardTransition,
                fullscreenVideoCardTransition = fullscreenVideoCardTransition,
            ) { key ->
                val participatesInSharedCardTransition = cardMorphAvailable && (
                    isCardMorphDestinationNavKey(key) ||
                        stackSnapshot.zipWithNext().any { (source, destination) ->
                            source == key && isCardMorphDestinationNavKey(destination)
                        } ||
                        previousStack.zipWithNext().any { (source, destination) ->
                            source == key && isCardMorphDestinationNavKey(destination)
                        }
                    )
                BiliPaiMiuixNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = performBack,
                ) {
                    val entryContent: @Composable () -> Unit = {
                        CompositionLocalProvider(
                            LocalVideoCardSharedElementSourceRoute provides key.toLegacyRoute(),
                            LocalVideoCardTransitionClock provides videoCardClock,
                            LocalVideoCardMorphProgressReporter provides videoCardMorphProgressReporter,
                            LocalVideoCardSourceMediaSnapshot provides sourceMetadata.sourceChromeSnapshot,
                            LocalVideoCardTransitionBackgroundState provides transitionBackgroundState,
                            LocalPredictiveBackBackgroundState provides predictiveBackBackgroundState,
                        ) {
                            ProvideMiuixNavViewModelApplicationExtras(application) {
                                content(key)
                            }
                        }
                    }
                    // Keep the bridge mounted for the complete lifetime of every NavDisplay entry.
                    // A source card must already own an AnimatedVisibilityScope on the frame before
                    // the click; conditionally adding the bridge after navigation is too late for
                    // SharedTransitionLayout to discover the source/target pair.
                    MiuixSharedTransitionEntryBridge(
                        entryKey = key,
                        initiallyVisible = !participatesInSharedCardTransition ||
                            previousStack.lastOrNull() == key,
                        participatesInSharedTransition = participatesInSharedCardTransition,
                        isTopEntry = currentKey == key,
                        isPredictiveBackTarget = currentBackTarget == key,
                        predictiveBackPreviewInProgress =
                            effectiveVideoCardExposure == VideoCardTransitionExposure.BackPreview,
                        content = entryContent,
                    )
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
