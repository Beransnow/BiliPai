package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.graphics.TransformOrigin
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import top.yukonga.miuix.kmp.nav.transition.NavRole

class MiuixVideoCardNavTransitionTest {
    @Test
    fun transitionUsesOpaqueRevealMaskInsteadOfLeavingDetailPageOverSourceChrome() {
        val source = File(
            "src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt"
        ).readText()
        val transform = source.substringAfter("override fun Modifier.transformEntry")

        assertEquals(true, transform.contains("alpha = 1f"))
        assertEquals(true, transform.contains("visibleHeightFraction = outgoingClipFraction"))
        assertEquals(false, transform.contains("alpha = resolveMiuixVideoCard"))
    }

    @Test
    fun predictiveSeekIsAReturnEvenWhenMiuixReportsIncomingRole() {
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
    fun videoDetailUncoversRetainedCardChromeDuringReturnHandoff() {
        assertEquals(
            1f,
            resolveMiuixVideoCardOutgoingClipFraction(
                morphProgress = 0.45f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0.5f,
            resolveMiuixVideoCardOutgoingClipFraction(
                morphProgress = 0.275f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0f,
            resolveMiuixVideoCardOutgoingClipFraction(
                morphProgress = 0.10f,
                isReturning = true,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun openingAndFullscreenMediaNeverUseWholeCardRevealMask() {
        assertEquals(
            1f,
            resolveMiuixVideoCardOutgoingClipFraction(
                morphProgress = 0.10f,
                isReturning = false,
                handoffWholeSourceCard = true,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            1f,
            resolveMiuixVideoCardOutgoingClipFraction(
                morphProgress = 0.10f,
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

    @Test
    fun sideBySideAndStackedUseFillWidthTopForLandingAnchors() {
        assertEquals(
            MiuixVideoCardContentScale.FillWidthTop,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
            ),
        )
        assertEquals(
            MiuixVideoCardContentScale.FillWidthTop,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.STACKED,
            ),
        )
        assertEquals(
            MiuixVideoCardContentScale.CropCenter,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.STACKED,
                fullscreen = true,
            ),
        )
    }

    @Test
    fun gestureFollowPeaksMidFlightAndKeepsBothLandingEndpointsExact() {
        val fullscreen = resolveMiuixVideoCardGestureTransform(
            morphProgress = 1f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val midFlight = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val landed = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )

        assertEquals(0f, fullscreen.translationX)
        assertEquals(0f, fullscreen.translationY)
        assertEquals(0f, fullscreen.rotationZ)
        assertEquals(0f, landed.translationX)
        assertEquals(0f, landed.translationY)
        assertEquals(0f, landed.rotationZ)
        assertEquals(true, abs(midFlight.translationX) > 0f)
        assertEquals(true, abs(midFlight.translationY) > 0f)
        assertEquals(true, abs(midFlight.rotationZ) > 0f)
    }

    @Test
    fun gestureFollowMirrorsAcrossScreenEdgesWithoutChangingVerticalLanding() {
        val left = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val right = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = false,
            maxVerticalTravelPx = 72f,
        )

        assertEquals(-left.translationX, right.translationX, absoluteTolerance = 0.001f)
        assertEquals(left.translationY, right.translationY, absoluteTolerance = 0.001f)
        assertEquals(-left.rotationZ, right.rotationZ, absoluteTolerance = 0.001f)
    }
}
