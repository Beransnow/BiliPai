package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentImageReplyStructureTest {

    @Test
    fun `reply composers keep image upload available when the comment area allows it`() {
        val detailAdapter = loadSource(
            "feature/video/screen/VideoDetailInputOverlayAdapter.kt"
        )
        val portraitPager = loadSource(
            "feature/video/ui/pager/PortraitVideoPager.kt"
        )

        assertTrue(detailAdapter.contains("canUploadImage = commentState.canUploadImage,"))
        assertTrue(portraitPager.contains("canUploadImage = commentState.canUploadImage,"))
        assertFalse(detailAdapter.contains("commentState.canUploadImage && replyingToComment == null"))
        assertFalse(portraitPager.contains("commentState.canUploadImage && replyingToComment == null"))
    }

    @Test
    fun `comment sender attaches uploaded pictures to root and child replies`() {
        val viewModel = loadSource("feature/video/viewmodel/VideoPlaybackViewModel.kt")
        val sendComment = viewModel
            .substringAfter("fun sendComment(")
            .substringBefore("private suspend fun uploadCommentPictures(")

        assertTrue(sendComment.contains("val replyTo = _replyingToComment.value"))
        assertTrue(sendComment.contains("val (root, parent) = resolveCommentReplyTargets("))
        assertTrue(sendComment.contains("val picturesResult = uploadCommentPictures(imageUris)"))
        assertTrue(sendComment.contains("root = root"))
        assertTrue(sendComment.contains("parent = parent"))
        assertTrue(sendComment.contains("pictures = pictures"))
    }

    private fun loadSource(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }.readText()
    }
}
