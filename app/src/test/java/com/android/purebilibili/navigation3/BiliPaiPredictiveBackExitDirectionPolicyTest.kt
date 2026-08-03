package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackExitDirectionSign
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiAutoPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackExitDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class BiliPaiPredictiveBackExitDirectionPolicyTest {

    @Test
    fun autoDerived_sharedElementRoute_followsGesture() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE, direction)
    }

    @Test
    fun autoDerived_cardFromLeft_stillFollowsGesture() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE, direction)
    }

    @Test
    fun autoDerived_cardFromRight_stillFollowsGesture() {
        val direction = resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT,
        )
        assertEquals(BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE, direction)
    }

    @Test
    fun storageAuto_followsGesture() {
        val autoDerived = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        assertEquals(
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE,
            resolveBiliPaiPredictiveBackExitDirection("auto", autoDerived),
        )
    }

    @Test
    fun legacyStorageOverride_doesNotOverridePhysicalGestureDirection() {
        val autoDerived = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        assertEquals(
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE,
            resolveBiliPaiPredictiveBackExitDirection("always_left", autoDerived),
        )
        assertEquals(
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE,
            resolveBiliPaiPredictiveBackExitDirection("follow_gesture", autoDerived),
        )
    }

    @Test
    fun swipeEdge_resolvesPhysicalExitDirection() {
        assertEquals(
            1,
            resolveBiliPaiPredictiveBackExitDirectionSign(androidx.navigationevent.NavigationEvent.EDGE_LEFT),
        )
        assertEquals(
            -1,
            resolveBiliPaiPredictiveBackExitDirectionSign(androidx.navigationevent.NavigationEvent.EDGE_RIGHT),
        )
        assertEquals(
            1,
            resolveBiliPaiPredictiveBackExitDirectionSign(androidx.navigationevent.NavigationEvent.EDGE_NONE),
        )
    }
}
