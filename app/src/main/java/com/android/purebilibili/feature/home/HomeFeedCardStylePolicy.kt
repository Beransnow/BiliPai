package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle

internal data class HomeFeedCardLayout(
    val coverAspectRatio: Float,
    val outerPaddingDp: Int,
    val itemSpacingDp: Int,
    val verticalItemSpacingDp: Int = itemSpacingDp,
    val storyCardHorizontalPaddingDp: Int,
    val compactMetadata: Boolean
)

/**
 * 官方粉版双列首页封面框比例（偏高的横图框，约 4:3）。
 * 注意：投稿/CDN 源图多为 16:9；列表用 4:3 框 + 居中 Crop 时会裁左右，这是本家行为。
 */
internal const val HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO = 4f / 3f

/**
 * 「当前样式」更宽的封面框（16:9），与 CDN 源比例一致，标准封面几乎不裁。
 */
internal const val HOME_FEED_CURRENT_COVER_ASPECT_RATIO = 16f / 9f

internal fun resolveHomeFeedCardLayout(style: HomeFeedCardStyle): HomeFeedCardLayout {
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HomeFeedCardLayout(
            coverAspectRatio = HOME_FEED_CURRENT_COVER_ASPECT_RATIO,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            verticalItemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 16,
            compactMetadata = false
        )

        HomeFeedCardStyle.OFFICIAL -> HomeFeedCardLayout(
            // 对齐官方粉版双列：4:3 框 + 居中 Crop（见用户截图）
            coverAspectRatio = HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO,
            outerPaddingDp = 4,
            itemSpacingDp = 4,
            verticalItemSpacingDp = 6,
            storyCardHorizontalPaddingDp = 0,
            compactMetadata = true
        )
    }
}
