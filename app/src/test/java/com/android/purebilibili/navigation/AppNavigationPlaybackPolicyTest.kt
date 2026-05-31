package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationPlaybackPolicyTest {

    @Test
    fun leavingVideoToHome_shouldStopPlaybackEagerly() {
        assertTrue(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToAudioMode_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.AudioMode.route
            )
        )
    }

    @Test
    fun switchingBetweenVideoRoutes_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun leavingVideoWithUnknownTargetRoute_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = null
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransition_shouldDeferBottomBarReveal() {
        assertTrue(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToMainHostHomeTabWithCardTransition_shouldDeferBottomBarReveal() {
        assertTrue(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToMainHostNonHomeTab_shouldDeferBottomBarReveal() {
        assertTrue(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Dynamic.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToNonHomeCardTarget_shouldAutoReleaseBottomBarReveal() {
        assertTrue(
            shouldAutoReleaseBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Dynamic.route
            )
        )
        assertFalse(
            shouldAutoReleaseBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransitionDisabled_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun notReturningFromDetail_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = false,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningButStillOnNonHomeRoute_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = VideoRoute.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun videoReturnBottomBarRestoreDelay_isImmediateForAppNavigationRelease() {
        assertEquals(
            0L,
            resolveVideoReturnBottomBarRestoreDelayMs(
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            0L,
            resolveVideoReturnBottomBarRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
        assertEquals(
            0L,
            resolveVideoReturnBottomBarRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun videoReturnBottomBarHideSuppression_matchesHomeNavigationResetTiming() {
        assertEquals(
            80L,
            resolveVideoReturnBottomBarHideSuppressionMs(cardTransitionEnabled = false)
        )
        assertEquals(
            200L,
            resolveVideoReturnBottomBarHideSuppressionMs(cardTransitionEnabled = true)
        )
    }

    @Test
    fun bottomBarHideSuppression_runsOnlyWhenDeferredRevealFinishesOnDestination() {
        assertTrue(
            shouldSuppressBottomBarHideAfterDeferredReveal(
                hadDeferredReveal = true,
                isBottomBarDestination = true,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldSuppressBottomBarHideAfterDeferredReveal(
                hadDeferredReveal = false,
                isBottomBarDestination = true,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldSuppressBottomBarHideAfterDeferredReveal(
                hadDeferredReveal = true,
                isBottomBarDestination = false,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldSuppressBottomBarHideAfterDeferredReveal(
                hadDeferredReveal = true,
                isBottomBarDestination = true,
                shouldDeferReveal = true
            )
        )
    }

    @Test
    fun bottomBarPrimesHiddenBeforeVideoNavigationFromVisibleBottomTab() {
        val visibleRoutes = setOf(
            ScreenRoutes.Home.route,
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route
        )

        assertTrue(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Dynamic.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertTrue(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = "${ScreenRoutes.Home.route}?from=feed",
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertFalse(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Search.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertFalse(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Dynamic.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = true
            )
        )
    }
}
