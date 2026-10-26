package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.motion.AppMotionEasing

internal fun resolvePlayerPlaybackCenterOffset(
    isFullscreen: Boolean,
    playerBoundsInWindow: IntRect?,
    windowSize: IntSize,
    inlineVideoTopInsetPx: Int = 0,
): IntOffset {
    if (!isFullscreen) {
        // VideoPlayerSection excludes the status/title strip from its video plane,
        // while chrome spans the whole host. Center in [inset, hostHeight], not [0, hostHeight].
        val topInset = inlineVideoTopInsetPx.coerceIn(0, playerBoundsInWindow?.height?.coerceAtLeast(0) ?: Int.MAX_VALUE)
        return IntOffset(0, topInset / 2)
    }
    if (playerBoundsInWindow == null ||
        playerBoundsInWindow.width <= 0 || playerBoundsInWindow.height <= 0
    ) return IntOffset.Zero
    return IntOffset(
        (windowSize.width - playerBoundsInWindow.left - playerBoundsInWindow.right) / 2,
        (windowSize.height - playerBoundsInWindow.top - playerBoundsInWindow.bottom) / 2,
    )
}

/** Inline targets the video below its top strip; fullscreen targets the physical window. */
@Composable
internal fun Modifier.playerPlaybackCenter(
    isFullscreen: Boolean,
    playerBoundsInWindow: IntRect?,
    inlineVideoTopInset: Dp = 0.dp,
): Modifier {
    val windowSize = LocalWindowInfo.current.containerSize
    val inlineVideoTopInsetPx = with(LocalDensity.current) { inlineVideoTopInset.roundToPx() }
    return offset {
        resolvePlayerPlaybackCenterOffset(isFullscreen, playerBoundsInWindow, windowSize, inlineVideoTopInsetPx)
    }
}

/** The central play/pause target follows the visibility of the rest of the player controls. */
@Composable
internal fun CenterPlaybackControl(
    visible: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    diameter: Dp,
    modifier: Modifier = Modifier,
    height: Dp = diameter,
) {
    val glass = LocalAppThemeConfig.current.liquidGlassEnabled
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.size(width = diameter, height = height).testTag("player_play_pause"),
        enter = fadeIn(tween(200, easing = AppMotionEasing.Continuity)),
        exit = fadeOut(tween(200, easing = AppMotionEasing.Continuity)),
    ) {
        PlayerControlTouchBounds(height = height) {
            // Keep the visible shell and glyph in the exact-size target, independently
            // of native button minimum-touch policies or the selected material.
            PlayerControlGlassDock(
                enabled = true,
                shape = CircleShape,
                modifier = Modifier.width(diameter).height(height),
            ) {
                AppSurface(
                    shape = CircleShape,
                    color = if (glass) Color.Transparent else Color.Black.copy(alpha = 0.38f),
                    border = if (glass) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AppIcon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停播放" else "开始播放",
                            tint = Color.White,
                            modifier = Modifier.size(diameter * 0.48f).testTag("player_play_pause_glyph"),
                        )
                    }
                }
            }
        }
    }
}
