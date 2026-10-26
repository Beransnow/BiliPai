package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerPlaybackCenterTest {
    @Test
    fun fullscreenCompensatesViewportLiftAndAsymmetricCutoutInsteadOfMovingTheVideo() {
        assertEquals(
            IntOffset(20, 48),
            resolvePlayerPlaybackCenterOffset(true, IntRect(0, -96, 1040, 2400), IntSize(1080, 2400)),
        )
        assertEquals(
            IntOffset(-40, 0),
            resolvePlayerPlaybackCenterOffset(true, IntRect(80, 0, 2400, 1080), IntSize(2400, 1080)),
        )
    }

    @Test
    fun alreadyFullscreenBoundsAndUnknownBoundsDoNotReceiveWindowCompensation() {
        assertEquals(IntOffset.Zero,
            resolvePlayerPlaybackCenterOffset(true, IntRect(0, 0, 1080, 2400), IntSize(1080, 2400)))
        assertEquals(IntOffset.Zero, resolvePlayerPlaybackCenterOffset(true, null, IntSize(1080, 2400)))
    }

    @Test
    fun inlineCenterMatchesTheActualVideoBelowItsInsetInPortraitAndLandscape() {
        for ((host, window) in listOf(
            IntRect(0, 96, 1080, 996) to IntSize(1080, 2400),
            IntRect(80, 48, 2320, 828) to IntSize(2400, 1080),
        )) {
            for (topInset in listOf(0, 72, 120)) {
                val offset = resolvePlayerPlaybackCenterOffset(false, host, window, topInset)
                val visibleVideoCenter = IntOffset(
                    (host.left + host.right) / 2,
                    (host.top + topInset + host.bottom) / 2,
                )
                val controlCenter = IntOffset(
                    (host.left + host.right) / 2 + offset.x,
                    (host.top + host.bottom) / 2 + offset.y,
                )
                assertEquals(visibleVideoCenter, controlCenter)
            }
        }
    }

    @Test
    fun fullscreenIgnoresInlineStripAndStillTargetsThePhysicalWindow() {
        val host = IntRect(0, -96, 1040, 2400)
        val window = IntSize(1080, 2400)
        val offset = resolvePlayerPlaybackCenterOffset(true, host, window, inlineVideoTopInsetPx = 120)
        assertEquals(
            IntOffset(window.width / 2, window.height / 2),
            IntOffset((host.left + host.right) / 2, (host.top + host.bottom) / 2) + offset,
        )
    }
}
