package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.android.purebilibili.core.util.CardPositionManager

/**
 * Records the stationary list card into a graphics layer so a click can freeze native pixels
 * instead of reconstructing title/spacing on the flying detail entry.
 */
@Composable
internal fun Modifier.recordNativeVideoCardLayer(
    layer: androidx.compose.ui.graphics.layer.GraphicsLayer,
): Modifier = drawWithContent {
    layer.record {
        this@drawWithContent.drawContent()
    }
    drawContent()
}

internal fun captureNativeVideoCardImage(
    layer: androidx.compose.ui.graphics.layer.GraphicsLayer,
) {
    runCatching {
        CardPositionManager.recordNativeCardImage(layer.toImageBitmap())
    }
}

@Composable
internal fun rememberNativeVideoCardLayer() = rememberGraphicsLayer()
