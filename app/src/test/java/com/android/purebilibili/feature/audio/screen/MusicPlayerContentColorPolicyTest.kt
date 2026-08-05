package com.android.purebilibili.feature.audio.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicPlayerContentColorPolicyTest {

    @Test
    fun lightPaletteUsesDarkContentForReadableButtons() {
        val content = resolveMusicPlayerContentColor(Color(0xFFF5F5F5))
        assertTrue(content.luminance() < 0.3f, "light bg needs dark text, was $content")
    }

    @Test
    fun darkPaletteKeepsWhiteContent() {
        val content = resolveMusicPlayerContentColor(Color(0xFF342B42))
        assertEquals(Color.White, content)
    }

    @Test
    fun borderlineLightBackgroundStillPrefersDarkText() {
        val content = resolveMusicPlayerContentColor(Color(0xFFBDBDBD))
        assertTrue(content.luminance() < 0.3f)
    }
}
