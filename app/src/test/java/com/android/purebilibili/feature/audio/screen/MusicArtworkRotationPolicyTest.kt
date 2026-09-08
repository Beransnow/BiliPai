package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicArtworkRotationPolicyTest {

    @Test
    fun rotatesOnlyWhilePlayingWithoutReduceMotion() {
        assertTrue(shouldRotateMusicArtwork(isPlaying = true, reduceMotion = false))
        assertFalse(shouldRotateMusicArtwork(isPlaying = false, reduceMotion = false))
        assertFalse(shouldRotateMusicArtwork(isPlaying = true, reduceMotion = true))
        assertFalse(shouldRotateMusicArtwork(isPlaying = false, reduceMotion = true))
    }
}
