package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomControlBarPolicyTest {

    @Test
    fun inlineAndFullscreen_shareCompactVerticalMetricsAcrossViewportWidths() {
        for (width in listOf(320, 599, 600, 840, 1600, 2400)) {
            for (compact in listOf(false, true)) {
                val inline = resolvePlayerBottomControlBarLayoutPolicy(width, compact, isFullscreen = false)
                val fullscreen = resolvePlayerBottomControlBarLayoutPolicy(width, compact, isFullscreen = true)

                assertEquals(40, inline.playButtonSizeDp)
                assertEquals(fullscreen.playButtonSizeDp, inline.playButtonSizeDp)
                assertEquals(fullscreen.timeFontSp, inline.timeFontSp)
                assertEquals(fullscreen.actionTextFontSp, inline.actionTextFontSp)
                assertEquals(fullscreen.danmakuIconSizeDp, inline.danmakuIconSizeDp)
                assertEquals(fullscreen.fullscreenIconSizeDp, inline.fullscreenIconSizeDp)
                assertEquals(40, inline.danmakuSettingButtonSizeDp)
                assertEquals(40, inline.danmakuInputHeightDp)
            }
        }
    }

    @Test
    fun inlineChromeReserve_containsOneCompactRowAndEdgeChapterScrubber() {
        for (width in listOf(320, 600, 840, 1600)) {
            val controls = resolvePlayerBottomControlBarLayoutPolicy(width)
            val progress = resolveVideoProgressBarLayoutPolicy(width)

            assertEquals(
                40 + progress.baseHeightWithChapterDp + controls.progressSpacingDp,
                playerBottomControlsHeightDp(width),
            )
        }
    }


    @Test
    fun subtitlePanelTrackOptions_dropBlankAndDeduplicateByTrackKey() {
        val options = resolveSubtitlePanelTrackOptions(
            listOf(
                SubtitleTrackOption(
                    trackKey = "zh",
                    languageCode = "zh-Hans",
                    label = "中文（简体）",
                    selected = true,
                    likelyAi = false
                ),
                SubtitleTrackOption(
                    trackKey = "zh",
                    languageCode = "zh-Hans",
                    label = "重复中文",
                    selected = false,
                    likelyAi = false
                ),
                SubtitleTrackOption(
                    trackKey = "",
                    languageCode = "en-US",
                    label = "英语",
                    selected = false,
                    likelyAi = false
                )
            )
        )

        assertEquals(1, options.size)
        assertEquals("中文（简体）", options.single().label)
    }

}
