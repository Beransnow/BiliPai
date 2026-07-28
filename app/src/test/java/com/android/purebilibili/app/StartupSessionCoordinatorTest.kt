package com.android.purebilibili.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupSessionCoordinatorTest {

    @Test
    fun mirrorHitDecidesImmediatelyAndAuthoritativeValueRepairsOnlyNextLaunch() = runTest {
        val authoritative = CompletableDeferred<Boolean>()
        var repairedMirror: Boolean? = null
        val coordinator = StartupSessionCoordinator(
            sessionStartedAtMs = 10L,
            readMirror = { true },
            readAuthoritative = { authoritative.await() },
            writeMirror = { repairedMirror = it },
        )

        coordinator.start(this)
        runCurrent()
        val decision = coordinator.freezeDecision(onboardingRequired = false, timeoutMs = 1_000L)

        assertTrue(decision.openPortraitFeedOnStartup)
        assertFalse(decision.usedTimeoutFallback)

        authoritative.complete(false)
        advanceUntilIdle()
        assertEquals(false, repairedMirror)
        assertEquals(decision, coordinator.currentDecisionOrNull())
    }

    @Test
    fun mirrorMissTimesOutToHomeAndLateValueCannotRedirectCurrentLaunch() = runTest {
        val authoritative = CompletableDeferred<Boolean>()
        var repairedMirror: Boolean? = null
        val coordinator = StartupSessionCoordinator(
            sessionStartedAtMs = 0L,
            readMirror = { null },
            readAuthoritative = { authoritative.await() },
            writeMirror = { repairedMirror = it },
        )

        coordinator.start(this)
        runCurrent()
        val decision = coordinator.freezeDecision(onboardingRequired = false, timeoutMs = 100L)

        assertFalse(decision.openPortraitFeedOnStartup)
        assertTrue(decision.usedTimeoutFallback)

        authoritative.complete(true)
        advanceUntilIdle()
        assertEquals(true, repairedMirror)
        assertEquals(decision, coordinator.currentDecisionOrNull())
    }

    @Test
    fun onboardingDecisionNeverWaitsForPortraitPreference() = runTest {
        val coordinator = StartupSessionCoordinator(
            sessionStartedAtMs = 0L,
            readMirror = { null },
            readAuthoritative = { CompletableDeferred<Boolean>().await() },
            writeMirror = {},
        )

        val decision = coordinator.freezeDecision(onboardingRequired = true, timeoutMs = 1_000L)

        assertTrue(decision.onboardingRequired)
        assertFalse(decision.openPortraitFeedOnStartup)
        assertFalse(decision.usedTimeoutFallback)
    }

    @Test
    fun exhaustedDeadlineFallsBackWithoutWaitingAnExtraFrame() = runTest {
        val authoritative = CompletableDeferred<Boolean>()
        val coordinator = StartupSessionCoordinator(
            sessionStartedAtMs = 0L,
            readMirror = { null },
            readAuthoritative = { authoritative.await() },
            writeMirror = {},
        )

        coordinator.start(this)
        runCurrent()
        val decision = coordinator.freezeDecision(onboardingRequired = false, timeoutMs = 0L)

        assertFalse(decision.openPortraitFeedOnStartup)
        assertTrue(decision.usedTimeoutFallback)
        authoritative.complete(true)
        advanceUntilIdle()
    }

    @Test
    fun remainingDeadlineUsesOneTotalLaunchBudget() {
        val coordinator = StartupSessionCoordinator(
            sessionStartedAtMs = 1_000L,
            readMirror = { false },
            readAuthoritative = { false },
            writeMirror = {},
        )

        assertEquals(750L, coordinator.remainingDecisionTimeMs(nowMs = 1_250L, totalDeadlineMs = 1_000L))
        assertEquals(0L, coordinator.remainingDecisionTimeMs(nowMs = 2_500L, totalDeadlineMs = 1_000L))
    }
}
