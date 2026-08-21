package com.android.purebilibili.feature.home.components

import kotlin.math.max
import kotlin.math.min

/** Home dock resting indicator is 56dp in a typically ~75dp slot (~1.35). */
internal const val FLOATING_DOCK_MIN_INDICATOR_ASPECT = 1.35f

internal const val FLOATING_DOCK_PREDICTIVE_BACK_EDGE_DP = 24f

internal const val FLOATING_DOCK_REFERENCE_SHELL_HEIGHT_DP = 64f

internal const val FLOATING_DOCK_SHELL_LENS_DP = 24f

internal const val FLOATING_DOCK_PRESS_BLOOM_DP = 16f

internal const val FLOATING_DOCK_INDICATOR_LENS_HEIGHT_DP = 10f

internal const val FLOATING_DOCK_INDICATOR_LENS_AMOUNT_DP = 14f

internal const val FLOATING_DOCK_INNER_SHADOW_RADIUS_DP = 8f

internal const val FLOATING_DOCK_TAB_PRESS_SCALE_EXTRA = 0.2f

/**
 * Short chrome (search 36dp, top tabs ~40dp) cannot use the home dock's 24dp lens:
 * top and bottom refraction meet in the middle as a black shrimp line.
 * Scale lens with shell height so a 64dp dock stays full strength.
 */
internal fun resolveCompactDockShellLensIntensity(
    shellHeightDp: Float,
    referenceShellHeightDp: Float = FLOATING_DOCK_REFERENCE_SHELL_HEIGHT_DP,
): Float {
    if (shellHeightDp <= 0f || referenceShellHeightDp <= 0f) return 0f
    return (shellHeightDp / referenceShellHeightDp).coerceIn(0f, 1f)
}

internal fun resolveCompactDockLensDp(shellHeightDp: Float): Float =
    FLOATING_DOCK_SHELL_LENS_DP * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveCompactDockPressBloomDp(shellHeightDp: Float): Float =
    FLOATING_DOCK_PRESS_BLOOM_DP * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveCompactDockIndicatorLensHeightDp(shellHeightDp: Float): Float =
    FLOATING_DOCK_INDICATOR_LENS_HEIGHT_DP * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveCompactDockIndicatorLensAmountDp(shellHeightDp: Float): Float =
    FLOATING_DOCK_INDICATOR_LENS_AMOUNT_DP * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveCompactDockInnerShadowRadiusDp(shellHeightDp: Float): Float =
    FLOATING_DOCK_INNER_SHADOW_RADIUS_DP * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveCompactDockTabPressScale(shellHeightDp: Float): Float =
    1f + FLOATING_DOCK_TAB_PRESS_SCALE_EXTRA * resolveCompactDockShellLensIntensity(shellHeightDp)

internal fun resolveFloatingDockIndicatorHeightDp(
    requestedHeightDp: Float,
    tabWidthDp: Float,
): Float {
    if (requestedHeightDp <= 0f) return 0f
    if (tabWidthDp <= 0f) return requestedHeightDp
    val maxHeightForCapsule = tabWidthDp / FLOATING_DOCK_MIN_INDICATOR_ASPECT
    return min(requestedHeightDp, maxHeightForCapsule)
}

internal fun resolveFloatingDockDragEdgeInsetPx(
    systemInsetPx: Float,
    fallbackPx: Float,
): Float = max(systemInsetPx, fallbackPx)

internal fun shouldAcceptFloatingDockDragAtWindowX(
    windowX: Float,
    screenWidthPx: Float,
    leftInsetPx: Float,
    rightInsetPx: Float,
): Boolean {
    if (screenWidthPx <= 0f) return true
    return windowX >= leftInsetPx && windowX <= screenWidthPx - rightInsetPx
}
