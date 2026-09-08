package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.LocalSettingsLiquidGlassEnabled
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell
import kotlin.math.abs

internal data class AudioNowPlayingBarState(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val isPlaying: Boolean
)

@Composable
internal fun AudioNowPlayingBar(
    state: AudioNowPlayingBarState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onDismiss: () -> Unit,
    glassEnabled: Boolean = LocalSettingsLiquidGlassEnabled.current,
    modifier: Modifier = Modifier
) {
    val chrome = resolveMusicPlayerChromeSpec(
        uiStyle = LocalAppUiStyle.current,
        glassEnabled = glassEnabled
    )
    val shape = AppShapes.container(ContainerLevel.Card)
    val containerColor = AppSurfaceTokens.surfaceContainer()
    AppSurface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = chrome.horizontalPaddingDp.dp,
                end = chrome.horizontalPaddingDp.dp,
                bottom = 72.dp
            )
            .then(
                if (glassEnabled) {
                    Modifier.biliPaiFloatingDockShell(
                        backdrop = null,
                        containerColor = containerColor,
                        pressProgress = 0f,
                        shape = shape,
                        enabled = false
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onExpand)
            .audioNowPlayingSkipGesture(
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious
            ),
        shape = shape,
        color = containerColor,
        tonalElevation = if (chrome.uiStyle == com.android.purebilibili.core.theme.AppUiStyle.MATERIAL3 && !glassEnabled) {
            3.dp
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(if (chrome.coverShapeIsCircle) CircleShape else AppShapes.container(ContainerLevel.Field)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                AppText(
                    text = state.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                AppText(
                    text = state.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppIconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
                AppIcon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "暂停" else "播放"
                )
            }
            AppIconButton(onClick = onExpand, modifier = Modifier.size(44.dp)) {
                AppIcon(Icons.Outlined.QueueMusic, contentDescription = "正在播放")
            }
            AppIconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                AppIcon(Icons.Filled.Close, contentDescription = "关闭听视频条")
            }
        }
    }
}

private fun Modifier.audioNowPlayingSkipGesture(
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
): Modifier = pointerInput(onSkipNext, onSkipPrevious) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragEnd = {
            if (abs(totalDrag) > 64f) {
                if (totalDrag < 0f) onSkipNext() else onSkipPrevious()
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f }
    ) { _, dragAmount ->
        totalDrag += dragAmount
    }
}
