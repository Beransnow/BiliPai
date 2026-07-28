package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

internal enum class ImagePreviewMotionPhase {
    CLOSED,
    OPENING,
    OPEN,
    VERTICAL_DRAG,
    PREDICTIVE_BACK,
    DISMISSING,
    DISMISSED
}

private enum class ImagePreviewBackOutcome {
    NONE,
    CANCELLED,
    COMPLETED
}

/**
 * One owner for the preview's opening, vertical-drag, predictive-back and dismiss motion.
 *
 * Continuous values are intentionally exposed as pull reads. Callers sample them from layout,
 * layer, draw or snapshotFlow blocks so the preview subtree is not recomposed for every frame.
 * Phase changes are low-frequency and may be observed from composition.
 */
@Stable
internal class ImagePreviewMotionState {
    private val transitionProgress = Animatable(0f)
    private val verticalSnap = Animatable(0f)

    private var verticalDragOffsetYPx by mutableFloatStateOf(0f)
    private var backOutcome = ImagePreviewBackOutcome.NONE
    private var dismissCallbackCommitted = false

    var phase by mutableStateOf(ImagePreviewMotionPhase.CLOSED)
        private set

    var dismissStartRect by mutableStateOf<Rect?>(null)
        private set

    val isDismissing: Boolean
        get() = phase == ImagePreviewMotionPhase.DISMISSING ||
            phase == ImagePreviewMotionPhase.DISMISSED

    val isVerticalDragging: Boolean
        get() = phase == ImagePreviewMotionPhase.VERTICAL_DRAG

    fun readTransitionProgress(): Float = transitionProgress.value

    fun readVerticalDragOffsetYPx(): Float = verticalDragOffsetYPx

    suspend fun open(animationSpec: AnimationSpec<Float>) {
        if (phase != ImagePreviewMotionPhase.CLOSED) return
        phase = ImagePreviewMotionPhase.OPENING
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(1f, animationSpec)
        if (phase == ImagePreviewMotionPhase.OPENING) {
            phase = ImagePreviewMotionPhase.OPEN
        }
    }

    suspend fun resetForPageChange() {
        if (isDismissing) return
        verticalSnap.stop()
        verticalDragOffsetYPx = 0f
        if (phase == ImagePreviewMotionPhase.VERTICAL_DRAG) {
            phase = ImagePreviewMotionPhase.OPEN
        }
    }

    fun beginVerticalDrag(): Boolean {
        if (phase != ImagePreviewMotionPhase.OPEN) return false
        verticalDragOffsetYPx = 0f
        phase = ImagePreviewMotionPhase.VERTICAL_DRAG
        return true
    }

    suspend fun stopVerticalSnap() {
        verticalSnap.stop()
    }

    fun dragVerticallyBy(deltaPx: Float): Boolean {
        if (phase != ImagePreviewMotionPhase.VERTICAL_DRAG) return false
        verticalDragOffsetYPx += deltaPx
        return true
    }

    fun finishVerticalDrag(): Float? {
        if (phase != ImagePreviewMotionPhase.VERTICAL_DRAG) return null
        phase = ImagePreviewMotionPhase.OPEN
        return verticalDragOffsetYPx
    }

    suspend fun snapVerticalDragBack(animationSpec: AnimationSpec<Float>) {
        val startOffset = verticalDragOffsetYPx
        verticalSnap.snapTo(startOffset)
        verticalSnap.animateTo(0f, animationSpec) {
            verticalDragOffsetYPx = value
        }
        verticalDragOffsetYPx = 0f
    }

    suspend fun updatePredictiveBack(progress: Float): Boolean {
        if (isDismissing || phase == ImagePreviewMotionPhase.DISMISSED) return false
        if (phase != ImagePreviewMotionPhase.PREDICTIVE_BACK) {
            backOutcome = ImagePreviewBackOutcome.NONE
            phase = ImagePreviewMotionPhase.PREDICTIVE_BACK
        }
        transitionProgress.snapTo(1f - progress.coerceIn(0f, 1f))
        return true
    }

    suspend fun cancelBack(animationSpec: AnimationSpec<Float>): Boolean {
        if (backOutcome != ImagePreviewBackOutcome.NONE || isDismissing) return false
        backOutcome = ImagePreviewBackOutcome.CANCELLED
        transitionProgress.animateTo(1f, animationSpec)
        phase = ImagePreviewMotionPhase.OPEN
        return true
    }

    fun completeBack(): Boolean {
        if (backOutcome != ImagePreviewBackOutcome.NONE || isDismissing) return false
        backOutcome = ImagePreviewBackOutcome.COMPLETED
        return true
    }

    fun beginDismiss(startRect: Rect?): Boolean {
        if (isDismissing || phase == ImagePreviewMotionPhase.DISMISSED) return false
        dismissStartRect = startRect
        verticalDragOffsetYPx = 0f
        phase = ImagePreviewMotionPhase.DISMISSING
        return true
    }

    suspend fun animateDismiss(animationSpec: AnimationSpec<Float>) {
        if (phase != ImagePreviewMotionPhase.DISMISSING) return
        verticalSnap.stop()
        verticalDragOffsetYPx = 0f
        transitionProgress.animateTo(0f, animationSpec)
    }

    /** Returns true exactly once for the owner that must invoke the external dismiss callback. */
    fun commitDismiss(): Boolean {
        if (phase != ImagePreviewMotionPhase.DISMISSING || dismissCallbackCommitted) return false
        dismissCallbackCommitted = true
        phase = ImagePreviewMotionPhase.DISMISSED
        return true
    }
}
