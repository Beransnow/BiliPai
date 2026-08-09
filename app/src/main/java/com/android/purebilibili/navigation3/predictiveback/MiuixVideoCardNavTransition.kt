package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

internal enum class MiuixVideoCardContentScale {
    FillWidthTop,
    CropCenter,
}

internal data class MiuixVideoCardContentCompensation(
    val scaleX: Float,
    val scaleY: Float,
    val transformOrigin: TransformOrigin,
)

internal fun resolveMiuixVideoCardContentCompensation(
    outerScaleX: Float,
    outerScaleY: Float,
    contentScale: MiuixVideoCardContentScale,
): MiuixVideoCardContentCompensation {
    val safeOuterScaleX = outerScaleX.coerceAtLeast(0.01f)
    val safeOuterScaleY = outerScaleY.coerceAtLeast(0.01f)
    val uniformScale = when (contentScale) {
        MiuixVideoCardContentScale.FillWidthTop -> safeOuterScaleX
        MiuixVideoCardContentScale.CropCenter -> maxOf(safeOuterScaleX, safeOuterScaleY)
    }
    return MiuixVideoCardContentCompensation(
        scaleX = uniformScale / safeOuterScaleX,
        scaleY = uniformScale / safeOuterScaleY,
        transformOrigin = when (contentScale) {
            MiuixVideoCardContentScale.FillWidthTop -> TransformOrigin(0.5f, 0f)
            MiuixVideoCardContentScale.CropCenter -> TransformOrigin.Center
        },
    )
}

/** Deferred bridge to the top video entry's live Miuix driver. */
internal class MiuixVideoCardTransitionProgress {
    private var topScope: NavTransitionScope? = null

    fun bind(scope: NavTransitionScope) {
        when (scope.role) {
            NavRole.Incoming,
            NavRole.Outgoing,
            -> topScope = scope
            NavRole.Top -> if (topScope == null || topScope?.role == NavRole.Covered) {
                topScope = scope
            }
            NavRole.Covered -> Unit
        }
    }

    fun depthOr(fallback: Float): Float = topScope
        ?.let { topProgress(it.relativeDepth) }
        ?: fallback.coerceIn(0f, 1f)

    fun isGestureInProgress(): Boolean = topScope?.gesture != null
}

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
    progress: MiuixVideoCardTransitionProgress,
    contentScale: MiuixVideoCardContentScale = MiuixVideoCardContentScale.FillWidthTop,
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

        // Source-page scrim and blur are rendered by the existing depth layer from this same
        // transition's deferred progress. Do not add Miuix's generic dim on top of it.
        override fun scrimFraction(scope: NavTransitionScope): Float = 0f

        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            progress.bind(scope)
            return graphicsLayer {
                val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                val depth = scope.relativeDepth
                if (depth <= 0f) {
                    val morph = topProgress(depth)
                    val sourceScaleX = (bounds.width / width).coerceIn(0.05f, 1f)
                    val sourceScaleY = (bounds.height / height).coerceIn(0.05f, 1f)
                    scaleX = sourceScaleX + (1f - sourceScaleX) * morph
                    scaleY = sourceScaleY + (1f - sourceScaleY) * morph
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = bounds.left.coerceIn(-width, width) * (1f - morph)
                    translationY = bounds.top.coerceIn(-height, height) * (1f - morph)
                    // The old sharedBounds path used Enter/Exit.None: the live card/player stays
                    // fully opaque while its bounds move. Fading the whole detail page changed the
                    // transition into a generic zoom and exposed the source beneath it.
                    alpha = 1f
                    clip = morph < 0.999f
                    shape = RoundedCornerShape((corner * (1f - morph)).dp)
                }
            }.graphicsLayer {
                val depth = scope.relativeDepth
                if (depth <= 0f) {
                    val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                    val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                    val morph = topProgress(depth)
                    val outerScaleX = (bounds.width / width).coerceIn(0.05f, 1f) +
                        (1f - (bounds.width / width).coerceIn(0.05f, 1f)) * morph
                    val outerScaleY = (bounds.height / height).coerceIn(0.05f, 1f) +
                        (1f - (bounds.height / height).coerceIn(0.05f, 1f)) * morph
                    val compensation = resolveMiuixVideoCardContentCompensation(
                        outerScaleX = outerScaleX,
                        outerScaleY = outerScaleY,
                        contentScale = contentScale,
                    )
                    scaleX = compensation.scaleX
                    scaleY = compensation.scaleY
                    transformOrigin = compensation.transformOrigin
                }
            }.zIndex(1f)
        }
    }
}
