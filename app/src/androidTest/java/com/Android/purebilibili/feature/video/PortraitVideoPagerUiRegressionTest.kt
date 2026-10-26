@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.Android.purebilibili.feature.video

import androidx.compose.foundation.background
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlay
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayActions
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayState
import com.android.purebilibili.feature.video.ui.overlay.portrait.PortraitPagerControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.pager.PortraitVideoViewportContainer
import com.android.purebilibili.feature.video.ui.pager.resolvePortraitInitialVideoAspectRatio
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortraitVideoPagerUiRegressionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun targetVideoNotReady_usesPortraitFallbackViewportInsteadOfStaleLandscapeFrame() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 640.dp)
                        .background(Color.Black)
                ) {
                    PortraitVideoViewportContainer(
                        currentVideoAspect = resolvePortraitInitialVideoAspectRatio(
                            itemBvid = "BV_NEXT",
                            currentPlayingBvid = "BV_PREV",
                            playerVideoWidth = 1920,
                            playerVideoHeight = 1080
                        ),
                        modifier = Modifier.fillMaxSize(),
                        viewportModifier = Modifier
                            .testTag("portrait_viewport")
                            .background(Color.Red)
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("portrait_viewport")
            .assertWidthIsEqualTo(360.dp)
            .assertHeightIsEqualTo(640.dp)
    }

    @Test
    fun landscapeFitViewport_doesNotPaintOverPortraitLetterboxInEitherMaterial() {
        var glass by mutableStateOf(false)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass)) {
                MaterialTheme {
                    Box(Modifier.size(320.dp, 480.dp).background(Color.Green).testTag("page")) {
                        PortraitVideoViewportContainer(
                            currentVideoAspect = 16f / 9f,
                            aspectRatio = VideoAspectRatio.FIT,
                            videoWidth = 1920,
                            videoHeight = 1080,
                            modifier = Modifier.fillMaxSize(),
                            viewportModifier = Modifier.testTag("landscape_viewport").background(Color.Red),
                        ) {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        for (useGlass in listOf(false, true)) {
            composeTestRule.runOnIdle { glass = useGlass }
            composeTestRule.onNodeWithTag("landscape_viewport")
                .assertWidthIsEqualTo(320.dp).assertHeightIsEqualTo(180.dp)
            val page = composeTestRule.onNodeWithTag("page")
            val viewport = composeTestRule.onNodeWithTag("landscape_viewport").fetchSemanticsNode().boundsInRoot
            val pageBounds = page.fetchSemanticsNode().boundsInRoot
            val pixels = page.captureToImage().toPixelMap()
            val x = pixels.width / 2
            assertEquals(Color.Green, pixels[x, 1])
            assertEquals(Color.Green, pixels[x, pixels.height - 2])
            val videoCenterY = (viewport.center.y - pageBounds.top).toInt()
            assertEquals(Color.Red, pixels[x, videoCenterY])
            // The lifted 16:9 frame remains entirely visible; there is no crop-to-fill.
            assertTrue(viewport.top > pageBounds.top)
            assertTrue(viewport.bottom < pageBounds.bottom)
        }
    }

    @Test
    fun inlinePortrait_appTopBarIsVisibleAndClickableInBothMaterials() {
        assertCanonicalPortraitTopBar(portraitFullscreen = false)
    }

    @Test
    fun immersivePortraitFullscreen_appTopBarStaysSafeAndObeysVisibilityAndLock() {
        composeTestRule.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(composeTestRule.activity.window, false)
            WindowInsetsControllerCompat(
                composeTestRule.activity.window, composeTestRule.activity.window.decorView,
            ).hide(WindowInsetsCompat.Type.systemBars())
        }
        assertCanonicalPortraitTopBar(portraitFullscreen = true)
    }

    private fun assertCanonicalPortraitTopBar(portraitFullscreen: Boolean) {
        var glass by mutableStateOf(false)
        var visible by mutableStateOf(true)
        var locked by mutableStateOf(false)
        var backClicks = 0
        var homeClicks = 0
        var menuVisible = false
        var safeTopPx = 0
        composeTestRule.setContent {
            val context = LocalContext.current
            val player = remember { ExoPlayer.Builder(context).build() }
            DisposableEffect(player) { onDispose { player.release() } }
            safeTopPx = WindowInsets.safeDrawing.union(WindowInsets.statusBarsIgnoringVisibility)
                .getTop(LocalDensity.current)
            CompositionLocalProvider(LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass)) {
                MaterialTheme {
                    MiuixTheme {
                        Box(Modifier.size(360.dp, 480.dp).background(Color.Black).testTag("chrome_page")) {
                            val state = VideoPlayerOverlayState(
                                player = player, title = "Portrait regression",
                                isVisible = visible, isFullscreen = portraitFullscreen,
                                isScreenLocked = locked,
                                currentQualityLabel = "1080P", qualityLabels = emptyList(),
                                playerDiagnosticLoggingEnabled = false,
                            )
                            val actions = VideoPlayerOverlayActions(
                                onToggleVisible = { visible = !visible },
                                onQualitySelected = {},
                                onBack = { backClicks++ },
                                onHomeClick = { homeClicks++ },
                                onFloatingPanelVisibilityChange = { menuVisible = it },
                                onToggleFullscreen = {},
                            )
                            VideoPlayerOverlay(
                                state = state,
                                actions = actions,
                                chromeContent = if (portraitFullscreen) {
                                    { presentation ->
                                        PortraitPagerControls(
                                            state = state, actions = actions, presentation = presentation,
                                            info = null, stat = Stat(), progress = PlayerProgress(0L, 120_000L, 0L),
                                            videoAspectRatio = 16f / 9f, commentExpansionProgress = 0f,
                                            onDetailClick = {}, onAuthorClick = {},
                                            onStaffMemberClick = {}, onSearchClick = {},
                                        )
                                    }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
        for (useGlass in listOf(false, true)) {
            composeTestRule.runOnIdle { glass = useGlass }
            composeTestRule.onNodeWithTag("player_top_bar").assertIsDisplayed()
            val back = composeTestRule.onNodeWithContentDescription("返回").assertIsDisplayed()
            val more = composeTestRule.onNodeWithContentDescription("更多设置")
                .assertIsDisplayed().assertHasClickAction()
            if (portraitFullscreen) {
                val pageTop = composeTestRule.onNodeWithTag("chrome_page").fetchSemanticsNode().boundsInRoot.top
                for (description in listOf("返回", "主界面", "更多设置")) {
                    val control = composeTestRule.onNodeWithContentDescription(description)
                        .assertIsDisplayed().assertHasClickAction()
                    assertTrue("$description must remain below hidden-bar/cutout safe area",
                        control.fetchSemanticsNode().boundsInRoot.top >= pageTop + safeTopPx)
                }
                composeTestRule.onNodeWithContentDescription("主界面").performClick()
            }
            back.performClick()
            composeTestRule.runOnIdle { visible = false }
            composeTestRule.onNodeWithTag("player_top_bar").assertDoesNotExist()
            composeTestRule.runOnIdle { visible = true; locked = true }
            composeTestRule.onNodeWithTag("player_top_bar").assertDoesNotExist()
            composeTestRule.runOnIdle { locked = false }
            composeTestRule.onNodeWithTag("player_top_bar").assertIsDisplayed()
            if (useGlass) {
                more.performClick()
                composeTestRule.waitUntil(5_000) { menuVisible }
                if (portraitFullscreen) {
                    composeTestRule.onNodeWithTag("player_top_bar").assertDoesNotExist()
                }
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(2, backClicks)
            assertEquals(if (portraitFullscreen) 2 else 0, homeClicks)
        }
    }
}
