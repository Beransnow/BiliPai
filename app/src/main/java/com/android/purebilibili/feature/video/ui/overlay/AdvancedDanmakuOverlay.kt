package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.feature.video.danmaku.AdvancedDanmakuData
import com.android.purebilibili.feature.video.danmaku.DanmakuPlaybackClock
import kotlin.math.roundToInt

/**
 * 高级弹幕渲染层 (Compose 实现)
 * 
 * 负责渲染 Mode 7 (高级定位弹幕)。
 * 使用 BoxWithConstraints 获取屏幕尺寸，并根据弹幕的 startX/Y 和 progress 进行定位。
 * 
 * @param danmakuList 所有高级弹幕数据
 * @param currentPosition 当前视频播放进度 (毫秒)
 */
@Composable
internal fun AdvancedDanmakuOverlay(
    clock: DanmakuPlaybackClock,
    modifier: Modifier = Modifier
) {
    val positionProvider = remember(clock) { { clock.positionMs } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight

        clock.activeAdvancedItems.forEach { danmaku ->
            key(danmaku.id) {
                RenderSingleAdvancedDanmaku(
                    danmaku = danmaku,
                    positionProvider = positionProvider,
                    maxWidth = maxWidthPx,
                    maxHeight = maxHeightPx
                )
            }
        }
    }
}

@Composable
private fun RenderSingleAdvancedDanmaku(
    danmaku: AdvancedDanmakuData,
    positionProvider: () -> Long,
    maxWidth: Int,
    maxHeight: Int
) {
    // 如果是高能弹幕 (maxCount > 1)，需要动态计算显示的文字
    val displayText = if (danmaku.maxCount > 1) {
        // derivedStateOf only invalidates composition when the displayed integer changes.
        val displayCount by remember(danmaku, positionProvider) {
            derivedStateOf {
                resolveAdvancedDanmakuDisplayCount(
                    danmaku = danmaku,
                    currentPosition = positionProvider(),
                )
            }
        }
        "${danmaku.content} ×$displayCount"
    } else {
        danmaku.content
    }

    // 颜色转换
    val color = Color(danmaku.color or 0xFF000000.toInt())

    Box(
        modifier = Modifier
            .offset {
                val progress = danmaku.getProgress(positionProvider())
                val currentX = danmaku.startX + (danmaku.endX - danmaku.startX) * progress
                val currentY = danmaku.startY + (danmaku.endY - danmaku.startY) * progress
                IntOffset(
                    x = (currentX * maxWidth).roundToInt(),
                    y = (currentY * maxHeight).roundToInt(),
                )
            }
            // 使得 (x,y) 成为中心点，而不是左上角
            .offset(x = (-50).sp.value.dp.run { -this }, y = (-20).sp.value.dp.run { -this }) // 粗略修正，或者使用 alignment
            // 由于我们不知道具体 Text 大小，无法完美居中，除非使用 onGloballyPositioned
            // 简化处理：对于 mode 7 和高能弹幕，通常文本较长，我们这里假设其锚点就是左上角，或者我们改用 Alignment
            // 但 AdvancedDanmakuOverlay 使用的是 BoxWithConstraints + absolute offset
            // FIXME: 暂时保持左上角锚点，避免复杂布局变动
            .alpha(danmaku.alpha)
            .rotate(danmaku.rotateZ)
            .graphicsLayer {
                val scale = resolveAdvancedDanmakuScale(
                    danmaku = danmaku,
                    currentPosition = positionProvider(),
                )
                scaleX = scale
                scaleY = scale
            }
    ) {
        // 主文字
        Text(
            text = displayText,
            color = color,
            fontSize = danmaku.fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

internal fun resolveAdvancedDanmakuDisplayCount(
    danmaku: AdvancedDanmakuData,
    currentPosition: Long,
): Int {
    if (danmaku.maxCount <= 1) return danmaku.maxCount.coerceAtLeast(1)
    val accumulationDurationMs = danmaku.accumulationDurationMs
    if (accumulationDurationMs <= 0L) return danmaku.maxCount
    val elapsed = currentPosition - danmaku.startTimeMs
    if (elapsed >= accumulationDurationMs) return danmaku.maxCount
    val fraction = elapsed.toFloat() / accumulationDurationMs.toFloat()
    return (1 + (danmaku.maxCount - 1) * fraction).toInt()
}

internal fun resolveAdvancedDanmakuScale(
    danmaku: AdvancedDanmakuData,
    currentPosition: Long,
): Float {
    val isAccumulating = currentPosition < danmaku.startTimeMs + danmaku.accumulationDurationMs
    if (danmaku.maxCount <= 1 || !isAccumulating) return 1f
    val pulsePhase = (currentPosition % 300L) / 300f
    return if (pulsePhase < 0.5f) {
        1.0f + 0.3f * (pulsePhase * 2)
    } else {
        1.3f - 0.3f * ((pulsePhase - 0.5f) * 2)
    }
}
