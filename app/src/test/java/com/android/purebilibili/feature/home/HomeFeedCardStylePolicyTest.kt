package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedCardStylePolicyTest {

    @Test
    fun currentStyle_usesSixteenByNineAndExistingSpacing() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.CURRENT)

        assertEquals(HOME_FEED_CURRENT_COVER_ASPECT_RATIO, layout.coverAspectRatio, 0.0001f)
        assertEquals(16f / 9f, layout.coverAspectRatio, 0.0001f)
        assertEquals(8, layout.outerPaddingDp)
        assertEquals(8, layout.itemSpacingDp)
        assertEquals(8, layout.verticalItemSpacingDp)
        assertEquals(16, layout.storyCardHorizontalPaddingDp)
        assertEquals(false, layout.compactMetadata)
    }

    @Test
    fun officialStyle_usesFourByThreeLikeOfficialDualColumnFeed() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.OFFICIAL)

        // 粉版双列列表框 4:3 + 居中 Crop（CDN 源 16:9 会裁左右，与本家截图一致）
        assertEquals(HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO, layout.coverAspectRatio, 0.0001f)
        assertEquals(4f / 3f, layout.coverAspectRatio, 0.0001f)
        assertEquals(4, layout.outerPaddingDp)
        assertEquals(4, layout.itemSpacingDp)
        assertEquals(6, layout.verticalItemSpacingDp)
        assertEquals(0, layout.storyCardHorizontalPaddingDp)
        assertEquals(true, layout.compactMetadata)
    }
}
