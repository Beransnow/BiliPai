package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailReturnSourceCardChromeTest {
    @Test
    fun nonWidescreenGridCardUsesItsMeasuredCoverBottom() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(left = 20f, top = 100f, right = 520f, bottom = 700f),
            // 4:3 cover: the anchor must come from this measured bottom, not 16:9 math.
            sourceCoverBounds = Rect(left = 20f, top = 100f, right = 520f, bottom = 475f),
        )

        assertTrue(layout.canRender)
        assertEquals(0.5f, layout.sourceScale, 0.0001f)
        assertEquals(500f, layout.sourceWidthPx, 0.0001f)
        assertEquals(225f, layout.sourceInfoHeightPx, 0.0001f)
        assertEquals(750f, layout.anchorYInViewportPx, 0.0001f)
    }

    @Test
    fun coverOnlyOrHorizontalLandingDoesNotInventABelowCoverRegion() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(left = 50f, top = 200f, right = 950f, bottom = 380f),
            sourceCoverBounds = Rect(left = 50f, top = 200f, right = 950f, bottom = 380f),
        )

        assertFalse(layout.canRender)
        assertEquals(0f, layout.sourceInfoHeightPx, 0.0001f)
    }

    @Test
    fun sideBySideCoverDoesNotPretendThatTextLivesBelowIt() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(left = 20f, top = 100f, right = 980f, bottom = 420f),
            sourceCoverBounds = Rect(left = 20f, top = 100f, right = 380f, bottom = 420f),
        )

        assertFalse(layout.canRender)
    }
}
