package com.android.purebilibili.core.ui.animation

import androidx.compose.runtime.Stable

/**
 * Stable, pull-based access to frame-rate motion values.
 *
 * Callers should sample these functions from a layout, layer, draw, pointer-input, or
 * [androidx.compose.runtime.snapshotFlow] block. Passing the reader across a composable boundary
 * keeps the changing value out of the parent composition.
 */
@Stable
internal interface MotionReader {
    fun readPosition(): Float

    fun readPressProgress(): Float

    fun readDragOffsetPx(): Float

    fun readVelocityPxPerSecond(): Float

    fun readDeformationVelocityItemsPerSecond(): Float

    fun readDragging(): Boolean
}
