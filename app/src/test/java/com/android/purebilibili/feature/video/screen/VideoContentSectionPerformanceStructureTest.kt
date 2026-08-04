package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoContentSectionPerformanceStructureTest {

    @Test
    fun videoContentSection_usesDerivedStateForMotionBudgetAndCommentLiteMode() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )

        assertTrue(source.contains("val isIntroListScrolling by remember"))
        assertTrue(source.contains("val isCommentListScrolling by remember"))
        assertTrue(source.contains("val videoDetailMotionBudget by remember"))
        assertTrue(source.contains("val lightweightCommentRendering by remember"))
        assertTrue(source.contains("isCommentListScrolling = isCommentListScrolling"))
    }

    @Test
    fun videoContentSection_commentLoadMore_isHoistedOutsideLazyItem() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val commentTabSource = source.substringAfter("private fun VideoCommentTab(")

        assertTrue(commentTabSource.contains("val shouldLoadMore by remember("))
        assertTrue(commentTabSource.contains("LaunchedEffect(shouldLoadMore)"))
        assertFalse(
            commentTabSource.contains(
                "item {\n                    val shouldLoadMore by remember("
            )
        )
    }

    @Test
    fun videoContentSection_pagerAvoidsKeepingIntroAliveOnCommentTab() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )

        assertTrue(source.contains("beyondViewportPageCount = resolveVideoDetailBeyondViewportPageCount("))
        assertTrue(source.contains("isVideoPlaying = isVideoPlaying,"))
        assertTrue(source.contains("selectedTabIndex = pagerState.currentPage"))
    }

    @Test
    fun videoContentSection_collapsesIntroCommentTabBarOnCommentScrollAndExpandsAtTop() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )

        assertTrue(source.contains("shouldCollapseVideoContentTabBarForCommentScroll("))
        assertTrue(source.contains("val collapseTabBarForCommentScroll by remember"))
        assertTrue(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("visible = !collapseTabBarForCommentScroll"))
        assertTrue(source.contains("expandVertically("))
        assertTrue(source.contains("shrinkVertically("))
        // Sort filter bar stays outside this collapse path (comment list chrome).
        assertTrue(source.contains("CommentSortFilterBar("))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/"))
        )
        return candidates.first { it.exists() }.readText()
    }
}
