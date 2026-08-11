package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.transition.LocalMiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceChromeVisualFrame
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceLayout
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewInfo

/**
 * Landing geometry for reconstructing the source card inside the Miuix flying entry.
 *
 * All sizes are **click-time screen pixels of the source card**. After inverse scale
 * `1/sourceScale` and the outer morph, the resting frame must match the stationary list card:
 * same width/height, cover flush on info, one continuous [AppSurfaceTokens.cardContainer] shell.
 */
internal data class VideoDetailReturnSourceCardLayout(
    val sourceScale: Float,
    /** Full card width (screen px). */
    val cardWidthPx: Float,
    /** Full card height (screen px). */
    val cardHeightPx: Float,
    /** Cover band height within the card (screen px). */
    val coverHeightPx: Float,
    /** Info band width (screen px); full card width for STACKED. */
    val infoWidthPx: Float,
    /** Info band height (screen px). */
    val infoHeightPx: Float,
    /** Card top-left X in entry viewport space (pre-inverse). */
    val cardAnchorXInViewportPx: Float,
    /** Card top-left Y in entry viewport space (pre-inverse). */
    val cardAnchorYInViewportPx: Float,
    /** Info band top-left relative to entry (pre-inverse). */
    val infoAnchorXInViewportPx: Float,
    val infoAnchorYInViewportPx: Float,
    val layout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
) {
    val canRender: Boolean
        get() = sourceScale > 0f &&
            cardWidthPx > 1f &&
            cardHeightPx > 1f &&
            infoWidthPx > 1f &&
            infoHeightPx > 1f &&
            layout != VideoCardSourceLayout.COVER_ONLY

    @Deprecated("Use infoWidthPx", ReplaceWith("infoWidthPx"))
    val sourceWidthPx: Float get() = infoWidthPx

    @Deprecated("Use infoHeightPx", ReplaceWith("infoHeightPx"))
    val sourceInfoHeightPx: Float get() = infoHeightPx

    @Deprecated("Use infoAnchorYInViewportPx", ReplaceWith("infoAnchorYInViewportPx"))
    val anchorYInViewportPx: Float get() = infoAnchorYInViewportPx

    @Deprecated("Use infoAnchorXInViewportPx", ReplaceWith("infoAnchorXInViewportPx"))
    val anchorXInViewportPx: Float get() = infoAnchorXInViewportPx
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

private fun emptyLayout(
    layout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
) = VideoDetailReturnSourceCardLayout(
    sourceScale = 0f,
    cardWidthPx = 0f,
    cardHeightPx = 0f,
    coverHeightPx = 0f,
    infoWidthPx = 0f,
    infoHeightPx = 0f,
    cardAnchorXInViewportPx = 0f,
    cardAnchorYInViewportPx = 0f,
    infoAnchorXInViewportPx = 0f,
    infoAnchorYInViewportPx = 0f,
    layout = layout,
)

/**
 * 将全屏详情壳中的卡片几何反向补偿到点击时的源卡尺寸。
 *
 * 落位（morph depth → 0）后必须与列表静止卡一致：整卡宽高、封面/信息分区、
 * 同一 [AppSurfaceTokens.cardContainer] 底壳，封面与信息区无缝。
 */
internal fun resolveVideoDetailReturnSourceCardLayout(
    viewportWidthPx: Float,
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
    sourceLayout: VideoCardSourceLayout? = null,
): VideoDetailReturnSourceCardLayout {
    val viewportWidth = viewportWidthPx.coerceAtLeast(1f)
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return emptyLayout()
    val coverBounds = sourceCoverBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return emptyLayout()
    val layout = sourceLayout ?: resolveVideoCardSourceLayout(bounds, coverBounds)
    val sourceScale = (bounds.width / viewportWidth).coerceIn(0.01f, 1f)
    // Entry (0,0) maps to card top-left after outer morph; anchors are card-local / sourceScale.
    val cardAnchorX = 0f
    val cardAnchorY = 0f
    return when (layout) {
        VideoCardSourceLayout.STACKED -> {
            val horizontalTolerance = bounds.width * 0.1f
            val isFullWidthCover = coverBounds.left <= bounds.left + horizontalTolerance &&
                coverBounds.right >= bounds.right - horizontalTolerance
            val isVerticallyInsideCard = coverBounds.top >= bounds.top - 1f &&
                coverBounds.bottom in (bounds.top + 1f)..(bounds.bottom + 1f)
            if (!isFullWidthCover || !isVerticallyInsideCard) {
                return emptyLayout(layout)
            }
            val coverHeight = (coverBounds.bottom - bounds.top).coerceAtLeast(0f)
            val infoHeight = (bounds.bottom - coverBounds.bottom).coerceAtLeast(0f)
            if (infoHeight <= 1f || coverHeight <= 1f) {
                return emptyLayout(layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                cardWidthPx = bounds.width,
                cardHeightPx = bounds.height,
                coverHeightPx = coverHeight,
                infoWidthPx = bounds.width,
                infoHeightPx = infoHeight,
                cardAnchorXInViewportPx = cardAnchorX,
                cardAnchorYInViewportPx = cardAnchorY,
                infoAnchorXInViewportPx = 0f,
                // Flush under cover: same junction as stationary home card (cover bottom sharp).
                infoAnchorYInViewportPx = coverHeight / sourceScale,
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
                val coverOnLeft = coverBounds.center.x <= bounds.center.x
                val coverNarrower = coverBounds.width < bounds.width * 0.75f
                if (!coverOnLeft || !coverNarrower) {
                    return emptyLayout(layout)
                }
            }
            val coverWidth = (coverBounds.right - bounds.left).coerceAtLeast(0f)
            val infoWidth = (bounds.right - coverBounds.right).coerceAtLeast(0f)
            val infoHeight = bounds.height.coerceAtLeast(0f)
            if (infoWidth <= 1f || infoHeight <= 1f) {
                return emptyLayout(layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                cardWidthPx = bounds.width,
                cardHeightPx = bounds.height,
                coverHeightPx = bounds.height,
                infoWidthPx = infoWidth,
                infoHeightPx = infoHeight,
                cardAnchorXInViewportPx = cardAnchorX,
                cardAnchorYInViewportPx = cardAnchorY,
                infoAnchorXInViewportPx = coverWidth / sourceScale,
                infoAnchorYInViewportPx = 0f,
                layout = layout,
            )
        }
        VideoCardSourceLayout.COVER_ONLY -> emptyLayout(layout)
    }
}

/**
 * Entry-space height of the cover band so the player can be clipped flush with the info shell.
 * Matches stationary home: cover bottom is sharp against the card surface.
 */
internal fun resolveVideoDetailReturnCoverHeightInEntryPx(
    layout: VideoDetailReturnSourceCardLayout,
): Float {
    if (!layout.canRender) return 0f
    // Layout coverHeight is screen px of the card; inverse scale expands to entry space.
    return layout.coverHeightPx / layout.sourceScale
}

/**
 * 飞行详情内的来源卡落位层：整卡底壳（在播放器下）+ 信息文案（在播放器上）。
 *
 * 静止列表卡结构对齐：
 * - 一张 [AppSurfaceTokens.cardContainer] 整卡底
 * - 封面叠在上半，底边直角贴信息区（非第二块独立圆角板）
 * - 落位 inverse scale **恒为 1/sourceScale**，禁止交接放大破坏整卡比例
 */
@Composable
internal fun BoxScope.VideoDetailReturnSourceCardChrome(
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
    val miuixHost = LocalMiuixVideoCardTransitionState.current
    // Must match outer morph: bounds.width / layoutSize.width (not Configuration screen width).
    val viewportWidthPx = miuixHost.layoutWidthProvider().takeIf { it > 1f }
        ?: with(density) { configuration.screenWidthDp.dp.toPx() }
    val layout = resolveVideoDetailReturnSourceCardLayout(
        viewportWidthPx = viewportWidthPx,
        sourceBounds = sourceBounds,
        sourceCoverBounds = sourceCoverBounds,
        sourceLayout = sourceLayout,
    )
    if (!layout.canRender) return

    val cardWidth = with(density) { layout.cardWidthPx.toDp() }
    val cardHeight = with(density) { layout.cardHeightPx.toDp() }
    val cardAnchorX = with(density) { layout.cardAnchorXInViewportPx.toDp() }
    val cardAnchorY = with(density) { layout.cardAnchorYInViewportPx.toDp() }
    val infoWidth = with(density) { layout.infoWidthPx.toDp() }
    val infoHeight = with(density) { layout.infoHeightPx.toDp() }
    val infoAnchorX = with(density) { layout.infoAnchorXInViewportPx.toDp() }
    val infoAnchorY = with(density) { layout.infoAnchorYInViewportPx.toDp() }
    val inverseScale = 1f / layout.sourceScale

    fun Modifier.landingLayer(): Modifier = graphicsLayer {
        val frame = resolveVideoCardSourceChromeVisualFrame(
            morphDepthProgress = morphDepthProgressProvider(),
            phase = phaseProvider(),
            isReturnGestureInProgress = isReturnGestureInProgressProvider(),
        )
        // Pure inverse scale — land frame must equal stationary list card after outer morph.
        scaleX = inverseScale * frame.layoutScaleMultiplier
        scaleY = inverseScale * frame.layoutScaleMultiplier
        transformOrigin = TransformOrigin(0f, 0f)
        alpha = frame.alpha
    }

    // 1) Full-card shell behind the player. Cover band shares this surface under the media;
    //    info band is the visible face — one continuous plate (home VideoCard shell).
    Box(
        modifier = modifier
            .zIndex(-1f)
            .align(Alignment.TopStart)
            .offset(x = cardAnchorX, y = cardAnchorY)
            .width(cardWidth)
            .height(cardHeight)
            .landingLayer()
            .background(AppSurfaceTokens.cardContainer()),
    )

    // 2) Info typography on the shell (no second background plate).
    Column(
        modifier = modifier
            .zIndex(1f)
            .align(Alignment.TopStart)
            .offset(x = infoAnchorX, y = infoAnchorY)
            .width(infoWidth)
            .height(infoHeight)
            .landingLayer()
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
