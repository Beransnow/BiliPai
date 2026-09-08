package com.android.purebilibili.feature.audio.screen

internal fun resolveAudioNowPlayingVisible(
    sessionActive: Boolean,
    isOnAudioModeScreen: Boolean,
    isInPipMode: Boolean,
    hasCurrentItem: Boolean,
    barEnabled: Boolean,
    isVideoDetailDestination: Boolean = false,
    isChromeTransitionRunning: Boolean = false
): Boolean {
    return barEnabled &&
        sessionActive &&
        !isOnAudioModeScreen &&
        !isInPipMode &&
        hasCurrentItem &&
        !isVideoDetailDestination &&
        !isChromeTransitionRunning
}
