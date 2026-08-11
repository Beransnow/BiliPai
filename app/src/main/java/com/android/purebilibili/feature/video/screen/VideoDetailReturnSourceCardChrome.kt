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
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceChromeVisualFrame
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceLayout
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewInfo

/**
 * Landing chrome geometry inside the Miuix flying detail entry.
 *
 * Dimensions ([infoWidthPx]/[infoHeightPx]) are in **screen pixels of the click-time card**.
 * Anchors ([anchorXInViewportPx]/[anchorYInViewportPx]) are in **full entry viewport space**
 * (card-local / sourceScale) so the inverse-scaled box lands on the measured info region after
 * the outer morph reaches the source card.
 */
internal data class VideoDetailReturnSourceCardLayout(
    val sourceScale: Float,
    val infoWidthPx: Float,
    val infoHeightPx: Float,
    val anchorXInViewportPx: Float,
    val anchorYInViewportPx: Float,
    val layout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
) {
    val canRender: Boolean
        get() = sourceScale > 0f &&
            infoWidthPx > 1f &&
            infoHeightPx > 1f &&
            layout != VideoCardSourceLayout.COVER_ONLY

    @Deprecated("Use infoWidthPx", ReplaceWith("infoWidthPx"))
    val sourceWidthPx: Float get() = infoWidthPx

    @Deprecated("Use infoHeightPx", ReplaceWith("infoHeightPx"))
    val sourceInfoHeightPx: Float get() = infoHeightPx
}

/** Text drawn while the flying card is still morphing (snapshot or live detail). */
internal data class VideoDetailReturnSourceCardChromeModel(
    val title: String,
    val ownerName: String,
    val viewText: String,
    val danmakuText: String,
    val durationText: String = "",
    val followed: Boolean = false,
)

/**
 * Prefer live detail once ready so stats can refresh; fall back to the click-time snapshot
 * while the destination is still Loading so the info region is never empty mid-morph.
 */
internal fun resolveVideoDetailReturnSourceCardChromeModel(
    info: ViewInfo?,
    snapshot: VideoCardSourceChromeSnapshot?,
): VideoDetailReturnSourceCardChromeModel? {
    if (info != null) {
        return VideoDetailReturnSourceCardChromeModel(
            title = info.title,
            ownerName = info.owner.name,
            viewText = FormatUtils.formatStat(info.stat.view.toLong()),
            danmakuText = FormatUtils.formatStat(info.stat.danmaku.toLong()),
            durationText = "",
            followed = false,
        )
    }
    val frozen = snapshot ?: return null
    return VideoDetailReturnSourceCardChromeModel(
        title = frozen.title,
        ownerName = frozen.ownerName,
        viewText = frozen.viewText,
        danmakuText = frozen.danmakuText,
        durationText = frozen.durationText,
        followed = frozen.followed,
    )
}

/**
 * 将全屏详情壳中的卡片正文反向补偿到点击时的源卡尺寸。
 *
 * - [VideoCardSourceLayout.STACKED]：正文在封面下方（首页双列/单列竖卡）。
 * - [VideoCardSourceLayout.SIDE_BY_SIDE]：正文在封面右侧（相关推荐横卡）。
 * - [VideoCardSourceLayout.COVER_ONLY]：无可独立信息区，不渲染 landing chrome。
 *
 * 锚点以**整页 entry 顶边/左边**为原点。调用方必须把 chrome 叠在全屏 entry 宿主上；
 * 若挂在播放器下方的 Column 子树里，会再偏移一整段媒体高度并被外层 clip 裁掉。
 */
