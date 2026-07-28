package com.android.purebilibili.feature.video.danmaku

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A time-sorted interval index used by the Compose danmaku overlays.
 *
 * Normal playback advances from the previous cursor and only visits crossed boundaries. A seek
 * rebuilds the active window from binary-searched bounds, so it does not scan the whole timeline.
 */
internal class DanmakuTimelineIndex<T>(
    items: List<T>,
    startTimeMs: (T) -> Long,
    durationMs: (T) -> Long,
    startPaddingMs: Long = 0L,
    endPaddingMs: Long = 0L,
) {
    private data class Window<T>(
        val sourceOrder: Int,
        val item: T,
        val startMs: Long,
        val endMs: Long,
    )

    private val windows = items.mapIndexed { index, item ->
        val rawStart = startTimeMs(item)
        val duration = durationMs(item).coerceAtLeast(0L)
        Window(
            sourceOrder = index,
            item = item,
            startMs = subtractSaturated(rawStart, startPaddingMs.coerceAtLeast(0L)),
            endMs = addSaturated(
                addSaturated(rawStart, duration),
                endPaddingMs.coerceAtLeast(0L),
            ),
        )
    }.sortedWith(compareBy<Window<T>> { it.startMs }.thenBy { it.sourceOrder })

    private val maximumWindowMs = windows.maxOfOrNull { window ->
        (window.endMs - window.startMs).coerceAtLeast(0L)
    } ?: 0L

    private val activeWindows = LinkedHashMap<Int, Window<T>>()
    private var nextStartIndex = 0
    private var nextRemovalBoundaryMs: Long? = null
    private var lastPositionMs = Long.MIN_VALUE

    var activeItems: List<T> = emptyList()
        private set

    fun moveTo(positionMs: Long, forceSeek: Boolean = false): Boolean {
        if (forceSeek || lastPositionMs == Long.MIN_VALUE || positionMs < lastPositionMs) {
            return rebuildAt(positionMs)
        }

        var changed = false
        while (nextStartIndex < windows.size && windows[nextStartIndex].startMs <= positionMs) {
            val window = windows[nextStartIndex++]
            if (window.endMs >= positionMs) {
                activeWindows[window.sourceOrder] = window
                changed = true
            }
        }

        if (nextRemovalBoundaryMs?.let { it <= positionMs } == true) {
            val iterator = activeWindows.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().value.endMs < positionMs) {
                    iterator.remove()
                    changed = true
                }
            }
        }
        lastPositionMs = positionMs
        if (changed) publishActiveItems()
        return changed
    }

    fun nextBoundaryAfter(positionMs: Long): Long? {
        val nextStart = windows.getOrNull(upperBoundStart(positionMs))?.startMs
        val nextEnd = nextRemovalBoundaryMs?.takeIf { it > positionMs }
        return listOfNotNull(nextStart, nextEnd).minOrNull()
    }

    private fun rebuildAt(positionMs: Long): Boolean {
        val previousOrders = activeWindows.keys.toList()
        activeWindows.clear()

        val earliestPossibleStart = subtractSaturated(positionMs, maximumWindowMs)
        val fromIndex = lowerBoundStart(earliestPossibleStart)
        nextStartIndex = upperBoundStart(positionMs)
        for (index in fromIndex until nextStartIndex) {
            val window = windows[index]
            if (window.endMs >= positionMs) {
                activeWindows[window.sourceOrder] = window
            }
        }
        lastPositionMs = positionMs
        val changed = previousOrders != activeWindows.keys.toList()
        if (changed || activeItems.isEmpty() != activeWindows.isEmpty()) publishActiveItems()
        return changed
    }

    private fun publishActiveItems() {
        activeItems = activeWindows.values
            .sortedBy { it.sourceOrder }
            .map { it.item }
        nextRemovalBoundaryMs = activeWindows.values.minOfOrNull { window ->
            if (window.endMs == Long.MAX_VALUE) Long.MAX_VALUE else window.endMs + 1L
        }
    }

    private fun lowerBoundStart(value: Long): Int {
        var low = 0
        var high = windows.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (windows[middle].startMs < value) low = middle + 1 else high = middle
        }
        return low
    }

    private fun upperBoundStart(value: Long): Int {
        var low = 0
        var high = windows.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (windows[middle].startMs <= value) low = middle + 1 else high = middle
        }
        return low
    }

    private companion object {
        fun addSaturated(value: Long, increment: Long): Long =
            if (increment > 0L && value > Long.MAX_VALUE - increment) Long.MAX_VALUE
            else value + increment

        fun subtractSaturated(value: Long, decrement: Long): Long =
            if (decrement > 0L && value < Long.MIN_VALUE + decrement) Long.MIN_VALUE
            else value - decrement
    }
}

/** Shared playback clock for advanced and command danmaku overlays. */
@Stable
internal class DanmakuPlaybackClock(
    advancedItems: List<AdvancedDanmakuData>,
    commandItems: List<CommandDanmakuItem>,
) {
    private val advancedIndex = DanmakuTimelineIndex(
        items = advancedItems,
        startTimeMs = AdvancedDanmakuData::startTimeMs,
        durationMs = AdvancedDanmakuData::durationMs,
        // Preserve the existing anti-flicker window exactly.
        startPaddingMs = ADVANCED_BOUNDARY_PADDING_MS,
        endPaddingMs = ADVANCED_BOUNDARY_PADDING_MS,
    )
    private val commandIndex = DanmakuTimelineIndex(
        items = commandItems,
        startTimeMs = CommandDanmakuItem::startTimeMs,
        durationMs = CommandDanmakuItem::durationMs,
    )

    var positionMs by mutableLongStateOf(0L)
        private set

    var activeAdvancedItems by mutableStateOf<List<AdvancedDanmakuData>>(emptyList())
        private set

    var activeCommandItems by mutableStateOf<List<CommandDanmakuItem>>(emptyList())
        private set

    var hasContinuousAdvancedMotion by mutableStateOf(false)
        private set

    fun seekTo(positionMs: Long) {
        synchronize(positionMs = positionMs, forceSeek = true)
    }

    fun advanceTo(positionMs: Long) {
        synchronize(positionMs = positionMs, forceSeek = false)
    }

    fun nextBoundaryAfter(positionMs: Long = this.positionMs): Long? =
        listOfNotNull(
            advancedIndex.nextBoundaryAfter(positionMs),
            commandIndex.nextBoundaryAfter(positionMs),
        ).minOrNull()

    private fun synchronize(positionMs: Long, forceSeek: Boolean) {
        this.positionMs = positionMs
        if (advancedIndex.moveTo(positionMs, forceSeek)) {
            activeAdvancedItems = advancedIndex.activeItems
        }
        hasContinuousAdvancedMotion = activeAdvancedItems.any { item ->
            hasContinuousMotion(item = item, positionMs = positionMs)
        }
        if (commandIndex.moveTo(positionMs, forceSeek)) {
            activeCommandItems = commandIndex.activeItems
        }
    }

    private fun hasContinuousMotion(item: AdvancedDanmakuData, positionMs: Long): Boolean =
        item.startX != item.endX ||
            item.startY != item.endY ||
            (item.maxCount > 1 && positionMs < item.startTimeMs + item.accumulationDurationMs)

    private companion object {
        const val ADVANCED_BOUNDARY_PADDING_MS = 100L
    }
}
