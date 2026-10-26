package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContinuousPlayerTransitionPolicyTest {

    @Test
    fun enteringFullscreenRequestsLandscapeImmediately() {
        val awaitingLandscape = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.AwaitingLandscape,
            awaitingLandscape.phase,
        )
        assertEquals(
            ContinuousPlayerOrientationRequest.Landscape,
            awaitingLandscape.orientationRequest,
        )
    }

    @Test
    fun portraitObservationDoesNotCancelButtonEnterBeforeLandscapeRequestCompletes() {
        assertTrue(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                isLandscape = false,
            )
        )

        assertFalse(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.Fullscreen,
                isLandscape = false,
            )
        )
        assertFalse(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                isLandscape = true,
            )
        )
    }

    @Test
    fun systemLandscapeCompletesFullscreenWithoutRestartingAnimation() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )

        assertEquals(ContinuousPlayerTransitionPhase.Fullscreen, result.phase)
        assertEquals(ContinuousPlayerOrientationRequest.None, result.orientationRequest)
    }

    @Test
    fun exitingFullscreenKeepsLandscapePresentationUntilPortraitArrives() {
        val awaitingPortrait = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Fullscreen,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.AwaitingPortrait,
            awaitingPortrait.phase,
        )
        assertEquals(
            ContinuousPlayerOrientationRequest.Portrait,
            awaitingPortrait.orientationRequest,
        )
        assertTrue(resolveContinuousPlayerFullscreenPresentation(
            orientationFullscreen = false,
            phase = awaitingPortrait.phase,
            isLandscape = true,
        ))

        val inline = reduceContinuousPlayerTransition(
            phase = awaitingPortrait.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
        )
        assertEquals(ContinuousPlayerTransitionPhase.Inline, inline.phase)
        assertFalse(resolveContinuousPlayerFullscreenPresentation(
            orientationFullscreen = false, phase = inline.phase, isLandscape = false,
        ))
    }

    @Test
    fun togglingBeforeLandscapeArrivesCancelsTheOrientationRequest() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )

        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, result.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Portrait, result.orientationRequest)

        val lateLandscape = reduceContinuousPlayerTransition(
            phase = result.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, lateLandscape.phase)
    }

    @Test
    fun backDuringPendingEnterCancelsAndConsumesRepeatedBackUntilInline() {
        val entering = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(
            VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = false,
                isPortraitFullscreen = false,
                continuousPlayerPhase = entering.phase,
            ),
        )

        val cancelled = reduceContinuousPlayerTransition(
            phase = entering.phase,
            event = ContinuousPlayerTransitionEvent.ExitFullscreen,
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, cancelled.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Portrait, cancelled.orientationRequest)
        assertEquals(
            cancelled,
            reduceContinuousPlayerTransition(
                phase = cancelled.phase,
                event = ContinuousPlayerTransitionEvent.ExitFullscreen,
            ),
        )
        val lateLandscape = reduceContinuousPlayerTransition(
            phase = cancelled.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, lateLandscape.phase)

        assertEquals(
            VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = false,
                isPortraitFullscreen = false,
                continuousPlayerPhase = lateLandscape.phase,
            ),
        )
        val inline = reduceContinuousPlayerTransition(
            phase = lateLandscape.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
        )
        // Android's first portrait frame is already inline; there is no second collapse.
        assertEquals(ContinuousPlayerTransitionPhase.Inline, inline.phase)
        assertEquals(
            VideoDetailLocalBackTarget.NAVIGATE_BACK,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = false,
                isPortraitFullscreen = false,
                continuousPlayerPhase = inline.phase,
            ),
        )
    }

    @Test
    fun backFromButtonOrAutoRotatedFullscreenRequestsPortraitAndRestoresInlineImmediately() {
        for (phaseBeforeLandscape in listOf(
            ContinuousPlayerTransitionPhase.AwaitingLandscape,
            ContinuousPlayerTransitionPhase.Inline,
        )) {
            val fullscreen = reduceContinuousPlayerTransition(
                phase = phaseBeforeLandscape,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
            )
            assertEquals(ContinuousPlayerTransitionPhase.Fullscreen, fullscreen.phase)
            assertEquals(
                VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN,
                resolveVideoDetailLocalBackTarget(
                    isLandscapeFullscreen = true,
                    isPortraitFullscreen = false,
                    continuousPlayerPhase = fullscreen.phase,
                ),
            )
            val exiting = reduceContinuousPlayerTransition(
                phase = fullscreen.phase,
                event = ContinuousPlayerTransitionEvent.ExitFullscreen,
            )
            assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, exiting.phase)
            assertEquals(ContinuousPlayerOrientationRequest.Portrait, exiting.orientationRequest)
            assertTrue(resolveContinuousPlayerFullscreenPresentation(
                orientationFullscreen = false,
                phase = exiting.phase,
                isLandscape = true,
            ))
            val inline = reduceContinuousPlayerTransition(
                phase = exiting.phase,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
            )
            assertEquals(ContinuousPlayerTransitionPhase.Inline, inline.phase)
            assertFalse(resolveContinuousPlayerFullscreenPresentation(
                orientationFullscreen = false,
                phase = inline.phase,
                isLandscape = false,
            ))
        }
    }

    @Test
    fun backBeforeFullscreenOrientationObservationStillRequestsPortrait() {
        val decision = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.ExitFullscreen,
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, decision.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Portrait, decision.orientationRequest)
    }

    @Test
    fun systemLandscapeFromInlineSettlesFullscreen() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )
        assertEquals(ContinuousPlayerTransitionPhase.Fullscreen, result.phase)
    }

    @Test
    fun systemPortraitFromFullscreenOrStuckPhasesRestoresInlineImmediately() {
        assertEquals(
            ContinuousPlayerTransitionPhase.Inline,
            reduceContinuousPlayerTransition(
                phase = ContinuousPlayerTransitionPhase.Fullscreen,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
            ).phase,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.Inline,
            reduceContinuousPlayerTransition(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
            ).phase,
        )
    }

}
