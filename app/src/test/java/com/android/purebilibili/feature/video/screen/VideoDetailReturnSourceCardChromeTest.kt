package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailReturnSourceCardChromeTest {
    @Test
    fun gridCardResolvesARealInfoRegionBelowItsCover() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(left = 20f, top = 100f, right = 520f, bottom = 600f),
        )

        assertTrue(layout.canRender)
        assertEquals(0.5f, layout.sourceScale, 0.0001f)
        assertEquals(500f, layout.sourceWidthPx, 0.0001f)
        assertEquals(218.75f, layout.sourceInfoHeightPx, 0.0001f)
    }

    @Test
    fun coverOnlyOrHorizontalLandingDoesNotInventABelowCoverRegion() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(left = 50f, top = 200f, right = 950f, bottom = 380f),
        )

        assertFalse(layout.canRender)
        assertEquals(0f, layout.sourceInfoHeightPx, 0.0001f)
    }
}
