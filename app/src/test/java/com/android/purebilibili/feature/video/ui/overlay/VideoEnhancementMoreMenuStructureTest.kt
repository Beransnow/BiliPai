package com.android.purebilibili.feature.video.ui.overlay

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoEnhancementMoreMenuStructureTest {

    @Test
    fun `一级更多菜单只保留紧凑画质增强入口`() {
        val source = bottomControlBarSource()

        assertTrue(source.contains("label = \"画质增强\""))
        assertTrue(source.contains("showVideoEnhancementPanel = true"))
        assertTrue(source.contains("highlighted = anime4kEnabled"))
        assertFalse(source.contains("Anime4KMoreAction("))
    }

    @Test
    fun `二级菜单承载当前视频开关和算法模型选择`() {
        val source = bottomControlBarSource()

        assertTrue(source.contains("VideoEnhancementSettingsPanel("))
        assertTrue(source.contains("onCheckedChange = onAnime4kToggle"))
        assertTrue(source.contains("onAlgorithmChange = onVideoEnhancementAlgorithmChange"))
        assertTrue(source.contains("VideoEnhancementAlgorithm.entries.forEach"))
        assertTrue(source.contains("Anime4K 模型"))
    }

    private fun bottomControlBarSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt"),
        File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt")
    ).first { it.exists() }.readText()
}
