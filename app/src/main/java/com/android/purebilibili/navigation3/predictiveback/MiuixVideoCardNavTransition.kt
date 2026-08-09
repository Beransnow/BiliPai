package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

/**
 * Video-card morph authored directly against Miuix's shared navigation driver.
 *
 * The video entry is transformed from the click-time card rectangle to the navigation host. The
 * same [NavTransitionScope.relativeDepth] drives push, programmatic pop, predictive back, commit,
 * and cancellation, so there is no AndroidX Navigation3 or AnimatedVisibility compatibility path.
 */
internal fun miuixVideoCardNavTransition(
    sourceBounds: Rect?,
    sourceCornerDp: Int?,
    durationMillis: Int,
    fallback: NavTransition,
): NavTransition {
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return fallback
    val motion = NavMotion(
        commit = NavSettleSpec.Tween(
            durationMillis = durationMillis.coerceAtLeast(1),
            easing = FastOutExtraSlowIn,
        ),
        cancel = NavSettleSpec.Spring(stiffness = 1500f),
        programmatic = NavSettleSpec.Tween(
            durationMillis = durationMillis.coerceAtLeast(1),
            easing = FastOutExtraSlowIn,
        ),
    )
    val corner = sourceCornerDp?.coerceAtLeast(0) ?: 16

    return object : NavTransition {
        override val opaqueDepth: Float = fallback.opaqueDepth
        override val motion: NavMotion = motion

        override fun scrimFraction(scope: NavTransitionScope): Float =
            if (scope.relativeDepth > 0f) {
                fallback.scrimFraction(scope)
            } else {
                1f - topProgress(scope.relativeDepth)
            }

        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            if (scope.relativeDepth > 0f) {
                return with(fallback) { transformEntry(scope) }
            }
            return graphicsLayer {
                val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                val progress = topProgress(scope.relativeDepth)
                val sourceScaleX = (bounds.width / width).coerceIn(0.05f, 1f)
                val sourceScaleY = (bounds.height / height).coerceIn(0.05f, 1f)
                scaleX = sourceScaleX + (1f - sourceScaleX) * progress
                scaleY = sourceScaleY + (1f - sourceScaleY) * progress
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = bounds.left.coerceIn(-width, width) * (1f - progress)
                translationY = bounds.top.coerceIn(-height, height) * (1f - progress)
                alpha = (0.2f + 0.8f * progress).coerceIn(0f, 1f)
                clip = progress < 0.999f
                shape = RoundedCornerShape((corner * (1f - progress)).dp)
            }.zIndex(1f)
        }
    }
}
