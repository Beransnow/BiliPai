package com.android.purebilibili.feature.audio.screen

internal fun resolveAudioNowPlayingVisible(
    sessionActive: Boolean,
    isOnAudioModeScreen: Boolean,
    isInPipMode: Boolean,
    hasCurrentItem: Boolean,
    barEnabled: Boolean
): Boolean {
    return barEnabled && sessionActive && !isOnAudioModeScreen && !isInPipMode && hasCurrentItem
}
