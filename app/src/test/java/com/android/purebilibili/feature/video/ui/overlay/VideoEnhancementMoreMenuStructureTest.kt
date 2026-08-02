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
        assertTrue(source.contains("算法与模型会沿用上次选择"))
    }

    @Test
    fun `二级菜单避让系统栏并在低高度横屏可滚动`() {
        val source = bottomControlBarSource()

        assertTrue(source.contains("windowInsetsPadding(WindowInsets.safeDrawing)"))
        assertTrue(source.contains("maxHeightDp = videoEnhancementPanelMaxHeightDp"))
        assertTrue(source.contains(".heightIn(max = maxHeightDp.dp)"))
        assertTrue(source.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("VideoEnhancementChoice("))
    }

    private fun bottomControlBarSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt"),
        File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt")
    ).first { it.exists() }.readText()
}
