package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LiquidGlassRuntimeGuardStructureTest {

    @Test
    fun `shared liquid glass leaves consume the runtime visual guard`() {
        val floatingChrome = source("FloatingDockChrome.kt")
        val floatingBar = source("FloatingBottomBar.kt")
        val progressiveTop = source("ProgressiveTopChrome.kt")
        val bottomBar = source("BottomBar.kt")
        val homeHeader = source("HomeHeader.kt")
        val videoCard = source("cards/VideoCard.kt")

        assertTrue(floatingChrome.contains("isLowBlurBudgetForced()"))
        assertTrue(floatingBar.contains("PlainMiuixFloatingBottomBar("))
        assertTrue(floatingBar.contains("isLowBlurBudgetForced()"))
        assertTrue(progressiveTop.contains("isLowBlurBudgetForced()"))
        assertTrue(bottomBar.contains("isLowBlurBudgetForced(forceLowBlurBudget)"))
        assertTrue(homeHeader.contains("!isLowBlurBudgetForced(forceLowBlurBudget)"))
        assertTrue(videoCard.contains("!isLowBlurBudgetForced()"))
    }

    private fun source(name: String): String {
        val root = listOf(File("."), File("..")).first { File(it, "app/src/main").exists() }
        return File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/home/components/$name",
        ).readText()
    }
}
