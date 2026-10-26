package com.android.purebilibili.feature.video.ui.overlay

/** Menu ownership stays with the player while portrait feeds supply their information layout. */
data class PlayerChromePresentation(
    val controlsVisible: Boolean,
    val isPlaying: Boolean,
    val currentSpeed: Float,
    val openSettings: () -> Unit,
    val openQuality: () -> Unit,
    val openSpeed: () -> Unit,
    val openRatio: () -> Unit,
    val openAudio: () -> Unit,
    val openSubtitle: () -> Unit,
    val openEpisodes: () -> Unit,
)

internal enum class PlayerQuickMenu { AUDIO, SUBTITLE }
