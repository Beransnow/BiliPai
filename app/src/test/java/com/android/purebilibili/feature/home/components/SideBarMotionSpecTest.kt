package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SideBarMotionSpecTest {
    @Test
    fun materialBottomBarMotion_keepsIndicatorCompanionMotionRestrained() {
        val selection = materialBottomBarSelectionScaleMotionSpec<Float>()
        val wobble = materialBottomBarIndicatorWobbleMotionSpec<Float>()

        assertEquals(0.72f, selection.dampingRatio)
        assertEquals(420f, selection.stiffness)
        assertEquals(0.62f, wobble.dampingRatio)
        assertEquals(720f, wobble.stiffness)
    }
}
