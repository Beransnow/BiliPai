package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur

internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP = 10f
private val BiliPaiProgressiveTopBlurShape = RoundedCornerShape(
    bottomStart = 28.dp,
    bottomEnd = 28.dp,
)

internal fun shouldUseBiliPaiProgressiveTopBlur(
    enabled: Boolean,
    hasBackdrop: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean = enabled && hasBackdrop && sdkInt >= Build.VERSION_CODES.TIRAMISU

/** Shared home-style edge blur for immersive floating top chrome. */
internal fun Modifier.biliPaiProgressiveTopBlur(
    backdrop: Backdrop?,
    enabled: Boolean,
    shape: Shape = BiliPaiProgressiveTopBlurShape,
    blurRadiusDp: Float = BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP,
): Modifier {
    if (!shouldUseBiliPaiProgressiveTopBlur(enabled, backdrop != null)) return this
    return progressiveTextureBlur(
        backdrop = requireNotNull(backdrop),
        shape = shape,
        blurRadius = blurRadiusDp,
        gradient = ProgressiveBlur.Top,
    )
}
