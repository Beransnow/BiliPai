package com.android.purebilibili.core.ui.performance

import androidx.metrics.performance.FrameData
import androidx.metrics.performance.StateInfo
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeVisualGuardTrackerTest {

    @Test
    fun sampleBelowMinimumFrameCount_isIgnored() {
        val tracker = RuntimeVisualGuardTracker()

        tracker.finishWindow(totalFrames = 11, jankyFrames = 11, nowMs = 100L)

        assertFalse(tracker.decision.value.downgraded)
    }

    @Test
    fun framesWithoutVideoTransitionState_areIgnored() {
        val tracker = RuntimeVisualGuardTracker()
        repeat(24) { index ->
            tracker.onFrame(
                frameData = FrameData(
                    frameStartNanos = index.toLong(),
                    frameDurationUiNanos = 20_000_000L,
                    isJank = true,
                    states = listOf(StateInfo("OtherTransition", "Running")),
                ),
                nowMs = index.toLong(),
            )
        }

        assertFalse(tracker.decision.value.downgraded)
    }

    @Test
    fun firstHighJankWindow_doesNotDowngrade() {
        val tracker = RuntimeVisualGuardTracker()

        tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 100L)

        assertEquals(MotionTier.Normal, tracker.decision.value.effectiveMotionTier)
    }

    @Test
    fun twoConsecutiveHighJankWindows_downgradeToReduced() {
        val tracker = RuntimeVisualGuardTracker()

        tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 100L)
        tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 200L)

        assertTrue(tracker.decision.value.downgraded)
        assertEquals(MotionTier.Reduced, tracker.decision.value.effectiveMotionTier)
    }

    @Test
    fun lowJankWindowDuringCooldown_keepsReducedTier() {
        val tracker = downgradedTracker(downgradedAtMs = 200L)

        tracker.finishWindow(
            totalFrames = 20,
            jankyFrames = 0,
            nowMs = 200L + RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS - 1L,
        )

        assertTrue(tracker.decision.value.downgraded)
    }

    @Test
    fun onlyLowJankWindowAfterCooldown_recoversNormalTier() {
        val tracker = downgradedTracker(downgradedAtMs = 200L)

        tracker.finishWindow(
            totalFrames = 20,
            jankyFrames = 1,
            nowMs = 200L + RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS,
        )
        assertTrue(tracker.decision.value.downgraded)

        tracker.finishWindow(
            totalFrames = 20,
            jankyFrames = 0,
            nowMs = 201L + RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS,
        )

        assertFalse(tracker.decision.value.downgraded)
        assertEquals(MotionTier.Normal, tracker.decision.value.effectiveMotionTier)
    }

    @Test
    fun discardedWindow_doesNotTriggerOrBreakConsecutiveHighCount() {
        val tracker = RuntimeVisualGuardTracker()
        tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 100L)
        repeat(20) {
            tracker.onFrame(tracked = true, isJank = true, nowMs = 150L)
        }

        tracker.discardActiveWindow()
        assertFalse(tracker.decision.value.downgraded)

        tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 200L)
        assertTrue(tracker.decision.value.downgraded)
    }

    @Test
    fun stateValueChange_closesPreviousWindowWithoutInactiveFrame() {
        val tracker = RuntimeVisualGuardTracker()

        repeat(12) { tracker.onFrame(stateValue = "Opening", isJank = true, nowMs = 100L) }
        repeat(12) { tracker.onFrame(stateValue = "Returning", isJank = true, nowMs = 200L) }
        tracker.onFrame(stateValue = null, isJank = false, nowMs = 300L)

        assertTrue(tracker.decision.value.downgraded)
    }

    private fun downgradedTracker(downgradedAtMs: Long): RuntimeVisualGuardTracker {
        return RuntimeVisualGuardTracker().also { tracker ->
            tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = 100L)
            tracker.finishWindow(totalFrames = 12, jankyFrames = 1, nowMs = downgradedAtMs)
        }
    }

    private fun RuntimeVisualGuardTracker.finishWindow(
        totalFrames: Int,
        jankyFrames: Int,
        nowMs: Long,
    ) {
        repeat(totalFrames) { index ->
            onFrame(
                tracked = true,
                isJank = index < jankyFrames,
                nowMs = nowMs,
            )
        }
        onFrame(tracked = false, isJank = false, nowMs = nowMs)
    }
}
