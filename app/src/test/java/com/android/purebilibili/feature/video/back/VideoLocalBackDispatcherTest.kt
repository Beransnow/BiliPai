package com.android.purebilibili.feature.video.back

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLocalBackDispatcherTest {
    @Test
    fun activeTargetUsesExplicitPriorityInsteadOfRegistrationOrder() {
        val dispatcher = VideoLocalBackDispatcher()
        dispatcher.register("thread", VideoLocalBackTarget.COMMENT_THREAD) {}
        dispatcher.register("queue", VideoLocalBackTarget.PLAYLIST_QUEUE) {}

        assertEquals(VideoLocalBackTarget.COMMENT_THREAD, dispatcher.activeTarget)
    }

    @Test
    fun gestureTargetIsFrozenWhenRegistrationsChange() {
        val dispatcher = VideoLocalBackDispatcher()
        dispatcher.register("queue", VideoLocalBackTarget.PLAYLIST_QUEUE) {}

        assertEquals(VideoLocalBackTarget.PLAYLIST_QUEUE, dispatcher.beginGesture())
        dispatcher.register("conversation", VideoLocalBackTarget.COMMENT_CONVERSATION) {}
        dispatcher.updateGestureProgress(0.42f)

        assertEquals(VideoLocalBackTarget.PLAYLIST_QUEUE, dispatcher.frozenTarget)
        assertEquals(0.42f, dispatcher.progressFor(VideoLocalBackTarget.PLAYLIST_QUEUE), 0.001f)
        assertEquals(0f, dispatcher.progressFor(VideoLocalBackTarget.COMMENT_CONVERSATION), 0.001f)
    }

    @Test
    fun cancelResetsProgressWithoutCommittingTarget() {
        val dispatcher = VideoLocalBackDispatcher()
        var commits = 0
        dispatcher.register("detail", VideoLocalBackTarget.PORTRAIT_DETAIL) { commits++ }
        dispatcher.beginGesture()
        dispatcher.updateGestureProgress(0.8f)

        assertTrue(dispatcher.cancelGesture())

        assertEquals(0, commits)
        assertNull(dispatcher.frozenTarget)
        assertEquals(0f, dispatcher.progressFor(VideoLocalBackTarget.PORTRAIT_DETAIL), 0.001f)
    }

    @Test
    fun completeCommitsFrozenTargetExactlyOnce() {
        val dispatcher = VideoLocalBackDispatcher()
        var queueCommits = 0
        var threadCommits = 0
        dispatcher.register("queue", VideoLocalBackTarget.PLAYLIST_QUEUE) { queueCommits++ }
        dispatcher.beginGesture()
        dispatcher.register("thread", VideoLocalBackTarget.COMMENT_THREAD) { threadCommits++ }

        assertTrue(dispatcher.completeGesture())
        assertFalse(dispatcher.completeGesture())

        assertEquals(1, queueCommits)
        assertEquals(0, threadCommits)
    }

    @Test
    fun expectedTargetPreventsStaleButtonFromClosingAnotherOverlay() {
        val dispatcher = VideoLocalBackDispatcher()
        var commits = 0
        dispatcher.register("thread", VideoLocalBackTarget.COMMENT_THREAD) { commits++ }

        assertFalse(dispatcher.requestBack(VideoLocalBackTarget.PLAYLIST_QUEUE))
        assertEquals(0, commits)
        assertTrue(dispatcher.requestBack(VideoLocalBackTarget.COMMENT_THREAD))
        assertEquals(1, commits)
    }

    @Test
    fun disposingLatestRegistrationRestoresPreviousTarget() {
        val dispatcher = VideoLocalBackDispatcher()
        dispatcher.register("queue", VideoLocalBackTarget.PLAYLIST_QUEUE) {}
        val thread = dispatcher.register("thread", VideoLocalBackTarget.COMMENT_THREAD) {}

        thread.dispose()

        assertEquals(VideoLocalBackTarget.PLAYLIST_QUEUE, dispatcher.activeTarget)
    }
}
