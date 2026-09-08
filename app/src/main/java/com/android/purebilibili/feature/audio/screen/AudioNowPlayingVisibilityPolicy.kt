package com.android.purebilibili.feature.audio.screen

internal fun resolveAudioNowPlayingVisible(
    sessionActive: Boolean,
    isOnAudioModeScreen: Boolean,
    isInPipMode: Boolean,
    hasCurrentItem: Boolean
): Boolean {
    return sessionActive && !isOnAudioModeScreen && !isInPipMode && hasCurrentItem
}
