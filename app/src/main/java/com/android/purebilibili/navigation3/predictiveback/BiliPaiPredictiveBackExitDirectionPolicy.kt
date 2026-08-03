package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavCardSourceDirection
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

internal fun resolveBiliPaiAutoPredictiveBackExitDirection(
    @Suppress("UNUSED_PARAMETER") popRouteTransition: BiliPaiNavRouteTransition,
    @Suppress("UNUSED_PARAMETER") cardSourceDirection: BiliPaiNavCardSourceDirection,
): BiliPaiPredictiveBackExitDirection {
    return BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
}

internal fun resolveBiliPaiPredictiveBackExitDirection(
    @Suppress("UNUSED_PARAMETER") storageValue: String?,
    @Suppress("UNUSED_PARAMETER") autoDerived: BiliPaiPredictiveBackExitDirection,
): BiliPaiPredictiveBackExitDirection {
    return BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
}
