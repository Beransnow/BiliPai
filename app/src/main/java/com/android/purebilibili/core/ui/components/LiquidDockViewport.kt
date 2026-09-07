package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * Frame the viewport to a pill dock shape so the ends never expose sharp corners
 * while the inner rail scrolls horizontally.
 */
@Composable
internal fun Modifier.liquidDockViewport(): Modifier = this.clip(CircleShape)
