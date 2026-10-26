package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

/** Measure actions first; each capsule scrolls inside its own non-overlapping edge slot. */
@Composable
internal fun PlayerBottomControlGroups(
    modifier: Modifier = Modifier,
    playback: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(
        content = {
            playback()
            actions()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = PlayerChromeLayout.ActionGapDp.dp.roundToPx().coerceAtMost(constraints.maxWidth)
        val rightMaxWidth = resolveBottomRightGroupMaxWidthPx(
            availableWidthPx = constraints.maxWidth,
            groupGapPx = gap,
            targetSizePx = PlayerChromeLayout.CircularButtonSizeDp.dp.roundToPx(),
        )
        val right = measurables[1].measure(constraints.copy(minWidth = 0, maxWidth = rightMaxWidth))
        val left = measurables[0].measure(
            constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - right.width - gap).coerceAtLeast(0)),
        )
        val height = maxOf(left.height, right.height)
        layout(constraints.maxWidth, height) {
            left.placeRelative(0, (height - left.height) / 2)
            right.placeRelative(constraints.maxWidth - right.width, (height - right.height) / 2)
        }
    }
}

