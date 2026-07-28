package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuPlaybackClockTest {
    @Test
    fun `seek uses interval window and restores overlapping items`() {
        val index = DanmakuTimelineIndex(
            items = listOf(
                TestItem("early", 100L, 50L),
                TestItem("long", 200L, 1_000L),
                TestItem("late", 900L, 300L),
            ),
            startTimeMs = TestItem::start,
            durationMs = TestItem::duration,
        )

        index.moveTo(positionMs = 950L, forceSeek = true)

        assertEquals(listOf("long", "late"), index.activeItems.map(TestItem::id))
        assertEquals(1_201L, index.nextBoundaryAfter(950L))
    }

    @Test
    fun `normal playback crosses only start and inclusive end boundaries`() {
        val index = DanmakuTimelineIndex(
            items = listOf(TestItem("item", 1_000L, 500L)),
            startTimeMs = TestItem::start,
            durationMs = TestItem::duration,
        )

        assertFalse(index.moveTo(999L))
        assertEquals(1_000L, index.nextBoundaryAfter(999L))
        assertTrue(index.moveTo(1_000L))
        assertEquals(listOf("item"), index.activeItems.map(TestItem::id))
        assertEquals(1_501L, index.nextBoundaryAfter(1_000L))
        assertFalse(index.moveTo(1_500L))
        assertTrue(index.moveTo(1_501L))
        assertTrue(index.activeItems.isEmpty())
    }

    @Test
    fun `clock updates commands only at boundaries and detects advanced motion`() {
        val moving = advanced(id = "moving", start = 1_000L, duration = 500L, endX = 0.8f)
        val fixed = advanced(id = "fixed", start = 2_000L, duration = 500L, endX = 0.1f)
        val command = CommandDanmakuItem(
            id = "command",
            type = CommandDanmakuType.TEXT,
            content = "hello",
            startTimeMs = 1_200L,
            durationMs = 200L,
        )
        val clock = DanmakuPlaybackClock(
            advancedItems = listOf(moving, fixed),
            commandItems = listOf(command),
        )

        clock.seekTo(1_000L)
        assertEquals(listOf("moving"), clock.activeAdvancedItems.map(AdvancedDanmakuData::id))
        assertTrue(clock.hasContinuousAdvancedMotion)
        assertTrue(clock.activeCommandItems.isEmpty())

        clock.advanceTo(1_200L)
        assertEquals(listOf("command"), clock.activeCommandItems.map(CommandDanmakuItem::id))
        clock.advanceTo(1_400L)
        assertEquals(listOf("command"), clock.activeCommandItems.map(CommandDanmakuItem::id))
        clock.advanceTo(1_401L)
        assertTrue(clock.activeCommandItems.isEmpty())

        clock.seekTo(2_100L)
        assertEquals(listOf("fixed"), clock.activeAdvancedItems.map(AdvancedDanmakuData::id))
        assertFalse(clock.hasContinuousAdvancedMotion)
    }

    private fun advanced(
        id: String,
        start: Long,
        duration: Long,
        endX: Float,
    ) = AdvancedDanmakuData(
        id = id,
        content = id,
        startTimeMs = start,
        durationMs = duration,
        startX = 0.1f,
        startY = 0.1f,
        endX = endX,
        endY = 0.1f,
    )

    private data class TestItem(
        val id: String,
        val start: Long,
        val duration: Long,
    )
}
