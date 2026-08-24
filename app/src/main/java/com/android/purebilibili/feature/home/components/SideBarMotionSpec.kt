package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val NavigationSelectionScale = 1.1f
internal const val NavigationSelectionWobbleDegrees = 4f
internal const val NavigationSelectionCounterWobbleDegrees = -3f
internal const val NavigationIndicatorSettleScaleX = 1.06f
internal const val NavigationIndicatorSettleScaleY = 0.97f
internal const val NavigationIndicatorSettleTranslationDp = 3f

internal fun <T> navigationSelectionScaleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.72f,
    stiffness = 420f,
)

internal fun <T> navigationSelectionWobbleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.62f,
    stiffness = 720f,
)

@Immutable
internal data class NavigationSelectionTransform(
    val scale: () -> Float,
    val rotationDegrees: () -> Float,
)

@Composable
internal fun rememberNavigationSelectionTransform(
    selected: Boolean,
    label: String,
): NavigationSelectionTransform {
    var wobbleTarget by remember { mutableFloatStateOf(0f) }
    var hasObservedSelection by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (selected) NavigationSelectionScale else 1f,
        animationSpec = navigationSelectionScaleMotionSpec(),
        label = "${label}_selection_scale",
    )
    val rotation = animateFloatAsState(
        targetValue = wobbleTarget,
        animationSpec = navigationSelectionWobbleMotionSpec(),
        label = "${label}_selection_wobble",
    )

    LaunchedEffect(selected) {
        if (hasObservedSelection && selected) {
            wobbleTarget = NavigationSelectionWobbleDegrees
            delay(45)
            wobbleTarget = NavigationSelectionCounterWobbleDegrees
            delay(45)
        }
        wobbleTarget = 0f
        hasObservedSelection = true
    }
    return remember(scale, rotation) {
        NavigationSelectionTransform(
            scale = { scale.value },
            rotationDegrees = { rotation.value },
        )
    }
}

@Immutable
internal data class NavigationIndicatorSettleTransform(
    val scaleX: () -> Float,
    val scaleY: () -> Float,
    val translationXDp: () -> Float,
)

@Composable
internal fun rememberNavigationIndicatorSettleTransform(
    pulseKey: Int,
    direction: Float,
): NavigationIndicatorSettleTransform {
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val translationXDp = remember { Animatable(0f) }

    LaunchedEffect(pulseKey) {
        if (pulseKey <= 0) return@LaunchedEffect
        scaleX.snapTo(1f)
        scaleY.snapTo(1f)
        translationXDp.snapTo(0f)
        coroutineScope {
            launch {
                scaleX.animateTo(
                    NavigationIndicatorSettleScaleX,
                    navigationSelectionScaleMotionSpec(),
                )
                scaleX.animateTo(1f, navigationSelectionScaleMotionSpec())
            }
            launch {
                scaleY.animateTo(
                    NavigationIndicatorSettleScaleY,
                    navigationSelectionScaleMotionSpec(),
                )
                scaleY.animateTo(1f, navigationSelectionScaleMotionSpec())
            }
            launch {
                translationXDp.animateTo(
                    direction.coerceIn(-1f, 1f) * NavigationIndicatorSettleTranslationDp,
                    navigationSelectionWobbleMotionSpec(),
                )
                translationXDp.animateTo(0f, navigationSelectionWobbleMotionSpec())
            }
        }
    }
    return NavigationIndicatorSettleTransform(
        scaleX = { scaleX.value },
        scaleY = { scaleY.value },
        translationXDp = { translationXDp.value },
    )
}
