package com.android.purebilibili.feature.audio.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTopControlDragPolicyTest {
    @Test
    fun idleControlKeepsIdentityTransform() {
        val transform = resolveMusicTopControlTransform(0f, 0f, maxDragPx = 18f)

        assertEquals(1f, transform.scaleX, 0.001f)
        assertEquals(1f, transform.scaleY, 0.001f)
    }

    @Test
    fun horizontalDragStretchesInPlaceAndClampsDeformation() {
        val transform = resolveMusicTopControlTransform(40f, 0f, maxDragPx = 18f)

        assertEquals(1.18f, transform.scaleX, 0.001f)
        assertEquals(0.94f, transform.scaleY, 0.001f)
    }

    @Test
    fun verticalDragStretchesInPlace() {
        val transform = resolveMusicTopControlTransform(0f, -18f, maxDragPx = 18f)

        assertEquals(0.94f, transform.scaleX, 0.001f)
        assertEquals(1.18f, transform.scaleY, 0.001f)
    }
}
