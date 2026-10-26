@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.video

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.store.PlayerProgressPlacement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real bundled media, actual rotations and native menus; no network or playback mocks. */
@RunWith(AndroidJUnit4::class)
class PlayerParityEmulatorTest {
    @get:Rule val rule = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<PlayerParityTestActivity>
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var originalEnhancement = "{}"
    private var originalSpeed = 1f
    private var originalProgressPlacement = PlayerProgressPlacement.ABOVE_CONTROLS

    @Before
    fun launchLocalPlayer() {
        PluginManager.initialize(context)
        runBlocking {
            originalEnhancement = PluginStore.getConfigJson(context, BuiltinPluginIds.VIDEO_ENHANCEMENT) ?: "{}"
            originalSpeed = SettingsManager.getLastPlaybackSpeed(context).first()
            originalProgressPlacement = SettingsManager.getPlayerProgressPlacement(context).first()
        }
        scenario = ActivityScenario.launch(Intent(context, PlayerParityTestActivity::class.java))
        awaitPlayer { it.player.playbackState == Player.STATE_READY && it.renderedFrames.get() > 0 && it.hasValidVideoSurface() }
    }

    @After
    fun releaseAndRestorePreferences() {
        if (::scenario.isInitialized) scenario.close()
        runBlocking {
            PluginStore.setConfigJson(context, BuiltinPluginIds.VIDEO_ENHANCEMENT, originalEnhancement)
            SettingsManager.setLastPlaybackSpeed(context, originalSpeed)
            SettingsManager.setPlayerProgressPlacement(context, originalProgressPlacement)
        }
    }

