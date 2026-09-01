// SPDX-License-Identifier: GPL-3.0-only
package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

internal const val MIUIX_CARD_BACK_SETTLE_DURATION_MILLIS = 300

private const val CARD_BACK_MIN_SCALE = 0.9f
private const val CARD_BACK_MAX_TRANSLATION_FRACTION = 0.125f
private val CardBackMaxCornerRadius = 20.dp
private val CardBackEasing = CubicBezierEasing(0.12f, 0.38f, 0.2f, 1f)

internal data class MiuixCardBackFrame(
    val scale: Float,
    val translationX: Float,
    val cornerRadiusPx: Float,
)

internal fun resolveMiuixCardBackFrame(
    progress: Float,
    widthPx: Float,
    cornerRadiusPx: Float,
    exitDirectionSign: Float,
): MiuixCardBackFrame {
    val safeProgress = progress.coerceIn(0f, 1f)
    val safeWidth = widthPx.coerceAtLeast(0f)
    return MiuixCardBackFrame(
        scale = 1f - (1f - CARD_BACK_MIN_SCALE) * safeProgress,
        translationX = exitDirectionSign.coerceIn(-1f, 1f) *
            safeWidth * CARD_BACK_MAX_TRANSLATION_FRACTION * safeProgress,
        cornerRadiusPx = cornerRadiusPx.coerceAtLeast(0f) * safeProgress,
    )
}

internal fun resolveMiuixCardBackExitDirectionSign(
    swipeEdge: NavSwipeEdge?,
    layoutDirection: LayoutDirection,
): Float = when (swipeEdge) {
    NavSwipeEdge.Left -> 1f
    NavSwipeEdge.Right -> -1f
    NavSwipeEdge.None,
    null,
    -> if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
}

internal fun miuixCardBackNavTransition(): NavTransition {
    val motion = NavMotion(
        commit = NavSettleSpec.Tween(
            durationMillis = MIUIX_CARD_BACK_SETTLE_DURATION_MILLIS,
            easing = CardBackEasing,
        ),
        cancel = NavSettleSpec.Spring(stiffness = 1500f),
        programmatic = NavSettleSpec.Tween(
            durationMillis = MIUIX_CARD_BACK_SETTLE_DURATION_MILLIS,
            easing = CardBackEasing,
        ),
    )
    val pop = navGraphicsTransition(
        opaqueDepth = 1f,
        motion = motion,
        scrim = { scope ->
            1f - resolveCardBackProgress(scope)
        },
    ) { scope ->
        val progress = resolveCardBackProgress(scope)
        val widthPx = scope.layoutSize.width.toFloat()
        if (scope.relativeDepth <= 0f) {
            val frame = resolveMiuixCardBackFrame(
                progress = progress,
                widthPx = widthPx,
                cornerRadiusPx = with(scope.density) { CardBackMaxCornerRadius.toPx() },
                exitDirectionSign = resolveMiuixCardBackExitDirectionSign(
                    swipeEdge = scope.gesture?.swipeEdge,
                    layoutDirection = scope.layoutDirection,
                ),
            )
            scaleX = snapScaleToPixelExtent(frame.scale, widthPx)
            scaleY = scaleX
            translationX = snapTranslationToPixelEdge(
                translation = frame.translationX,
                scale = scaleX,
                extent = widthPx,
            )
            clip = progress > 0.001f
            shape = MiuixCardBackClipShape(
                radiusPx = frame.cornerRadiusPx / scaleX.coerceAtLeast(0.01f),
            )
        } else {
            val coveredDepth = 1f - progress
            val direction = if (scope.layoutDirection == LayoutDirection.Rtl) 1f else -1f
            translationX = direction * coveredDepth * widthPx * 0.25f
            alpha = 1f - 0.1f * coveredDepth
        }
    }
    return navDirectionalTransition(
        push = NavTransitions.MiuixDefault,
        pop = pop,
        predictivePop = pop,
    )
}

private fun resolveCardBackProgress(scope: NavTransitionScope): Float =
    (if (scope.relativeDepth <= 0f) {
        1f - topProgress(scope.relativeDepth)
    } else {
        1f - coverProgress(scope.relativeDepth)
    }).coerceIn(0f, 1f)

private data class MiuixCardBackClipShape(
    val radiusPx: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Rounded(
        RoundRect(
            rect = Rect(0f, 0f, size.width, size.height),
            cornerRadius = radiusPx.coerceIn(0f, minOf(size.width, size.height) / 2f)
                .let { radius -> CornerRadius(x = radius, y = radius) },
        ),
    )
}
