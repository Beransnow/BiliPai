package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive

internal const val MUSIC_ARTWORK_ROTATION_DURATION_MS = 24_000

internal fun shouldRotateMusicArtwork(
    isPlaying: Boolean,
    reduceMotion: Boolean
): Boolean = isPlaying && !reduceMotion

@Composable
internal fun rememberMusicArtworkRotationDegrees(
    active: Boolean,
    contentKey: String
): () -> Float {
    val rotation = remember(contentKey) { Animatable(0f) }
    LaunchedEffect(active, contentKey) {
        if (!active) return@LaunchedEffect
        while (isActive) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(
                    durationMillis = MUSIC_ARTWORK_ROTATION_DURATION_MS,
                    easing = LinearEasing
                )
            )
        }
    }
    return remember(rotation) { { rotation.value } }
}
