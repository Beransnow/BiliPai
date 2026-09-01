package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge

class MiuixCardBackNavTransitionTest {

    @Test
    fun `card frame follows progress and clamps invalid values`() {
        val start = resolveMiuixCardBackFrame(
            progress = -1f,
            widthPx = 1000f,
            cornerRadiusPx = 60f,
            exitDirectionSign = 1f,
        )
        val middle = resolveMiuixCardBackFrame(
            progress = 0.5f,
            widthPx = 1000f,
            cornerRadiusPx = 60f,
            exitDirectionSign = 1f,
        )
        val end = resolveMiuixCardBackFrame(
            progress = 2f,
            widthPx = 1000f,
            cornerRadiusPx = 60f,
            exitDirectionSign = -1f,
        )

        assertEquals(1f, start.scale, 0.0001f)
        assertEquals(0f, start.translationX, 0.0001f)
        assertEquals(0f, start.cornerRadiusPx, 0.0001f)
        assertEquals(0.95f, middle.scale, 0.0001f)
        assertEquals(62.5f, middle.translationX, 0.0001f)
        assertEquals(30f, middle.cornerRadiusPx, 0.0001f)
        assertEquals(0.9f, end.scale, 0.0001f)
        assertEquals(-125f, end.translationX, 0.0001f)
        assertEquals(60f, end.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun `exit direction follows gesture edge then layout direction`() {
        assertEquals(1f, resolveMiuixCardBackExitDirectionSign(NavSwipeEdge.Left, LayoutDirection.Ltr))
        assertEquals(-1f, resolveMiuixCardBackExitDirectionSign(NavSwipeEdge.Right, LayoutDirection.Ltr))
        assertEquals(1f, resolveMiuixCardBackExitDirectionSign(null, LayoutDirection.Ltr))
        assertEquals(-1f, resolveMiuixCardBackExitDirectionSign(NavSwipeEdge.None, LayoutDirection.Rtl))
    }

    @Test
    fun `enhancement is selected only for enabled Miuix style`() {
        assertTrue(
            shouldUseMiuixCardBackTransition(
                animation = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                enabled = true,
            ),
        )
        assertFalse(
            shouldUseMiuixCardBackTransition(
                animation = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                enabled = false,
            ),
        )
        assertFalse(
            shouldUseMiuixCardBackTransition(
                animation = BiliPaiPredictiveBackAnimationStyle.SCALE,
                enabled = true,
            ),
        )
    }

    @Test
    fun `gesture conflict routes keep their excluded or dedicated transitions`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt"),
        ).first(File::exists).readText()

        listOf(
            "Search",
            "JsPluginContent",
            "ExternalMedia",
            "OfflineVideoPlayer",
            "AudioMode",
            "BangumiPlayer",
            "MusicDetail",
            "NativeMusic",
            "Live",
            "Web",
        ).forEach { keyName ->
            val entryBlock = source
                .substringAfter("entry<BiliPaiNavKey.$keyName>(")
                .substringBefore("content = content")
            assertTrue(
                entryBlock.contains("transition = cardBackExcludedTransition"),
                "$keyName must not use the card-depth transition",
            )
        }
        assertTrue(
            source.substringAfter("entry<BiliPaiNavKey.VideoDetail>(")
                .substringBefore("content = content")
                .contains("transition = videoCardTransition"),
        )
        assertTrue(
            source.substringAfter("entry<BiliPaiNavKey.Story>(")
                .substringBefore("content = content")
                .contains("transition = fullscreenVideoCardTransition"),
        )
    }
}
