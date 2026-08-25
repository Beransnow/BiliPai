package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur

private val BiliPaiTopChromeGradient = ProgressiveBlur(
    angle = 90f,
    startFraction = 0.08f,
    endFraction = 0.92f,
    curve = 0.72f,
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
    shape: Shape = RectangleShape,
    blurRadiusDp: Float = 24f,
): Modifier {
    if (!shouldUseBiliPaiProgressiveTopBlur(enabled, backdrop != null)) return this
    return progressiveTextureBlur(
        backdrop = requireNotNull(backdrop),
        shape = shape,
        blurRadius = blurRadiusDp,
        gradient = BiliPaiTopChromeGradient,
    )
}
