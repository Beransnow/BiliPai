package com.android.purebilibili.core.ui.transition

/**
 * Whether the retained source page is actually exposed by the card transition.
 *
 * This is deliberately separate from [VideoCardTransitionBackgroundPhase]: HELD keeps the
 * return contract alive, but the fully covered source page must not keep blur/composition work
 * alive while the user watches the detail page.
 */
internal enum class VideoCardTransitionExposure {
    Idle,
    Opening,
    SettledHidden,
    BackPreview,
    Returning,
    Restoring,
}

internal data class VideoCardTransitionRenderDecision(
    val retainSourceSnapshot: Boolean,
    val drawSourceNormally: Boolean,
    val drawTransitionBackground: Boolean,
    val updateBlurEffect: Boolean,
    val drawNavBackdrop: Boolean,
)

internal fun resolveVideoCardTransitionExposure(
    phase: VideoCardTransitionBackgroundPhase,
    predictiveBackInProgress: Boolean,
    gestureRestoreInProgress: Boolean,
): VideoCardTransitionExposure {
    return when (phase) {
        VideoCardTransitionBackgroundPhase.IDLE -> VideoCardTransitionExposure.Idle
        VideoCardTransitionBackgroundPhase.OPENING -> VideoCardTransitionExposure.Opening
        VideoCardTransitionBackgroundPhase.RETURNING -> VideoCardTransitionExposure.Returning
        VideoCardTransitionBackgroundPhase.HELD -> when {
            predictiveBackInProgress -> VideoCardTransitionExposure.BackPreview
            gestureRestoreInProgress -> VideoCardTransitionExposure.Restoring
            else -> VideoCardTransitionExposure.SettledHidden
        }
    }
}

internal fun resolveVideoCardTransitionRenderDecision(
    exposure: VideoCardTransitionExposure,
): VideoCardTransitionRenderDecision {
    return when (exposure) {
        VideoCardTransitionExposure.Idle -> VideoCardTransitionRenderDecision(
            retainSourceSnapshot = false,
            drawSourceNormally = true,
            drawTransitionBackground = false,
            updateBlurEffect = false,
            drawNavBackdrop = false,
        )
        VideoCardTransitionExposure.SettledHidden -> VideoCardTransitionRenderDecision(
            retainSourceSnapshot = true,
            drawSourceNormally = false,
            drawTransitionBackground = false,
            updateBlurEffect = false,
            drawNavBackdrop = false,
        )
        VideoCardTransitionExposure.Restoring -> VideoCardTransitionRenderDecision(
            retainSourceSnapshot = true,
            drawSourceNormally = false,
            drawTransitionBackground = true,
            updateBlurEffect = true,
            drawNavBackdrop = false,
        )
        VideoCardTransitionExposure.Opening,
        VideoCardTransitionExposure.BackPreview,
        VideoCardTransitionExposure.Returning -> VideoCardTransitionRenderDecision(
            retainSourceSnapshot = true,
            drawSourceNormally = false,
            drawTransitionBackground = true,
            updateBlurEffect = true,
            drawNavBackdrop = true,
        )
    }
}
