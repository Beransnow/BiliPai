package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardNativeSnapshotPolicyTest {
    @Test
    fun stationarySourceCardIsEmptyWhileTheFlyingCardOwnsTheSlot() {
        assertTrue(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                depthProgress = 0.2f,
                isReturnGestureInProgress = false,
            ),
        )
        assertTrue(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                depthProgress = 1f,
                isReturnGestureInProgress = false,
            ),
        )
        assertTrue(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                depthProgress = 0.4f,
                isReturnGestureInProgress = true,
            ),
        )
        assertTrue(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                depthProgress = 0.5f,
                isReturnGestureInProgress = false,
            ),
        )
    }

    @Test
    fun stationarySourceCardReturnsAfterLandAndNeverHidesUnrelatedCards() {
        assertFalse(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.IDLE,
                depthProgress = 0f,
                isReturnGestureInProgress = false,
            ),
        )
        assertFalse(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                depthProgress = 0f,
                isReturnGestureInProgress = false,
            ),
        )
        assertFalse(
            shouldHideStationarySourceCard(
                isSharedMorphSourceCard = false,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                depthProgress = 0.2f,
                isReturnGestureInProgress = false,
            ),
        )
    }
}
