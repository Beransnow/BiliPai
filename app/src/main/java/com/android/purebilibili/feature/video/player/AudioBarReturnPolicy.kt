package com.android.purebilibili.feature.video.player

internal fun shouldActivateAudioBarOnVideoExit(
    barEnabled: Boolean,
    stopPlaybackOnExit: Boolean,
    hasPlayer: Boolean,
    hasVideoIdentity: Boolean,
    isLive: Boolean,
    isMiniOrPip: Boolean,
    isNavigatingToVideo: Boolean,
): Boolean = barEnabled && !stopPlaybackOnExit && hasPlayer && hasVideoIdentity &&
    !isLive && !isMiniOrPip && !isNavigatingToVideo
