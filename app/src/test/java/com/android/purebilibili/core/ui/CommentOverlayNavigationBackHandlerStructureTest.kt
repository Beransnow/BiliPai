package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentOverlayNavigationBackHandlerStructureTest {

    @Test
    fun videoCommentOverlayUsesNavigationEventBackHandling() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/VideoCommentSheetHost.kt"
        ).readText()

        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("isBackEnabled = hostVisible"))
        assertTrue(source.contains("resolveVideoCommentPredictiveBackTarget("))
        assertTrue(source.contains("threadBackProgress"))
        assertTrue(source.contains("translationY = resolveCommentThreadPredictiveBackOffsetY("))
        assertTrue(source.contains("slideInVertically("))
        assertTrue(source.contains("resolvePredictiveBackBlurFrame("))
        assertFalse(source.contains("translationX = threadBackProgress"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
    }

    @Test
    fun dynamicCommentSheetsUseNavigationEventBackHandling() {
        val mainSheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val subReplySource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplySheet.kt"
        ).readText()

        assertTrue(mainSheetSource.contains("WindowNavigationEventBridge()"))
        assertTrue(mainSheetSource.contains("NavigationBackHandler("))
        assertTrue(mainSheetSource.contains("resolveVideoCommentPredictiveBackTarget("))
        assertTrue(mainSheetSource.contains("threadBackProgress"))
        assertTrue(mainSheetSource.contains("translationY = resolveCommentThreadPredictiveBackOffsetY("))
        assertTrue(mainSheetSource.contains("slideInVertically("))
        assertTrue(mainSheetSource.contains("SubReplyDetailContent("))
        assertTrue(subReplySource.contains("WindowNavigationEventBridge()"))
        assertTrue(subReplySource.contains("LocalNavigationBackHandler("))
    }
}
