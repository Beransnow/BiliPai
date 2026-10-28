package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot

class DetailReturnPolicyTest {

    @Test
    fun quickReturn_whenBackWithinThreshold_returnsTrue() {
        assertTrue(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_420L
            )
        )
    }

    @Test
    fun slowReturn_whenBackAfterThreshold_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_650L
            )
        )
    }

    @Test
    fun invalidTimeline_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 2_000L,
                detailReturnUptimeMs = 1_900L
            )
        )
    }

    @Test
    fun clear_resetsOnlyCardGeometryFallback() {
        CardPositionManager.recordCardPosition(
            bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
            screenWidth = 200f,
            screenHeight = 200f
        )
        assertTrue(CardPositionManager.lastClickedCardBounds != null)
        assertTrue(CardPositionManager.lastClickedCardCenter != null)

        CardPositionManager.clear()

        assertNull(CardPositionManager.lastClickedCardBounds)
        assertNull(CardPositionManager.lastClickedCardCenter)
        assertNull(CardPositionManager.lastClickedNativeCardLayer)
        assertNull(CardPositionManager.lastClickedNativeCardBitmap)
        assertNull(CardPositionManager.lastClickedNativeCoverOverlayLayer)
    }

    @Test
    fun recordVideoCardPosition_preservesHomeCategorySourceKey() {
        CardPositionManager.recordVideoCardPosition(
            bvid = "BV1xx411c7mD",
            sourceRoute = "home?category=POPULAR",
            bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
            screenWidth = 200f,
            screenHeight = 200f
        )

        assertEquals(
            "home?category=POPULAR:BV1xx411c7mD",
            CardPositionManager.lastClickedVideoSourceKey
        )
    }

    @Test
    fun laterClickCannotSupplyChromeToEarlierSessionEvenAfterSourceRestoration() {
        fun click(bvid: String, title: String) {
            CardPositionManager.recordVideoCardPosition(
                bvid = bvid,
                sourceRoute = "home",
                bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
                screenWidth = 200f,
                screenHeight = 200f,
                sourceChromeSnapshot = VideoCardSourceChromeSnapshot(title = title, ownerName = "UP"),
            )
        }
        try {
            click("BV_A", "第一张")
            val firstOwner = requireNotNull(CardPositionManager.lastClickedVideoPixelOwner)
            assertEquals("第一张", CardPositionManager.sourceChromeSnapshotFor(firstOwner)?.title)

            click("BV_B", "第二张")
            val secondOwner = requireNotNull(CardPositionManager.lastClickedVideoPixelOwner)
            assertFalse(CardPositionManager.isCurrentVideoCardClick(firstOwner))
            assertTrue(CardPositionManager.isCurrentVideoCardClick(secondOwner))
            assertNull(CardPositionManager.sourceChromeSnapshotFor(firstOwner))
            CardPositionManager.restoreVideoSource("home:BV_A", firstOwner)
            assertNull(CardPositionManager.sourceChromeSnapshotFor(firstOwner))
            assertEquals("第二张", CardPositionManager.sourceChromeSnapshotFor(secondOwner)?.title)
        } finally {
            CardPositionManager.clear()
        }
    }
}
