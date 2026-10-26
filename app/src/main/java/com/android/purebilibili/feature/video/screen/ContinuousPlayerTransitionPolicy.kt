package com.android.purebilibili.feature.video.screen

internal enum class ContinuousPlayerTransitionPhase {
    Inline,
    AwaitingLandscape,
    Fullscreen,
    AwaitingPortrait,
}
internal enum class ContinuousPlayerOrientationRequest {
    None,
    Landscape,
    Portrait,
}

internal sealed interface ContinuousPlayerTransitionEvent {
    data object Toggle : ContinuousPlayerTransitionEvent
    data object ExitFullscreen : ContinuousPlayerTransitionEvent
    data class OrientationChanged(val isLandscape: Boolean) : ContinuousPlayerTransitionEvent
}

internal data class ContinuousPlayerTransitionDecision(
    val phase: ContinuousPlayerTransitionPhase,
    val orientationRequest: ContinuousPlayerOrientationRequest =
        ContinuousPlayerOrientationRequest.None,
)

internal fun shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
    phase: ContinuousPlayerTransitionPhase,
    isLandscape: Boolean,
): Boolean {
    return !isLandscape && phase == ContinuousPlayerTransitionPhase.AwaitingLandscape
}

internal fun resolveContinuousPlayerFullscreenPresentation(
    orientationFullscreen: Boolean,
    phase: ContinuousPlayerTransitionPhase,
    isLandscape: Boolean,
): Boolean = orientationFullscreen ||
    (isLandscape && phase == ContinuousPlayerTransitionPhase.AwaitingPortrait)

internal fun reduceContinuousPlayerTransition(
    phase: ContinuousPlayerTransitionPhase,
    event: ContinuousPlayerTransitionEvent,
): ContinuousPlayerTransitionDecision {
    return when (event) {
        ContinuousPlayerTransitionEvent.Toggle -> when (phase) {
            ContinuousPlayerTransitionPhase.Inline -> ContinuousPlayerTransitionDecision(
                // 直接请求横屏，避免先在竖屏窗口展开成全屏再旋转。
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                orientationRequest = ContinuousPlayerOrientationRequest.Landscape,
            )

            ContinuousPlayerTransitionPhase.AwaitingLandscape,
            ContinuousPlayerTransitionPhase.Fullscreen -> ContinuousPlayerTransitionDecision(
                phase = ContinuousPlayerTransitionPhase.AwaitingPortrait,
                orientationRequest = ContinuousPlayerOrientationRequest.Portrait,
            )

            ContinuousPlayerTransitionPhase.AwaitingPortrait -> ContinuousPlayerTransitionDecision(
                phase = ContinuousPlayerTransitionPhase.AwaitingPortrait,
                orientationRequest = ContinuousPlayerOrientationRequest.Portrait,
            )
        }

        // Back is exit-only, including repeated presses while Android is still rotating.
        ContinuousPlayerTransitionEvent.ExitFullscreen -> ContinuousPlayerTransitionDecision(
            phase = ContinuousPlayerTransitionPhase.AwaitingPortrait,
            orientationRequest = ContinuousPlayerOrientationRequest.Portrait,
        )

        is ContinuousPlayerTransitionEvent.OrientationChanged -> when {
            event.isLandscape &&
                phase == ContinuousPlayerTransitionPhase.AwaitingLandscape ->
                ContinuousPlayerTransitionDecision(ContinuousPlayerTransitionPhase.Fullscreen)

            // 系统旋转进横屏（非按钮路径）时，任意非全屏相位都应落到 Fullscreen，
            // 避免卡在 AwaitingLandscape 导致回竖屏后进度仍为 1。
            event.isLandscape &&
                phase != ContinuousPlayerTransitionPhase.Fullscreen &&
                phase != ContinuousPlayerTransitionPhase.AwaitingPortrait ->
                ContinuousPlayerTransitionDecision(ContinuousPlayerTransitionPhase.Fullscreen)

            !event.isLandscape &&
                phase == ContinuousPlayerTransitionPhase.AwaitingPortrait ->
                ContinuousPlayerTransitionDecision(ContinuousPlayerTransitionPhase.Inline)

            // Android owns rotation; the first portrait frame must already use inline bounds.
            !event.isLandscape &&
                (
                    phase == ContinuousPlayerTransitionPhase.Fullscreen ||
                        phase == ContinuousPlayerTransitionPhase.AwaitingLandscape
                    ) ->
                ContinuousPlayerTransitionDecision(ContinuousPlayerTransitionPhase.Inline)

            else -> ContinuousPlayerTransitionDecision(phase)
        }
    }
}
