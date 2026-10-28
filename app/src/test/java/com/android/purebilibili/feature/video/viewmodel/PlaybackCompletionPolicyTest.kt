package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackCompletionPolicyTest {

    @Test
    fun `end timer only stops the selected video part`() {
        val selected = "BV1" to 123L
        assertTrue(shouldStopAfterCurrentPlayback(selected, "BV1", 123L))
        assertFalse(shouldStopAfterCurrentPlayback(selected, "BV1", 456L))
        assertFalse(shouldStopAfterCurrentPlayback(selected, "BV2", 123L))
        assertFalse(shouldStopAfterCurrentPlayback(null, "BV1", 123L))
    }

    @Test
    fun `audio mode leaves repeat decisions to audio queue`() {
        assertEquals(
            Player.REPEAT_MODE_OFF,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE, isInAudioMode = true),
        )
        assertEquals(
            Player.REPEAT_MODE_ONE,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE, isInAudioMode = false),
        )
    }

    @Test
    fun `comment interaction suppresses playback completion navigation`() {
        assertTrue(
            shouldSuppressPlaybackCompletionForCommentInteraction(
                commentInteractionActive = true
            )
        )
        assertFalse(
            shouldSuppressPlaybackCompletionForCommentInteraction(
                commentInteractionActive = false
            )
        )
    }

    @Test
    fun `stop mode always stops after ended`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = true,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true
            )
        )
    }

    @Test
    fun `play in order should request next in playlist`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `repeat one should replay current`() {
        assertEquals(
            PlaybackEndAction.REPEAT_CURRENT,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.REPEAT_ONE,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `loop playlist should request next with loop`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.LOOP_PLAYLIST,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto mode stops when current video has no next page or season episode`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = true,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto mode continues when current video has next page or season episode`() {
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = true,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false,
                hasNextPageOrSeasonTarget = true
            )
        )
    }

    @Test
    fun `auto mode stops before next page or season episode when autoplay switch is off`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = true,
                hasNextPageOrSeasonTarget = true
            )
        )
    }

    @Test
    fun `auto continue stops for external playlist when dedicated switch is off`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto continue still continues for external playlist when dedicated switch is on`() {
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true
            )
        )
    }

    @Test
    fun `favorite external playlist should respect explicit stop behavior`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playMode = PlayMode.SEQUENTIAL
            )
        )
    }

    @Test
    fun `favorite external playlist should respect play in order behavior`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playMode = PlayMode.REPEAT_ONE
            )
        )
    }

    @Test
    fun `non-favorite external playlist should still respect selected completion behavior`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = true,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playMode = PlayMode.SEQUENTIAL
            )
        )
    }

    @Test
    fun `repeat one maps to exoplayer repeat one mode`() {
        assertEquals(
            Player.REPEAT_MODE_ONE,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE)
        )
    }

    @Test
    fun `non repeat one completion modes keep exoplayer repeat off`() {
        listOf(
            PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
            PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
            PlaybackCompletionBehavior.PLAY_IN_ORDER,
            PlaybackCompletionBehavior.LOOP_PLAYLIST
        ).forEach { behavior ->
            assertEquals(Player.REPEAT_MODE_OFF, resolvePlaybackCompletionRepeatMode(behavior))
        }
    }
}