internal fun resolveVideoDetailReturnSourceCardLayout(
    viewportWidthPx: Float,
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
    sourceLayout: VideoCardSourceLayout? = null,
): VideoDetailReturnSourceCardLayout {
    val viewportWidth = viewportWidthPx.coerceAtLeast(1f)
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f)
    val coverBounds = sourceCoverBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f)
    val layout = sourceLayout ?: resolveVideoCardSourceLayout(bounds, coverBounds)
    val sourceScale = (bounds.width / viewportWidth).coerceIn(0.01f, 1f)
    return when (layout) {
        VideoCardSourceLayout.STACKED -> {
            val horizontalTolerance = bounds.width * 0.1f
            val isFullWidthCover = coverBounds.left <= bounds.left + horizontalTolerance &&
                coverBounds.right >= bounds.right - horizontalTolerance
            val isVerticallyInsideCard = coverBounds.top >= bounds.top - 1f &&
                coverBounds.bottom in (bounds.top + 1f)..(bounds.bottom + 1f)
            if (!isFullWidthCover || !isVerticallyInsideCard) {
                return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f, layout)
            }
            val infoHeight = (bounds.bottom - coverBounds.bottom).coerceAtLeast(0f)
            if (infoHeight <= 1f) {
                return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f, layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                infoWidthPx = bounds.width,
                infoHeightPx = infoHeight,
                anchorXInViewportPx = 0f,
                anchorYInViewportPx = (coverBounds.bottom - bounds.top) / sourceScale,
                layout = layout,
            )
        }
        VideoCardSourceLayout.SIDE_BY_SIDE -> {
            val verticalTolerance = bounds.height * 0.1f
            val isFullHeightCover = coverBounds.top <= bounds.top + verticalTolerance &&
                coverBounds.bottom >= bounds.bottom - verticalTolerance
            val isHorizontallyInsideCard = coverBounds.left >= bounds.left - 1f &&
                coverBounds.right in (bounds.left + 1f)..(bounds.right + 1f)
            if (!isFullHeightCover || !isHorizontallyInsideCard) {
                // Related rows often have padding so cover is slightly inset; still treat as
                // side-by-side when cover is left-of-center and shorter than card width.
                val coverOnLeft = coverBounds.center.x <= bounds.center.x
                val coverNarrower = coverBounds.width < bounds.width * 0.75f
                if (!coverOnLeft || !coverNarrower) {
                    return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f, layout)
                }
            }
            val infoWidth = (bounds.right - coverBounds.right).coerceAtLeast(0f)
            val infoHeight = maxOf(coverBounds.height, bounds.height).coerceAtLeast(0f)
            if (infoWidth <= 1f || infoHeight <= 1f) {
                return VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f, layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                infoWidthPx = infoWidth,
                infoHeightPx = infoHeight,
                anchorXInViewportPx = (coverBounds.right - bounds.left) / sourceScale,
                anchorYInViewportPx = (coverBounds.top - bounds.top).coerceAtLeast(0f) / sourceScale,
                layout = layout,
            )
        }
        VideoCardSourceLayout.COVER_ONLY ->
            VideoDetailReturnSourceCardLayout(0f, 0f, 0f, 0f, 0f, layout)
    }
}

/**
 * 真正位于 Miuix 飞行详情 entry 内的来源卡正文。
 * 它不依赖底下已保留列表页的静态卡片，因此不会再出现封面飞走、文字留在原地的分层。
 *
 * 必须作为**全屏 entry 宿主**的直接子层（与播放器 Column 同级），使用
 * [resolveVideoDetailReturnSourceCardLayout] 给出的 viewport 锚点；不要嵌进播放器下方的
 * 信息区 Box。
 */
@Composable
internal fun VideoDetailReturnSourceCardChrome(
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
    morphDepthProgressProvider: () -> Float,
    modifier: Modifier = Modifier,
    sourceLayout: VideoCardSourceLayout? = null,
    chromeModel: VideoDetailReturnSourceCardChromeModel? = null,
    info: ViewInfo? = null,
    sourceChromeSnapshot: VideoCardSourceChromeSnapshot? = null,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.RETURNING
    },
    isReturnGestureInProgressProvider: () -> Boolean = { true },
) {
    val model = chromeModel
        ?: resolveVideoDetailReturnSourceCardChromeModel(info, sourceChromeSnapshot)
        ?: return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewportWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx()
    }
    val layout = resolveVideoDetailReturnSourceCardLayout(
        viewportWidthPx = viewportWidthPx,
        sourceBounds = sourceBounds,
        sourceCoverBounds = sourceCoverBounds,
        sourceLayout = sourceLayout,
    )
    if (!layout.canRender) return

    val infoWidth = with(density) { layout.infoWidthPx.toDp() }
    val infoHeight = with(density) { layout.infoHeightPx.toDp() }
    val anchorX = with(density) { layout.anchorXInViewportPx.toDp() }
    val anchorY = with(density) { layout.anchorYInViewportPx.toDp() }
    Column(
        modifier = modifier
            .offset(x = anchorX, y = anchorY)
            .width(infoWidth)
            .height(infoHeight)
            .graphicsLayer {
                val frame = resolveVideoCardSourceChromeVisualFrame(
                    morphDepthProgress = morphDepthProgressProvider(),
                    phase = phaseProvider(),
                    isReturnGestureInProgress = isReturnGestureInProgressProvider(),
                )
                // Resting inverse scale lands card-native type size after outer morph; mid-handoff
                // boost makes size meet the shrinking detail body instead of popping small text.
                val inverseScale = (1f / layout.sourceScale) * frame.layoutScaleMultiplier
                scaleX = inverseScale
                scaleY = inverseScale
                transformOrigin = TransformOrigin(0f, 0f)
                alpha = frame.alpha
            }
            .background(AppSurfaceTokens.cardContainer())
            .padding(
                horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                vertical = AppSpacingTokens.Small,
            ),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        AppText(
            text = model.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = buildString {
                append(model.ownerName)
                if (model.followed) {
                    append("  ·  已关注")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = buildString {
                append(model.viewText)
                append("播放  ·  ")
                append(model.danmakuText)
                append("弹幕")
                if (model.durationText.isNotBlank()) {
                    append("  ·  ")
                    append(model.durationText)
                } else if (info != null && info.pubdate > 0L) {
                    append("  ·  ")
                    append(FormatUtils.formatPublishTime(info.pubdate))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
