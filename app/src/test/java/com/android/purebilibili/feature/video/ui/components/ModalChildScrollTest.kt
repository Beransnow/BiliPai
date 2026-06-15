package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ModalChildScrollTest {

    @Test
    fun `upward pointer movement scrolls modal content forward`() {
        assertEquals(24f, resolveModalChildScrollDelta(availableY = -24f))
    }

    @Test
    fun `downward pointer movement scrolls modal content backward`() {
        assertEquals(-18f, resolveModalChildScrollDelta(availableY = 18f))
    }
}
