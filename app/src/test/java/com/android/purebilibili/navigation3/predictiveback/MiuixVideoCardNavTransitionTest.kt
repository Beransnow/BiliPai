package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.graphics.TransformOrigin
import top.yukonga.miuix.kmp.nav.transition.NavRole
import kotlin.test.Test
import kotlin.test.assertEquals

class MiuixVideoCardNavTransitionTest {
    @Test
    fun predictiveSeekIsAReturnEvenWhileMiuixReportsIncomingRole() {
        assertEquals(
            true,
            isMiuixVideoCardReturning(
                role = NavRole.Incoming,
                hasGesture = true,
            ),
        )
        assertEquals(
            false,
            isMiuixVideoCardReturning(
                role = NavRole.Incoming,
                hasGesture = false,
            ),
        )
        assertEquals(
            true,
            isMiuixVideoCardReturning(
                role = NavRole.Outgoing,
                hasGesture = false,
            ),
        )
    }

    @Test
    fun returnDepthClearsBlurInsteadOfReversingIt() {
        assertEquals(
            1f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = 0f),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0.5f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = -0.5f),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = -1f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun cardClipKeepsPhysicalCornerRadiusAcrossNonUniformScale() {
        val radii = resolveMiuixVideoCardClipRadii(
            sourceCornerPx = 12f,
            outerScaleX = 0.5f,
            outerScaleY = 0.25f,
        )

        assertEquals(12f, radii.radiusX * 0.5f, absoluteTolerance = 0.0001f)
        assertEquals(12f, radii.radiusY * 0.25f, absoluteTolerance = 0.0001f)
    }

    @Test
    fun videoDetailHandsTheWholeSourceCardBackDuringTheGesture() {
        assertEquals(
            1f,
            resolveMiuixVideoCardReturnContentAlpha(
                morphProgress = 0f,
                isReturning = false,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0.5f,
            resolveMiuixVideoCardReturnContentAlpha(
                morphProgress = 0.275f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            1f,
            resolveMiuixVideoCardReturnContentAlpha(
                morphProgress = 0.45f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0f,
            resolveMiuixVideoCardReturnContentAlpha(
                morphProgress = 0.10f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun fullscreenStoryKeepsItsMediaOnlyContentOpaqueDuringReturn() {
        assertEquals(
            1f,
            resolveMiuixVideoCardReturnContentAlpha(
                morphProgress = 0.05f,
                isReturning = true,
                handoffWholeSourceCard = false,
            ),
            absoluteTolerance = 0.0001f,
        )
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
