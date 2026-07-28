package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.android.purebilibili.navigation.resolveBottomPagerNavigationDurationMillis

internal fun resolveBottomBarLikeTransitionMillis(
    animationEnabled: Boolean,
    reduceMotion: Boolean,
    pageDistance: Int = 1
): Int {
    if (!animationEnabled || reduceMotion) return 0
    return resolveBottomPagerNavigationDurationMillis(pageDistance = pageDistance)
}

internal fun resolveBottomBarLikeHorizontalContentTransform(
    durationMillis: Int,
    forward: Boolean
): ContentTransform {
    if (durationMillis <= 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    // 页面切换只需要空间位移，使用临界阻尼 spring 可更快收敛，且不会产生
    // 额外的长尾帧；预测返回路径不经过这里，仍由专用 tween 保证可 seek。
    val spec = navigationSlideSpring(durationMillis)
    return if (forward) {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = spec
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = spec
        )
    } else {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth },
            animationSpec = spec
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = spec
        )
    }
}
