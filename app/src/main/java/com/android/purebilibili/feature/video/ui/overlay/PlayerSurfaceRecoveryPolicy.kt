package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldRebindFullscreenSurfaceOnResume(
    hasPlayerView: Boolean,
    hasPlayer: Boolean,
): Boolean = hasPlayerView && hasPlayer
