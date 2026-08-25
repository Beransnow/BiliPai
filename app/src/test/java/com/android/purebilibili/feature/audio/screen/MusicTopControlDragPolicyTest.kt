package com.android.purebilibili.feature.audio.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTopControlDragPolicyTest {
    @Test
    fun idleControlKeepsIdentityTransform() {
        val transform = resolveMusicTopControlTransform(0f, 0f, maxDragPx = 36f, glassProgress = 0f)

        assertEquals(1f, transform.scaleX, 0.001f)
        assertEquals(1f, transform.scaleY, 0.001f)
        assertEquals(0.5f, transform.pivotFractionX, 0.001f)
        assertEquals(0.5f, transform.pivotFractionY, 0.001f)
    }

    @Test
    fun horizontalDragStretchesInPlaceAndClampsDeformation() {
        val transform = resolveMusicTopControlTransform(72f, 0f, maxDragPx = 36f, glassProgress = 0f)

        assertEquals(1.24f, transform.scaleX, 0.001f)
        assertEquals(0.93f, transform.scaleY, 0.001f)
        assertEquals(0.3f, transform.pivotFractionX, 0.001f)
        assertEquals(0.5f, transform.pivotFractionY, 0.001f)
    }

    @Test
    fun verticalDragStretchesInPlace() {
        val transform = resolveMusicTopControlTransform(0f, -36f, maxDragPx = 36f, glassProgress = 0f)

        assertEquals(0.93f, transform.scaleX, 0.001f)
        assertEquals(1.24f, transform.scaleY, 0.001f)
        assertEquals(0.5f, transform.pivotFractionX, 0.001f)
        assertEquals(0.7f, transform.pivotFractionY, 0.001f)
    }

    @Test
    fun frostedPresetKeepsTheSameDirectionWithCalmerDeformation() {
        val transform = resolveMusicTopControlTransform(36f, 0f, maxDragPx = 36f, glassProgress = 1f)

        assertEquals(1.18f, transform.scaleX, 0.001f)
        assertEquals(0.95f, transform.scaleY, 0.001f)
        assertEquals(0.38f, transform.pivotFractionX, 0.001f)
    }
}
