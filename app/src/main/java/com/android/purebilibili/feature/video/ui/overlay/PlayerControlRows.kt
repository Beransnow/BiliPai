package com.android.purebilibili.feature.video.ui.overlay


import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt

enum class PlayerControlAnchor { QUALITY, SPEED, SUBTITLE, RATIO }

internal fun playerBottomControlsHeightDp(widthDp: Int): Int {
    val controls = resolvePlayerBottomControlBarLayoutPolicy(widthDp)
    val progress = resolveVideoProgressBarLayoutPolicy(widthDp)
    // Reserve the chapter and action rows regardless of their vertical order.
    return controls.progressSpacingDp + progress.baseHeightWithChapterDp + controls.playButtonSizeDp
}

internal fun Modifier.playerActionBounds(
    clipBounds: Boolean = true,
    onBounds: (IntRect) -> Unit,
): Modifier =
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow(clipBounds = clipBounds)
        if (bounds.width > 0f && bounds.height > 0f) {
            onBounds(IntRect(
                bounds.left.roundToInt(), bounds.top.roundToInt(),
                bounds.right.roundToInt(), bounds.bottom.roundToInt(),
            ))
        }
    }

