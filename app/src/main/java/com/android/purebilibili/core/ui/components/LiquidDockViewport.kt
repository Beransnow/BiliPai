package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Frame the viewport, not the scrolling rail. Clipping alone removes the offscreen
 * rail endcaps and leaves an open-looking edge when the content overflows.
 * Place before horizontalScroll (or on its parent) so the frame never scrolls.
 */
@Composable
internal fun Modifier.liquidDockViewport(): Modifier = this
    .clip(CircleShape)
    .border(
        width = 0.75.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        shape = CircleShape,
    )
