package com.android.purebilibili.feature.onboarding

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingMotionPolicyTest {

    @Test
    fun pageCountKeepsSettingsGuideAsFinalPage() {
        assertEquals(5, resolveOnboardingPageCount())
        assertEquals(4, resolveOnboardingLastPageIndex())
    }

    @Test
    fun normalMotionUsesLayeredPageAndHeroAnimation() {
        val spec = resolveOnboardingMotionSpec(reduceMotion = false)

        assertTrue(spec.pager.minScale < 1f)
        assertTrue(spec.pager.minAlpha < 1f)
        assertTrue(spec.floating.translationYPx > 0f)
        assertTrue(spec.halo.maxScale > spec.halo.minScale)
        assertTrue(spec.card.selectedScale > spec.card.unselectedScale)
    }

    @Test
    fun reduceMotionDisablesLoopingMotion() {
        val spec = resolveOnboardingMotionSpec(reduceMotion = true)

        assertEquals(1f, spec.pager.minScale)
        assertEquals(1f, spec.pager.minAlpha)
        assertEquals(0f, spec.floating.translationYPx)
        assertEquals(0, spec.floating.durationMillis)
        assertEquals(1f, spec.halo.minScale)
        assertEquals(1f, spec.halo.maxScale)
        assertEquals(1f, spec.card.selectedScale)
    }

    @Test
    fun pagerOffsetIsReadInsideThePageLayer() {
        val path = "app/src/main/java/com/android/purebilibili/feature/onboarding/OnboardingScreen.kt"
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        val source = sourceFile.readText()

        assertTrue(source.contains("pageOffsetProvider = remember(pagerState, page)"))
        assertTrue(source.contains("pageOffsetProvider().absoluteValue"))
        assertFalse(source.contains("pageOffset = pageOffset,"))
    }
}
