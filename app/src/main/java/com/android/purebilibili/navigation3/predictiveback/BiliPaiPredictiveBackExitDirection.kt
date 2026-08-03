package com.android.purebilibili.navigation3.predictiveback

import androidx.navigationevent.NavigationEvent

internal enum class BiliPaiPredictiveBackExitDirection {
    FOLLOW_GESTURE,
    ALWAYS_RIGHT,
    ALWAYS_LEFT,
}

internal fun resolveBiliPaiPredictiveBackExitDirectionSign(
    @NavigationEvent.SwipeEdge swipeEdge: Int,
): Int = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1

internal fun resolveBiliPaiPredictiveBackSlideOffsetX(
    @NavigationEvent.SwipeEdge swipeEdge: Int,
): (Int) -> Int {
    val directionSign = resolveBiliPaiPredictiveBackExitDirectionSign(swipeEdge)
    return { width -> directionSign * width }
}
