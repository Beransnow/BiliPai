package com.android.purebilibili.feature.home

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionExposure
import com.android.purebilibili.core.ui.transition.videoCardTransitionBackgroundEffect
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class HomeCardTransitionDrawCacheUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun blurredBackgroundDoesNotDrawLiveHomeUntilTakeoverAndReusesSnapshotOnCancel() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val progress = mutableFloatStateOf(1f)
        val returning = mutableStateOf(false)
        val idle = mutableStateOf(false)
        val color = mutableStateOf(Color.Blue)
        val draws = AtomicInteger()
        rule.setContent {
            Box(Modifier.size(120.dp)
                .videoCardTransitionBackgroundEffect(
                    progressProvider = { progress.floatValue },
                    phaseProvider = {
                        when {
                            idle.value -> VideoCardTransitionBackgroundPhase.IDLE
                            returning.value -> VideoCardTransitionBackgroundPhase.RETURNING
                            else -> VideoCardTransitionBackgroundPhase.OPENING
                        }
                    },
                    exposureProvider = {
                        when {
                            idle.value -> VideoCardTransitionExposure.Idle
                            returning.value -> VideoCardTransitionExposure.BackPreview
                            else -> VideoCardTransitionExposure.Opening
                        }
                    },
                    realtimeBlurEnabledProvider = { true },
                )
                .drawWithContent {
                    draws.incrementAndGet()
                    drawRect(color.value)
                }
                .testTag("blurred-home"))
        }
        fun pixel(): Color {
            val pixels = rule.onNodeWithTag("blurred-home").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }
        assertTrue(pixel().blue > 0.8f)
        rule.runOnIdle { returning.value = true }
        pixel()
        val capturedDraws = draws.get()
        rule.runOnIdle { progress.floatValue = 0.32f }
        pixel()
        val primedDraws = draws.get()
        assertEquals("Prepare the glass source once ahead of takeover", capturedDraws + 1, primedDraws)
        for (depth in listOf(0.3f, 0.25f, 0.32f)) {
            rule.runOnIdle { progress.floatValue = depth }
            assertTrue(pixel().blue > 0.8f)
            assertEquals("Clearing frozen blur must not repeatedly redraw live home", primedDraws, draws.get())
        }
        // Real content changes cannot silently mutate the frozen recording.
        rule.runOnIdle { color.value = Color.Green; progress.floatValue = 0.1f }
        val live = pixel()
        assertTrue(live.green > 0.9f && live.blue < 0.05f)
        assertTrue(draws.get() > capturedDraws)
        val handoffDraws = draws.get()
        for (depth in listOf(0.25f, 0.32f, 0.4f, 1f)) {
            rule.runOnIdle { progress.floatValue = depth }
            assertTrue("Cancellation must restore the retained snapshot", pixel().blue > 0.8f)
            assertEquals("Reversing into frozen blur must stop live drawing", handoffDraws, draws.get())
        }
        rule.runOnIdle { idle.value = true; progress.floatValue = 0f }
        val restored = pixel()
        assertTrue("Idle restores current content", restored.green > 0.9f && restored.blue < 0.05f)
    }

    @Test
    fun scrimProgressReusesSourceDrawingAndContentChangesStillAppear() {
        val progress = mutableFloatStateOf(0f)
        val color = mutableStateOf(Color.Blue)
        val draws = AtomicInteger()
        rule.setContent {
            Box(Modifier.size(120.dp)
                .videoCardTransitionBackgroundEffect(
                    progressProvider = { progress.floatValue },
                    phaseProvider = { VideoCardTransitionBackgroundPhase.OPENING },
                    exposureProvider = { VideoCardTransitionExposure.Opening },
                    realtimeBlurEnabledProvider = { false },
                    clearSnapshotEnabledProvider = { false },
                )
                .drawWithContent {
                    draws.incrementAndGet()
                    drawRect(color.value)
                }
                .testTag("cached-home"))
        }
        fun pixel(): Color {
            val pixels = rule.onNodeWithTag("cached-home").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }
        val initialBlue = pixel().blue
        assertTrue(initialBlue > 0.9f)
        val initialDraws = draws.get()
        var darkestBlue = initialBlue
        for (depth in listOf(0.25f, 0.5f, 0.9f, 0.5f, 0f)) {
            rule.runOnIdle { progress.floatValue = depth }
            val currentBlue = pixel().blue
            assertTrue(currentBlue > 0.8f)
            darkestBlue = minOf(darkestBlue, currentBlue)
            assertEquals("Scrim progress must not redraw the source list", initialDraws, draws.get())
        }
        assertTrue("Scrim itself must still animate", darkestBlue < initialBlue - 0.02f)
        assertEquals("Reversed progress restores the source", initialBlue, pixel().blue, 0.02f)
        rule.runOnIdle { color.value = Color.Green }
        val updated = pixel()
        assertTrue(updated.green > 0.9f && updated.blue < 0.05f)
        assertTrue("Image/data updates must invalidate the live source", draws.get() > initialDraws)
    }
}
