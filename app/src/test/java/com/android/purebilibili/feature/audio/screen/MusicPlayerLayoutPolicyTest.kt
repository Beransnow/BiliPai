package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicPlayerLayoutPolicyTest {

    @Test
    fun `compact portrait uses horizontal pager`() {
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(widthDp = 393, isInPipMode = false)
        )
    }

    @Test
    fun `wide screen keeps artwork and lyrics visible together`() {
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = false)
        )
    }

    @Test
    fun `pip always renders artwork only`() {
        assertEquals(
            MusicPlayerLayout.PIP_ARTWORK,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = true)
        )
    }

    @Test
    fun `compact pager tabs stay on cover and lyrics`() {
        assertEquals(listOf("封面", "歌词"), resolveMusicPlayerPageTabs())
        assertEquals(70, MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP)
    }

    @Test
    fun `compact artwork respects available height`() {
        assertEquals(
            320,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 720,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
        assertEquals(
            240,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 320,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
    }
}
