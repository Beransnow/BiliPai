package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur

internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP = 10f
internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_CLEAR_TAIL_FRACTION = 0.08f
private const val BILIPAI_PROGRESSIVE_TOP_BLUR_MIN_EXTENSION_DP = 20f
private const val BILIPAI_PROGRESSIVE_TOP_BLUR_EXTRA_EXTENSION_DP = 28f
private val BiliPaiProgressiveTopBlurShape = RoundedCornerShape(
    bottomStart = 28.dp,
    bottomEnd = 28.dp,
)

internal fun shouldUseBiliPaiProgressiveTopBlur(
    enabled: Boolean,
    hasBackdrop: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean = enabled && hasBackdrop && sdkInt >= Build.VERSION_CODES.TIRAMISU

internal fun resolveProgressiveTopBlurBottomExtension(
    enabled: Boolean,
    endFraction: Float,
): Dp = if (enabled) {
    (
        BILIPAI_PROGRESSIVE_TOP_BLUR_MIN_EXTENSION_DP +
            endFraction.coerceIn(0f, 1f) * BILIPAI_PROGRESSIVE_TOP_BLUR_EXTRA_EXTENSION_DP
    ).dp
} else {
    0.dp
}

internal fun shouldExtendProgressiveTopBlurBelowTabs(
    progressiveBlurEnabled: Boolean,
    tabRowIncludedInBlur: Boolean,
): Boolean = progressiveBlurEnabled && !tabRowIncludedInBlur

/**
 * End the blur before the clipped container edge. Miuix already renders the clear end at native
 * resolution; keeping a short clear tail prevents the final antialiased shape row from reading as
 * a horizontal divider over scrolling content.
 */
internal fun resolveSeamlessProgressiveTopBlurGradient(
    gradient: ProgressiveBlur,
): ProgressiveBlur {
    val seamlessEndFraction = minOf(
        gradient.endFraction,
        1f - BILIPAI_PROGRESSIVE_TOP_BLUR_CLEAR_TAIL_FRACTION,
    )
    return if (seamlessEndFraction > gradient.startFraction) {
        gradient.copy(endFraction = seamlessEndFraction)
    } else {
        gradient
    }
}

/** Shared home-style edge blur for immersive floating top chrome. */
internal fun Modifier.biliPaiProgressiveTopBlur(
    backdrop: Backdrop?,
    enabled: Boolean,
    shape: Shape = BiliPaiProgressiveTopBlurShape,
    blurRadiusDp: Float = BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
): Modifier {
    if (
        !shouldUseBiliPaiProgressiveTopBlur(enabled, backdrop != null) ||
        blurRadiusDp <= 0.001f
    ) {
        return this
    }
    val source = requireNotNull(backdrop)
    return composed {
        if (isLowBlurBudgetForced()) return@composed this
        val seamlessGradient = remember(gradient) {
            resolveSeamlessProgressiveTopBlurGradient(gradient)
        }
        // The non-composable factory creates new shape/effect callbacks on each call.
        // Keep their identity while the material is unchanged, so an unrelated header
        // recomposition does not rebuild the progressive stack and its sharp-end effect.
        // Geometry changes and source redraws are still handled by Miuix's draw node.
        val effect = remember(source, shape, blurRadiusDp, seamlessGradient) {
            Modifier.progressiveTextureBlur(
                backdrop = source,
                shape = shape,
                blurRadius = blurRadiusDp,
                gradient = seamlessGradient,
            )
        }
        this.then(effect)
    }
}
