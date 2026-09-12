package com.android.purebilibili.feature.home.components

import kotlin.math.roundToInt

/** Accumulate one direction before changing chrome; tiny reversals must not cause flicker. */
internal fun accumulateDockScroll(previous: Float, delta: Float): Float =
    if (previous * delta < 0f) delta else previous + delta

internal data class LinkedDockGeometry(
    val searchWidth: Int,
    val audioWidth: Int,
    val audioX: Int,
    val audioY: Int,
    val top: Int,
    val height: Int,
)

internal fun resolveLinkedDockGeometry(
    width: Int,
    button: Int,
    barHeight: Int,
    gap: Int,
    hasAudio: Boolean,
    searchEnabled: Boolean,
    mergeProgress: Float,
    searchProgress: Float,
): LinkedDockGeometry {
    val merge = mergeProgress.coerceIn(0f, 1f)
    val search = searchProgress.coerceIn(0f, 1f)
    val top = ((if (hasAudio) barHeight + gap else 0) * (1f - merge)).roundToInt()
    val searchWidth = if (!searchEnabled) 0 else (
        button + (width - button * (if (hasAudio) 3 else 2)) * search
    ).roundToInt().coerceAtLeast(button).coerceAtMost((width - button).coerceAtLeast(0))
    // Playback uses three separate capsules. Only active search joins the surfaces.
    val playbackGap = (gap * (1f - search)).roundToInt()
    val compactAudioWidth = (width - button - searchWidth -
        playbackGap * (if (searchEnabled) 2 else 1)).coerceAtLeast(0)
    return LinkedDockGeometry(
        searchWidth = searchWidth,
        audioWidth = if (hasAudio) (width + (compactAudioWidth - width) * merge).roundToInt() else 0,
        audioX = ((button + playbackGap) * merge).roundToInt(),
        audioY = (top * merge).roundToInt(),
        top = top,
        height = top + barHeight,
    )
}
