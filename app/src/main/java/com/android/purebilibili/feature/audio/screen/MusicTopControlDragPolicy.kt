package com.android.purebilibili.feature.audio.screen

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.sign

@Immutable
internal data class MusicTopControlTransform(
    val scaleX: Float,
    val scaleY: Float,
    val pivotFractionX: Float,
    val pivotFractionY: Float,
)

/** Converts drag distance into directional deformation without changing the control's layout position. */
internal fun resolveMusicTopControlTransform(
    dragX: Float,
    dragY: Float,
    maxDragPx: Float,
    glassProgress: Float,
): MusicTopControlTransform {
    val safeMaxDragPx = maxDragPx.coerceAtLeast(1f)
    val clampedX = dragX.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val clampedY = dragY.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val horizontalPull = abs(clampedX) / safeMaxDragPx
    val verticalPull = abs(clampedY) / safeMaxDragPx
    val liveliness = 1f - glassProgress.coerceIn(0f, 1f)
    val stretch = 0.18f + 0.06f * liveliness
    val squeeze = 0.05f + 0.02f * liveliness
    val pivotShift = 0.12f + 0.08f * liveliness
    return MusicTopControlTransform(
        scaleX = 1f + horizontalPull * stretch - verticalPull * squeeze,
        scaleY = 1f + verticalPull * stretch - horizontalPull * squeeze,
        pivotFractionX = 0.5f - sign(clampedX) * horizontalPull * pivotShift,
        pivotFractionY = 0.5f - sign(clampedY) * verticalPull * pivotShift,
    )
}
