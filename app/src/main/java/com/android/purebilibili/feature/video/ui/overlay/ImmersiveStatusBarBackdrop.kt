package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * 播放器顶部为系统状态栏预留的纯黑背景条，保证系统状态图标在视频画面上清晰可见。
 *
 * 早期版本会实时采样视频帧做毛玻璃模糊；为降低采样开销并保持视觉统一，
 * 现仅保留黑色背景，不再跟随视频画面变化。
 */
@Composable
internal fun ImmersiveStatusBarBackdrop(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    if (height.value <= 0f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Black),
    )
}
