package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.graphics.TransformOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class MiuixVideoCardNavTransitionTest {
    @Test
    fun cardClipKeepsPhysicalCornerRadiusAcrossNonUniformScale() {
        val radii = resolveMiuixVideoCardClipRadii(
            sourceCornerPx = 12f,
            morphProgress = 0f,
            outerScaleX = 0.5f,
            outerScaleY = 0.25f,
        )

        assertEquals(12f, radii.radiusX * 0.5f, absoluteTolerance = 0.0001f)
        assertEquals(12f, radii.radiusY * 0.25f, absoluteTolerance = 0.0001f)
    }

    @Test
    fun fillWidthTopPreservesAspectRatioAndTopAlignment() {
        val compensation = resolveMiuixVideoCardContentCompensation(
            outerScaleX = 0.5f,
            outerScaleY = 0.25f,
            contentScale = MiuixVideoCardContentScale.FillWidthTop,
        )

        assertEquals(0.5f, 0.5f * compensation.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(0.5f, 0.25f * compensation.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(TransformOrigin(0.5f, 0f), compensation.transformOrigin)
    }

    @Test
    fun cropCenterPreservesAspectRatioUsingCoverScale() {
        val compensation = resolveMiuixVideoCardContentCompensation(
            outerScaleX = 0.35f,
            outerScaleY = 0.6f,
            contentScale = MiuixVideoCardContentScale.CropCenter,
        )

        assertEquals(0.6f, 0.35f * compensation.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(0.6f, 0.6f * compensation.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(TransformOrigin.Center, compensation.transformOrigin)
    }
}
