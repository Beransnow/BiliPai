package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioNowPlayingVisibilityPolicyTest {

    @Test
    fun visibleOnHomeWhenListeningAndNotOnPlayerPage() {
        assertTrue(
            resolveAudioNowPlayingVisible(
                sessionActive = true,
                isOnAudioModeScreen = false,
                isInPipMode = false,
                hasCurrentItem = true
            )
        )
    }

    @Test
    fun hiddenOnAudioModeScreenPipOrEmptyQueue() {
        assertFalse(
            resolveAudioNowPlayingVisible(
                sessionActive = true,
                isOnAudioModeScreen = true,
                isInPipMode = false,
                hasCurrentItem = true
            )
        )
        assertFalse(
            resolveAudioNowPlayingVisible(
                sessionActive = true,
                isOnAudioModeScreen = false,
                isInPipMode = true,
                hasCurrentItem = true
            )
        )
        assertFalse(
            resolveAudioNowPlayingVisible(
                sessionActive = true,
                isOnAudioModeScreen = false,
                isInPipMode = false,
                hasCurrentItem = false
            )
        )
        assertFalse(
            resolveAudioNowPlayingVisible(
                sessionActive = false,
                isOnAudioModeScreen = false,
                isInPipMode = false,
                hasCurrentItem = true
            )
        )
    }
}
