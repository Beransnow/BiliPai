package com.android.purebilibili.core.ui

import kotlin.math.roundToInt

/** Home floating bottom-bar shell height. Other chrome scales from this. */
const val BottomBarReferenceShellHeightDp = 64f

/** Home floating bottom-bar rest indicator height. */
const val BottomBarReferenceIndicatorHeightDp = 56f

/** Home floating bottom-bar pressed / drag height. */
const val BottomBarReferencePressedHeightDp = 78f

/** HyperIsland liquid indicator press bloom: 56dp -> 78dp. */
const val BottomBarReferencePressedScale = BottomBarReferencePressedHeightDp / 56f

data class MatchedLiquidIndicatorGeometry(
    val dockHeightDp: Float,
    val indicatorHeightDp: Float,
    val pressedScale: Float,
) {
    val pressedHeightDp: Float get() = indicatorHeightDp * pressedScale
}

fun resolveMatchedLiquidIndicatorHeightDp(dockHeightDp: Float): Float {
    if (dockHeightDp <= 0f) return 0f
    return dockHeightDp *
        (BottomBarReferenceIndicatorHeightDp / BottomBarReferenceShellHeightDp)
}

fun roundMatchedLiquidIndicatorHeightDp(dockHeightDp: Float): Int {
    return resolveMatchedLiquidIndicatorHeightDp(dockHeightDp)
        .roundToInt()
        .coerceAtLeast(1)
}

fun resolveMatchedLiquidIndicatorPressedScale(
    dockHeightDp: Float,
    indicatorHeightDp: Float,
): Float {
    if (dockHeightDp <= 0f || indicatorHeightDp <= 0f) return 1f
    // Keep HyperIsland's 64/56/78 geometry even when a text-heavy or narrow global control
    // has to fit a shorter resting pill: the pressed pill must still reach 78/64 of its dock.
    val pressedHeight = dockHeightDp *
        (BottomBarReferencePressedHeightDp / BottomBarReferenceShellHeightDp)
    return (pressedHeight / indicatorHeightDp).coerceAtLeast(1f)
}

fun resolveMatchedLiquidIndicatorGeometry(
    dockHeightDp: Float,
    indicatorHeightDp: Float = resolveMatchedLiquidIndicatorHeightDp(dockHeightDp),
): MatchedLiquidIndicatorGeometry {
    val restHeight = if (indicatorHeightDp > 0f) {
        indicatorHeightDp
    } else {
        resolveMatchedLiquidIndicatorHeightDp(dockHeightDp)
    }
    return MatchedLiquidIndicatorGeometry(
        dockHeightDp = dockHeightDp,
        indicatorHeightDp = restHeight,
        pressedScale = resolveMatchedLiquidIndicatorPressedScale(
            dockHeightDp = dockHeightDp,
            indicatorHeightDp = restHeight,
        ),
    )
}
