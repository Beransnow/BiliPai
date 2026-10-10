/*
 * Motion adapted from Kyant's AndroidLiquidGlass LiquidButton / InteractiveHighlight.
 * Copyright 2025 Kyant. Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Modified for BiliPai: keep page-safe backdrop rendering separate, and use the
 * visible button size when its touch target is larger than the glass surface.
 */
package com.android.purebilibili.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private object ElasticPressMarker : Modifier.Element

/** A moving capsule can suppress duplicate deformation on its child click targets. */
val LocalElasticPressEnabled = compositionLocalOf { true }

/** Shared gesture state; glass renderers use [layerBlock] to inverse-transform backdrop sampling. */
class AppElasticPressState internal constructor(
    internal val motionEnabled: Boolean,
    private val visibleSizePx: Float?,
    private val expansionPx: Float,
) {
    internal val press = Animatable(0f, visibilityThreshold = 0.001f)
    internal val position = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    internal var startPosition by mutableStateOf(Offset.Zero)

    val layerBlock: GraphicsLayerScope.() -> Unit = {
        if (motionEnabled) {
            val width = visibleSizePx ?: size.width
            val height = visibleSizePx ?: size.height
            if (width > 0f && height > 0f) {
                val minDimension = minOf(width, height)
                val maxDimension = maxOf(width, height)
                val offset = position.value - startPosition
                val baseScale = 1f + expansionPx / height * press.value
                val dragScale = expansionPx / height
                val angle = atan2(offset.y, offset.x)
                translationX = minDimension * tanh(0.05f * offset.x / minDimension)
                translationY = minDimension * tanh(0.05f * offset.y / minDimension)
                scaleX = baseScale + dragScale * abs(cos(angle) * offset.x / maxDimension) *
                    (width / height).coerceAtMost(1f)
                scaleY = baseScale + dragScale * abs(sin(angle) * offset.y / maxDimension) *
                    (height / width).coerceAtMost(1f)
            }
        }
    }
}

@Composable
fun rememberAppElasticPressState(visualSize: Dp? = null): AppElasticPressState {
    val density = LocalDensity.current
    val motionEnabled = LocalComponentMotionEnabled.current
    val visibleSizePx = visualSize?.let { with(density) { it.toPx() } }
    val expansionPx = with(density) { 4.dp.toPx() }
    return remember(motionEnabled, visibleSizePx, expansionPx) {
        AppElasticPressState(motionEnabled, visibleSizePx, expansionPx)
    }
}

/**
 * Kyant's elastic button motion. Does not add clicks, consume gestures, or sample backgrounds.
 * Wraps the complete modifier chain so existing surfaces and clips deform too; native clicks and long
 * presses keep ownership. Parent scrolling cancels the feedback. Disabled controls and the
 * app's reduced-motion setting opt out. Repeated applications on one chain do not stack.
 * [visualSize] overrides geometry for a square surface inside a larger touch target.
 * With [transformInBackdrop], callers must pass [state] and render its layerBlock in drawBackdrop.
 */
fun Modifier.appElasticPress(
    enabled: Boolean = true,
    visualSize: Dp? = null,
    dragEnabled: Boolean = true,
    state: AppElasticPressState? = null,
    transformInBackdrop: Boolean = false,
): Modifier {
    if (foldIn(false) { found, element -> found || element === ElasticPressMarker }) return this
    return Modifier.then(ElasticPressMarker).composed {
        if (!enabled || !LocalComponentMotionEnabled.current || !LocalElasticPressEnabled.current) return@composed this
        val elasticState = state ?: rememberAppElasticPressState(visualSize)
        val press = elasticState.press
        val position = elasticState.position
        val scope = rememberCoroutineScope()
        var pressJob by remember { mutableStateOf<Job?>(null) }
        var positionJob by remember { mutableStateOf<Job?>(null) }

        fun release() {
            pressJob?.cancel()
            positionJob?.cancel()
            pressJob = scope.launch {
                press.animateTo(0f, spring(0.5f, 300f, 0.001f))
            }
            positionJob = scope.launch {
                position.animateTo(elasticState.startPosition, spring(0.5f, 300f, Offset.VisibilityThreshold))
            }
        }

        this
            .pointerInput(elasticState, dragEnabled) {
                // Pointer-down starts feedback; Final-pass observation gives native business
                // gestures first refusal. Multi-touch cancels feedback instead of changing fingers.
                awaitEachGesture {
                    val down = awaitFirstDown(false, PointerEventPass.Initial)
                    pressJob?.cancel()
                    positionJob?.cancel()
                    elasticState.startPosition = down.position
                    pressJob = scope.launch {
                        press.animateTo(1f, spring(0.5f, 300f, 0.001f))
                    }
                    positionJob = scope.launch { position.snapTo(down.position) }
                    val pointerId = down.id
                    try {
                        while (true) {
                            // Observe after click/scroll/zoom recognizers, never compete with them.
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.count { it.pressed } > 1) break
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (change.position != change.previousPosition) {
                                if (change.isConsumed) break
                                if (dragEnabled) {
                                    positionJob?.cancel()
                                    positionJob = scope.launch { position.snapTo(change.position) }
                                }
                            }
                        }
                    } finally {
                        release()
                    }
                }
            }
            .then(
                // drawBackdrop must own the transform so its sampling can undo it.
                // An ancestor graphicsLayer only stretches a previously rendered glass texture.
                if (transformInBackdrop) Modifier else Modifier.graphicsLayer(elasticState.layerBlock)
            )
    }.then(this)
}
