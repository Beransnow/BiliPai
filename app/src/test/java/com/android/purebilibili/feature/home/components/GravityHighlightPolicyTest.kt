package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class GravityHighlightPolicyTest {

    @Test
    fun `nearby gravity samples collapse onto the same highlight direction`() {
        val base = quantizeGravityHighlightDirection(gravityX = 0.20f, gravityY = -0.98f)
        val nearby = quantizeGravityHighlightDirection(gravityX = 0.22f, gravityY = -0.97f)

        assertEquals(base.first, nearby.first, 0.0001f)
        assertEquals(base.second, nearby.second, 0.0001f)
    }

    @Test
    fun `missing gravity falls back to straight up`() {
        val direction = quantizeGravityHighlightDirection(gravityX = 0f, gravityY = 0f)

        assertEquals(0f, direction.first, 0.0001f)
        assertEquals(-1f, direction.second, 0.0001f)
    }
}
