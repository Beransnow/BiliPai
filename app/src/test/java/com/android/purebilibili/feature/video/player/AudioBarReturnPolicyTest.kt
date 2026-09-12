package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioBarReturnPolicyTest {
    @Test
    fun eligibleVideoReturnActivatesAudioBar() {
        assertTrue(
            shouldActivateAudioBarOnVideoExit(
                barEnabled = true,
                stopPlaybackOnExit = false,
                hasPlayer = true,
                hasVideoIdentity = true,
                isLive = false,
                isMiniOrPip = false,
                isNavigatingToVideo = false,
            )
        )
    }

    @Test
    fun explicitStopAndNonVideoDestinationsDoNotActivateAudioBar() {
        assertFalse(eligible(stopPlaybackOnExit = true))
        assertFalse(eligible(isLive = true))
        assertFalse(eligible(isMiniOrPip = true))
        assertFalse(eligible(isNavigatingToVideo = true))
    }

    private fun eligible(
        stopPlaybackOnExit: Boolean = false,
        isLive: Boolean = false,
        isMiniOrPip: Boolean = false,
        isNavigatingToVideo: Boolean = false,
    ) = shouldActivateAudioBarOnVideoExit(
        barEnabled = true,
        stopPlaybackOnExit = stopPlaybackOnExit,
        hasPlayer = true,
        hasVideoIdentity = true,
        isLive = isLive,
        isMiniOrPip = isMiniOrPip,
        isNavigatingToVideo = isNavigatingToVideo,
    )
}
