package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

internal fun <T> materialBottomBarSelectionScaleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.72f,
    stiffness = 420f,
)

internal fun <T> materialBottomBarIndicatorWobbleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.62f,
    stiffness = 720f,
)
