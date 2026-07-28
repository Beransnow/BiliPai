package com.android.purebilibili.core.ui

import com.android.purebilibili.core.ui.blur.BlurIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeConfigTest {

    @Test
    fun `defaults keep standalone UI hosts functional`() {
        val config = AppThemeConfig()

        assertEquals(BlurIntensity.THIN, config.blurIntensity)
        assertTrue(config.hapticFeedbackEnabled)
        assertTrue(config.uiEntranceAnimationEnabled)
        assertTrue(config.runtimeVisualGuardEnabled)
    }
}
