package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.parseSplLyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerVisualPolicyTest {

    @Test
    fun `liquid controls require supported foreground renderer`() {
        assertFalse(resolveMusicLiquidGlassEnabled(32, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(33, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(36, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, true, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, false, true))
        assertFalse(resolveMusicLiquidGlassEnabled(35, false, false, false))
    }

    @Test
    fun `current lyric line follows offset adjusted playback time`() {
        val document = parseSplLyrics(
            """
            [00:01.00]One
            [00:03.00]Two
            [00:05.00]Three
            """.trimIndent()
        ).withOffset(500L)

        assertEquals(-1, resolveCurrentLyricIndex(document, positionMs = 1_000L))
        assertEquals(0, resolveCurrentLyricIndex(document, positionMs = 1_600L))
        assertEquals(1, resolveCurrentLyricIndex(document, positionMs = 3_500L))
        assertEquals(2, resolveCurrentLyricIndex(document, positionMs = 8_000L))
    }

    @Test
    fun `lyric focus keeps current line sharp and progressively blurs distant lines`() {
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 100),
            resolveMusicLyricFocusStyle(lineIndex = 4, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 2, alphaPercent = 46),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 7, alphaPercent = 20),
            resolveMusicLyricFocusStyle(lineIndex = 8, currentIndex = 4, blurEnabled = true)
        )
    }

    @Test
    fun `lyric blur falls back to opacity when renderer or motion policy disables it`() {
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 30, effectsEnabled = true, reduceMotion = false))
        assertTrue(resolveMusicLyricsBlurEnabled(sdkInt = 31, effectsEnabled = true, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = false, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = true, reduceMotion = true))
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 46),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = false)
        )
    }
}
