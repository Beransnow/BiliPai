package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.LyricDocument
import kotlin.math.abs

internal data class MusicLyricFocusStyle(
    val blurRadiusDp: Int,
    val alphaPercent: Int
)

internal fun resolveMusicLyricsBlurEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    reduceMotion: Boolean
): Boolean = sdkInt >= 31 && effectsEnabled && !reduceMotion

internal fun resolveMusicLyricFocusStyle(
    lineIndex: Int,
    currentIndex: Int,
    blurEnabled: Boolean
): MusicLyricFocusStyle {
    val distance = abs(lineIndex - currentIndex)
    val alphaPercent = when (distance) {
        0 -> 100
        1 -> 46
        2 -> 30
        else -> 20
    }
    val blurRadiusDp = if (!blurEnabled) {
        0
    } else {
        when (distance) {
            0 -> 0
            1 -> 2
            2 -> 4
            else -> 7
        }
    }
    return MusicLyricFocusStyle(blurRadiusDp, alphaPercent)
}

internal fun resolveMusicLiquidGlassEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    isAppInBackground: Boolean,
    reduceMotion: Boolean
): Boolean {
    return effectsEnabled &&
        sdkInt in 33..35 &&
        !isAppInBackground &&
        !reduceMotion
}

internal fun resolveCurrentLyricIndex(
    document: LyricDocument,
    positionMs: Long
): Int {
    if (document.lines.isEmpty()) return -1
    val adjustedPosition = positionMs - document.offsetMs
    var low = 0
    var high = document.lines.lastIndex
    var result = -1
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (document.lines[middle].startTimeMs <= adjustedPosition) {
            result = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return result
}
