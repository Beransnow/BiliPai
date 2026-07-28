package com.android.purebilibili.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayerLeaseRegistryTest {
    @Test
    fun `old owner release becomes invalid after transfer`() {
        val state = PlayerLeaseGenerationState()
        val screenOwner = state.acquire("screen")

        val miniOwner = state.transfer(screenOwner, "mini")

        assertNotNull(miniOwner)
        assertFalse(state.release(screenOwner))
        assertTrue(state.release(miniOwner!!))
    }

    @Test
    fun `same generation releases exactly once`() {
        val state = PlayerLeaseGenerationState()
        val owner = state.acquire("external")

        assertTrue(state.release(owner))
        assertFalse(state.release(owner))
        assertThrows(IllegalStateException::class.java) {
            state.acquire("reacquired")
        }
    }

    @Test
    fun `stale token cannot transfer a newly acquired generation`() {
        val state = PlayerLeaseGenerationState()
        val first = state.acquire("first")
        val second = state.acquire("second")

        assertNull(state.transfer(first, "mini"))
        assertTrue(state.isCurrent(second))
    }

    @Test
    fun `release fence waits for committed pop and resumed top`() {
        assertFalse(
            isReleaseFenceSettled(
                capturedAtUptimeMs = 1_000L,
                nowUptimeMs = 1_100L,
                lastPopUptimeMs = 1_020L,
                lastTopResumeUptimeMs = 1_010L,
            )
        )
        assertTrue(
            isReleaseFenceSettled(
                capturedAtUptimeMs = 1_000L,
                nowUptimeMs = 1_100L,
                lastPopUptimeMs = 1_020L,
                lastTopResumeUptimeMs = 1_060L,
            )
        )
    }

    @Test
    fun `release fence timeout bounds non navigation disposal`() {
        assertTrue(
            isReleaseFenceSettled(
                capturedAtUptimeMs = 1_000L,
                nowUptimeMs = 2_000L,
                lastPopUptimeMs = Long.MIN_VALUE,
                lastTopResumeUptimeMs = Long.MIN_VALUE,
            )
        )
    }
}
