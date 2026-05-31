package com.android.purebilibili.navigation

import androidx.lifecycle.Lifecycle
import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun shouldStopPlaybackEagerlyOnVideoRouteExit(
    fromRoute: String?,
    toRoute: String?
): Boolean {
    if (toRoute.isNullOrBlank()) return false
    return isVideoDetailRoute(fromRoute) &&
        !isVideoDetailRoute(toRoute) &&
        toRoute != ScreenRoutes.AudioMode.route
}

internal fun shouldDeferBottomBarRevealOnVideoReturn(
    isReturningFromDetail: Boolean,
    activeBottomTabRoute: String?,
    cardTransitionEnabled: Boolean
): Boolean {
    // 仅在共享缩放路径下延迟显示底栏；card 关闭（横向滑动）时内容连同底栏整体滑入，不应延后弹出。
    if (!cardTransitionEnabled) return false
    if (!isReturningFromDetail) return false
    return isVideoCardReturnTargetRoute(activeBottomTabRoute)
}

internal fun shouldAutoReleaseBottomBarRevealOnVideoReturn(
    isReturningFromDetail: Boolean,
    activeBottomTabRoute: String?
): Boolean {
    if (!isReturningFromDetail) return false
    // 首页由 HomeScreen 动画完成 LaunchedEffect 恢复底栏，不在 AppNavigation 层面计时
    if (activeBottomTabRoute == ScreenRoutes.Home.route) return false
    return isVideoCardReturnTargetRoute(activeBottomTabRoute)
}

internal fun resolveVideoReturnBottomBarRestoreDelayMs(
    cardTransitionEnabled: Boolean,
    isQuickReturnFromDetail: Boolean
): Long = 0L

internal fun resolveVideoReturnBottomBarHideSuppressionMs(
    cardTransitionEnabled: Boolean
): Long {
    return if (cardTransitionEnabled) 200L else 80L
}

internal fun shouldSuppressBottomBarHideAfterDeferredReveal(
    hadDeferredReveal: Boolean,
    isBottomBarDestination: Boolean,
    shouldDeferReveal: Boolean
): Boolean {
    return hadDeferredReveal && isBottomBarDestination && !shouldDeferReveal
}

internal fun shouldPrimeBottomBarHiddenBeforeVideoNavigation(
    sourceRoute: String?,
    visibleBottomBarRoutes: Set<String>,
    useSideNavigation: Boolean
): Boolean {
    val sourceRouteBase = sourceRoute?.substringBefore("?") ?: return false
    return !useSideNavigation && sourceRouteBase in visibleBottomBarRoutes
}

internal fun shouldClearReturningStateWhenDisposingVideoDestination(
    stillInVideoRoute: Boolean
): Boolean {
    return stillInVideoRoute
}

internal fun isVideoCardReturnTargetRoute(route: String?): Boolean {
    val routeBase = route?.substringBefore("?") ?: return false
    return routeBase == "main_host" ||
        routeBase == ScreenRoutes.Home.route ||
        routeBase == ScreenRoutes.History.route ||
        routeBase == ScreenRoutes.Favorite.route ||
        routeBase == ScreenRoutes.WatchLater.route ||
        routeBase == ScreenRoutes.Search.route ||
        routeBase == ScreenRoutes.Dynamic.route ||
        routeBase.startsWith("dynamic_detail/") ||
        routeBase == ScreenRoutes.Partition.route ||
        routeBase.startsWith("category/") ||
        routeBase.startsWith("season_series_detail/") ||
        routeBase.startsWith("space/")
}

internal fun isVideoDetailRoute(route: String?): Boolean {
    return route?.startsWith("${VideoRoute.base}/") == true
}

internal fun shouldEnableVideoDetailSharedTransition(
    cardTransitionEnabled: Boolean
): Boolean {
    return cardTransitionEnabled
}

internal fun shouldShareAudioModeViewModelWithPreviousEntry(
    previousRoute: String?,
    previousLifecycleState: Lifecycle.State?
): Boolean {
    return previousLifecycleState?.isAtLeast(Lifecycle.State.CREATED) == true &&
        isVideoDetailRoute(previousRoute)
}

internal fun shouldNavigateAudioModeBackToCurrentVideo(
    previousVideoBvid: String?,
    currentVideoBvid: String
): Boolean {
    val normalizedCurrentBvid = currentVideoBvid.trim()
    if (normalizedCurrentBvid.isEmpty()) return false
    return previousVideoBvid?.trim() != normalizedCurrentBvid
}

internal data class AudioModeInitialLoadRequest(
    val bvid: String,
    val cid: Long,
    val resumePositionMs: Long
)

internal fun resolveAudioModeInitialLoadRequest(
    key: BiliPaiNavKey.AudioMode,
    hasDisplayState: Boolean
): AudioModeInitialLoadRequest? {
    if (hasDisplayState) return null
    val sourceBvid = key.sourceBvid.trim()
    if (sourceBvid.isEmpty()) return null
    return AudioModeInitialLoadRequest(
        bvid = sourceBvid,
        cid = key.sourceCid.coerceAtLeast(0L),
        resumePositionMs = key.sourceResumePositionMs.coerceAtLeast(0L)
    )
}
