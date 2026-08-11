package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceChromeReturnAlpha
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewInfo

internal data class VideoDetailReturnSourceCardLayout(
    val sourceScale: Float,
    val sourceWidthPx: Float,
    val sourceInfoHeightPx: Float,
    val anchorYInViewportPx: Float,
) {
    val canRender: Boolean
        get() = sourceScale > 0f && sourceWidthPx > 1f && sourceInfoHeightPx > 1f
}

/**
 * 将全屏详情壳中的卡片正文反向补偿到点击时的源卡尺寸。
 *
 * Miuix 外层落位时会把整页统一缩小到 [sourceBounds]；正文内层反向缩放后，
 * 标题字号和间距在落点仍保持与列表卡一致，而它的顶部接在点击时记录的真实封面底边。
 */
internal fun resolveVideoDetailReturnSourceCardLayout(
    viewportWidthPx: Float,
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
): VideoDetailReturnSourceCardLayout {
    val viewportWidth = viewportWidthPx.coerceAtLeast(1f)
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f)
    val coverBounds = sourceCoverBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f)
    val horizontalTolerance = bounds.width * 0.1f
    val isFullWidthCover = coverBounds.left <= bounds.left + horizontalTolerance &&
        coverBounds.right >= bounds.right - horizontalTolerance
    val isVerticallyInsideCard = coverBounds.top >= bounds.top - 1f &&
        coverBounds.bottom in (bounds.top + 1f)..(bounds.bottom + 1f)
    if (!isFullWidthCover || !isVerticallyInsideCard) {
        return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f)
    }
    val sourceScale = (bounds.width / viewportWidth).coerceIn(0.01f, 1f)
    return VideoDetailReturnSourceCardLayout(
        sourceScale = sourceScale,
        sourceWidthPx = bounds.width,
        sourceInfoHeightPx = (bounds.bottom - coverBounds.bottom).coerceAtLeast(0f),
        anchorYInViewportPx = (coverBounds.bottom - bounds.top) / sourceScale,
    )
}

/**
 * 真正位于 Miuix 飞行详情 entry 内的来源卡正文。
 * 它不依赖底下已保留列表页的静态卡片，因此不会再出现封面飞走、文字留在原地的分层。
 */
@Composable
internal fun VideoDetailReturnSourceCardChrome(
    info: ViewInfo,
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
    morphDepthProgressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewportWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx()
    }
    val layout = resolveVideoDetailReturnSourceCardLayout(
        viewportWidthPx = viewportWidthPx,
        sourceBounds = sourceBounds,
        sourceCoverBounds = sourceCoverBounds,
    )
    if (!layout.canRender) return

    val sourceWidth = with(density) { layout.sourceWidthPx.toDp() }
    val sourceInfoHeight = with(density) { layout.sourceInfoHeightPx.toDp() }
    val anchorY = with(density) { layout.anchorYInViewportPx.toDp() }
    Column(
        modifier = modifier
            .offset(y = anchorY)
            .width(sourceWidth)
            .height(sourceInfoHeight)
            .graphicsLayer {
                val inverseScale = 1f / layout.sourceScale
                scaleX = inverseScale
                scaleY = inverseScale
                transformOrigin = TransformOrigin(0f, 0f)
                alpha = resolveVideoCardSourceChromeReturnAlpha(
                    morphDepthProgress = morphDepthProgressProvider(),
                )
            }
            .background(AppSurfaceTokens.cardContainer())
            .padding(
                horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                vertical = AppSpacingTokens.Small,
            ),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        AppText(
            text = info.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = info.owner.name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = buildString {
                append(FormatUtils.formatStat(info.stat.view.toLong()))
                append("播放  ·  ")
                append(FormatUtils.formatStat(info.stat.danmaku.toLong()))
                append("弹幕  ·  ")
                append(FormatUtils.formatPublishTime(info.pubdate))
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