    @Test
    fun materialToggleKeepsPlaybackTargetAndDockGeometryAcrossActualRotations() {
        var original: Player? = null
        scenario.onActivity { original = it.player }
        for (style in AppUiStyle.entries) {
            for (landscape in listOf(false, true)) {
                scenario.onActivity { it.uiStyle = style; it.rotateToLandscape(landscape) }
                awaitPlayer { (it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) == landscape }
                val inlineRowHeights = mutableMapOf<Boolean, Float>()
                for (fullscreen in listOf(false, true)) {
                    var baseline: Map<String, Rect>? = null
                    for (glass in listOf(false, true)) {
                        scenario.onActivity {
                            it.fullscreenViewportLift = fullscreen
                            it.glass = glass
                            it.player.play()
                        }
                        awaitPlayer { it.player.isPlaying && it.hasValidVideoSurface() }
                        var needsFullscreenToggle = false
                        scenario.onActivity { needsFullscreenToggle = it.fullscreen != fullscreen }
                        if (needsFullscreenToggle) {
                            rule.onNodeWithTag("player_fullscreen_toggle").assertHasClickAction().performClick()
                            awaitPlayer { it.fullscreen == fullscreen }
                        }
                        val tags = listOf(
                            "player_play_pause", "player_progress", "player_control_row", "player_fullscreen_toggle",
                            "player_playback_dock", "player_tools_dock",
                        ) + if (!fullscreen) listOf(
                            "player_subtitle_action", "player_ratio_action", "player_portrait_fullscreen_action",
                        ) else emptyList()
                        val bounds = tags.associateWith { rule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
                        val viewport = rule.onNodeWithTag("parity_player_viewport").fetchSemanticsNode().boundsInRoot
                        bounds.forEach { (tag, rect) ->
                            assertTrue("$tag escapes player: $rect / $viewport", rect.left >= viewport.left - 1f && rect.right <= viewport.right + 1f && rect.top >= viewport.top - 1f && rect.bottom <= viewport.bottom + 1f)
                        }
                        assertPlaybackGlyphCentered(fullscreen)
                        val rowHeight = bounds.getValue("player_control_row").height
                        var expectedRowHeight = 0f
                        scenario.onActivity { expectedRowHeight = 40f * it.resources.displayMetrics.density }
                        assertEquals("Control row must use the shared 40dp slot", expectedRowHeight, rowHeight, 1f)
                        if (!fullscreen) {
                            val ratio = rule.onNodeWithTag("player_ratio_action").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                            val portrait = rule.onNodeWithTag("player_portrait_fullscreen_action").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                            assertTrue("Inline actions must stay compact without shrinking their targets",
                                portrait.left >= ratio.right - 1f &&
                                    portrait.left - ratio.right <= expectedRowHeight / 5f + 1f &&
                                    portrait.width >= expectedRowHeight - 1f)
                            val exit = bounds.getValue("player_fullscreen_toggle")
                            for (tag in listOf("player_subtitle_action", "player_ratio_action", "player_portrait_fullscreen_action", "player_fullscreen_toggle")) {
                                assertEquals("$tag must share Material's compact 40dp width",
                                    expectedRowHeight, bounds.getValue(tag).width, 1f)
                            }
                            assertEquals("Independent fullscreen action must use the same gap as adjacent icons",
                                portrait.left - ratio.right, exit.left - portrait.right, 1f)
                        }
                        if (!fullscreen) inlineRowHeights[glass] = rowHeight else {
                            assertEquals("Inline and fullscreen vertical metrics differ", inlineRowHeights.getValue(glass), rowHeight, 1f)
                            val playback = bounds.getValue("player_playback_dock")
                            val tools = bounds.getValue("player_tools_dock")
                            val exit = bounds.getValue("player_fullscreen_toggle")
                            assertTrue("Playback capsule must occupy the left side", playback.center.x < viewport.center.x)
                            assertTrue("Tools capsule must occupy the right side", tools.center.x > viewport.center.x)
                            assertTrue("Fullscreen capsules overlap", playback.right <= tools.left + 1f)
                            assertTrue("Tools capsule overlaps fullscreen exit", tools.right <= exit.left + 1f)
                        }
                        if (baseline == null) baseline = bounds else assertEquals("Material changed geometry", baseline, bounds)
                        rule.onNodeWithTag("player_play_pause").performClick()
                        awaitPlayer { !it.player.playWhenReady && it.player.playbackState == Player.STATE_READY }
                        rule.waitUntil(5_000) {
                            rule.onAllNodesWithTag("player_play_pause").fetchSemanticsNodes().size == 1
                        }
                        rule.onNodeWithTag("player_play_pause").performClick()
                        awaitPlayer { it.player.playWhenReady }
                        scenario.onActivity { assertSame(original, it.player) }
                        if (style == AppUiStyle.MATERIAL3) screenshot("${if (landscape) "landscape" else "portrait"}_${if (fullscreen) "fullscreen" else "inline"}_${if (glass) "glass" else "plain"}")
                    }
                }

            }
        }
    }

    @Test
    fun nativeMenusRetainSubtitleToolsAndChangeActualMediaInBothMaterials() {
        scenario.onActivity { it.fullscreen = true; it.rotateToLandscape(true) }
        awaitPlayer { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        for (glass in listOf(false, true)) {
            scenario.onActivity { it.glass = glass; it.player.setPlaybackSpeed(1f) }
            rule.onNodeWithTag("player_speed_action").performScrollTo().performClick()
            rule.onNodeWithText("1.5X").performScrollTo().performClick()
            awaitPlayer { it.player.playbackParameters.speed == 1.5f }
            openSubtitleTools()
            rule.onNodeWithText("大字号").assertExists()
            rule.onNodeWithText("锁定位置").assertExists()
            rule.onNodeWithText("关闭", useUnmergedTree = true).performScrollTo().performClick()
            awaitPlayer { C.TRACK_TYPE_TEXT in it.player.trackSelectionParameters.disabledTrackTypes }
            openSubtitleTools()
            rule.onNodeWithText("Local English subtitles").performScrollTo().performClick()
            awaitPlayer { it.subtitleSelected }
            rule.onNodeWithTag("player_ratio_action").performScrollTo().performClick()
            rule.onNodeWithText("裁剪").performScrollTo().performClick()
            awaitPlayer { it.playerView.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM }
            rule.onNodeWithTag("player_top_more").performClick()
            val menuBounds = rule.onNodeWithTag("player_settings_panel").fetchSemanticsNode().boundsInRoot
            var menuMaxWidth = 0f
            scenario.onActivity { menuMaxWidth = 320f * it.resources.displayMetrics.density }
            assertTrue("Landscape settings must not consume the wider episode-panel slot", menuBounds.width <= menuMaxWidth + 1f)
            rule.onNodeWithText("字幕设置").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("加载字幕").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("播放信息").performScrollTo().assertIsDisplayed()
            screenshot("native_more_${if (glass) "glass" else "plain"}")
            rule.onNodeWithText("选集").performScrollTo().performClick()
            val target = if (glass) 1 else 2
            rule.onNodeWithText("Local episode ${target + 1}", substring = true).performScrollTo().performClick()
            awaitPlayer { it.selectedPage == target && it.player.currentMediaItem?.mediaId == "$target:" && it.player.playbackState == Player.STATE_READY }
            rule.onNodeWithTag("player_top_more").performClick()
            rule.onNodeWithText("上一集").performScrollTo().performClick()
            awaitPlayer { it.selectedPage == target - 1 && it.player.currentMediaItem?.mediaId == "${target - 1}:" }
        }
    }

    @Test
    fun portraitFeedUsesSharedNativeSettingsAndSingleScreenCenteredPlaybackControl() {
        scenario.onActivity { it.rotateToLandscape(false); it.fullscreen = true; it.portraitFeed = true }
        awaitPlayer { it.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        for (glass in listOf(false, true)) {
            scenario.onActivity { it.glass = glass }
            rule.onAllNodesWithTag("player_play_pause").assertCountEquals(1)
            assertPlaybackGlyphCentered(fullscreen = true)
            screenshot("portrait_feed_${if (glass) "glass" else "plain"}")
            rule.onNodeWithContentDescription("更多设置").performClick()
            rule.onNodeWithText("加载字幕").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("选集").performScrollTo().performClick()
            rule.onNodeWithText("Local episode 3", substring = true).performScrollTo().performClick()
            awaitPlayer { it.selectedPage == 2 }
            rule.onNodeWithContentDescription("未锁定").performClick()
            awaitPlayer { it.locked }
            rule.onNodeWithTag("player_play_pause").assertDoesNotExist()
            rule.onNodeWithContentDescription("已锁定").performClick()
            awaitPlayer { !it.locked }
        }
    }
    @Test
    fun progressPreferenceIsHonoredFullscreenAndStaysAboveTheInlineControls() {
        scenario.onActivity { it.fullscreen = true; it.rotateToLandscape(true) }
        awaitPlayer { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        for (glass in listOf(false, true)) {
            scenario.onActivity { it.glass = glass }
            for (placement in PlayerProgressPlacement.entries) {
                runBlocking { SettingsManager.setPlayerProgressPlacement(context, placement) }
                rule.waitUntil(5_000) {
                    val progress = rule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
                    val controls = rule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
                    if (placement == PlayerProgressPlacement.ABOVE_CONTROLS) progress.bottom <= controls.top
                    else progress.top >= controls.bottom
                }
            }
        }
        for (landscape in listOf(false, true)) {
            scenario.onActivity { it.fullscreen = false; it.rotateToLandscape(landscape) }
            awaitPlayer { (it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) == landscape }
            for (glass in listOf(false, true)) {
                scenario.onActivity { it.glass = glass }
                for (placement in PlayerProgressPlacement.entries) {
                    runBlocking { SettingsManager.setPlayerProgressPlacement(context, placement) }
                    rule.waitUntil(5_000) {
                        val progress = rule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
                        val controls = rule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
                        progress.bottom <= controls.top
                    }
                    val seekArea = rule.onNodeWithTag("player_progress_seek_area").fetchSemanticsNode().boundsInRoot
                    val viewport = rule.onNodeWithTag("parity_player_viewport").fetchSemanticsNode().boundsInRoot
                    assertTrue("Inline scrubber needs space at both video edges",
                        seekArea.left > viewport.left && seekArea.right < viewport.right)
                }
            }
        }
    }

    @Test
    fun inlineSeekEndpointsClearBackGesturesAndDragActualPlayback() {
        scenario.onActivity { it.player.pause() }
        awaitPlayer { !it.player.playWhenReady }
        for (landscape in listOf(false, true)) {
            scenario.onActivity { it.fullscreen = false; it.rotateToLandscape(landscape) }
            awaitPlayer { (it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) == landscape }
            for (glass in listOf(false, true)) {
                scenario.onActivity { it.glass = glass }
                rule.waitUntil(5_000) {
                    rule.onAllNodes(hasText(" / 00:03", substring = true)).fetchSemanticsNodes().isNotEmpty()
                }
                val seekArea = rule.onNodeWithTag("player_progress_seek_area").assertIsDisplayed()
                val bounds = seekArea.fetchSemanticsNode().boundsInRoot
                val window = rule.onNodeWithTag("parity_window").fetchSemanticsNode().boundsInRoot
                scenario.onActivity {
                    val insets = androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                        ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemGestures())
                    assertTrue("Seek start overlaps edge-back gestures",
                        bounds.left >= window.left + (insets?.left ?: 0) - 1f)
                    assertTrue("Seek end overlaps edge-back gestures",
                        bounds.right <= window.right - (insets?.right ?: 0) + 1f)
                }
                for ((from, to) in listOf(0.01f to 0.3f, 0.99f to 0.7f)) {
                    seekArea.performTouchInput {
                        swipe(percentOffset(from, 0.9f), percentOffset(to, 0.9f), durationMillis = 600)
                    }
                    rule.waitForIdle()
                    scenario.onActivity {
                        val expected = (it.player.duration * to).toDouble()
                        assertFalse("Dragging seek must preserve pause (landscape=$landscape, glass=$glass)", it.player.playWhenReady)
                        assertEquals("Endpoint $from to $to did not seek (landscape=$landscape, glass=$glass)",
                            expected, it.player.currentPosition.toDouble(), 150.0)
                    }
                }
                if (!landscape) screenshot("inline_seek_clearance_${if (glass) "glass" else "plain"}")
            }
        }
    }

    private fun assertPlaybackGlyphCentered(fullscreen: Boolean) {
        val target = rule.onNodeWithTag(
            if (fullscreen) "parity_window" else "parity_video_viewport",
        ).fetchSemanticsNode().boundsInRoot
        // The AnimatedVisibility/touch wrapper alone can be centered while the visible
        // icon is displaced by native button measurement. Inspect the rendered icon node.
        val glyph = rule.onNodeWithTag("player_play_pause_glyph", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val control = rule.onNodeWithTag("player_play_pause").fetchSemanticsNode().boundsInRoot
        assertEquals("Visible playback glyph horizontal center", target.center.x, glyph.center.x, 1f)
        assertEquals("Visible playback glyph vertical center", target.center.y, glyph.center.y, 1f)
        assertEquals("Touch wrapper and glyph horizontal center differ", control.center.x, glyph.center.x, 1f)
        assertEquals("Touch wrapper and glyph vertical center differ", control.center.y, glyph.center.y, 1f)
    }

    private fun openSubtitleTools() {
        rule.onNodeWithTag("player_subtitle_action").performScrollTo().performClick()
    }


    private fun screenshot(name: String) {
        rule.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(context.getExternalFilesDir(null), "player_parity_$name.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    private fun awaitPlayer(predicate: (PlayerParityTestActivity) -> Boolean) {
        rule.waitUntil(20_000) {
            var matches = false
            scenario.onActivity { matches = predicate(it) }
            matches
        }
    }
}
