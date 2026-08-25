package com.android.purebilibili.feature.audio.screen

import androidx.compose.runtime.Immutable
import kotlin.math.abs

@Immutable
internal data class MusicTopControlTransform(
    val scaleX: Float,
    val scaleY: Float,
)

/** Converts drag distance into anchored deformation; the control never leaves its layout position. */
internal fun resolveMusicTopControlTransform(
    dragX: Float,
    dragY: Float,
    maxDragPx: Float,
): MusicTopControlTransform {
    val safeMaxDragPx = maxDragPx.coerceAtLeast(1f)
    val clampedX = dragX.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val clampedY = dragY.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val horizontalPull = abs(clampedX) / safeMaxDragPx
    val verticalPull = abs(clampedY) / safeMaxDragPx
    return MusicTopControlTransform(
        scaleX = 1f + horizontalPull * 0.18f - verticalPull * 0.06f,
        scaleY = 1f + verticalPull * 0.18f - horizontalPull * 0.06f,
    )
}
