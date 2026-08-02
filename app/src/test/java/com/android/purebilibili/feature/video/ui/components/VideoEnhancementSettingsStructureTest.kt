package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoEnhancementSettingsStructureTest {

    @Test
    fun `竖屏设置提供算法模型与FSR锐化细节`() {
        val panelSource = source("VideoSettingsPanel.kt")
        val sharedSource = source("Anime4KSettingsUi.kt")

        assertTrue(panelSource.contains("VideoEnhancementAlgorithmOptions("))
        assertTrue(panelSource.contains("onAlgorithmChange = onVideoEnhancementAlgorithmChange"))
        assertTrue(panelSource.contains("Anime4KPresetOptions("))
        assertTrue(panelSource.contains("FsrSharpnessOptions("))
        assertTrue(panelSource.contains("onSharpnessChange = onFsrSharpnessChange"))
        assertTrue(sharedSource.contains("VideoEnhancementAlgorithm.entries.forEach"))
        assertTrue(sharedSource.contains("text = \"FSR 锐化\""))
        assertTrue(sharedSource.contains("onValueChange = onSharpnessChange"))
    }

    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/ui/components/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/video/ui/components/$fileName")
    ).first { it.exists() }.readText()
}
