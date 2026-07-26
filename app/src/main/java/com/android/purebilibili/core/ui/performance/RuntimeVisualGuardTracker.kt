package com.android.purebilibili.core.ui.performance

import androidx.metrics.performance.FrameData
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.RuntimeVisualGuardDecision
import com.android.purebilibili.core.ui.adaptive.isRuntimeVisualGuardHighJankWindow
import com.android.purebilibili.core.ui.adaptive.resolveRuntimeVisualGuardDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val VIDEO_CARD_TRANSITION_JANK_STATE = "VideoCardTransition"
internal const val RUNTIME_VISUAL_GUARD_MIN_FRAME_COUNT = 12

internal class RuntimeVisualGuardTracker(
    private val baseTier: MotionTier = MotionTier.Normal,
    private val enabled: Boolean = true,
    private val minimumFrameCount: Int = RUNTIME_VISUAL_GUARD_MIN_FRAME_COUNT,
) {
    private val _decision = MutableStateFlow(normalDecision())
    val decision: StateFlow<RuntimeVisualGuardDecision> = _decision.asStateFlow()

    private var activeStateValue: String? = null
    private var windowFrameCount = 0
    private var windowJankyFrameCount = 0
    private var consecutiveHighJankWindows = 0
    private var lastDowngradeAtMs: Long? = null

    fun onFrame(frameData: FrameData, nowMs: Long) {
        val stateValue = frameData.states
            .firstOrNull { it.key == VIDEO_CARD_TRANSITION_JANK_STATE }
            ?.value
        onFrame(
            stateValue = stateValue,
            isJank = frameData.isJank,
            nowMs = nowMs,
        )
    }

    internal fun onFrame(
        tracked: Boolean,
        isJank: Boolean,
        nowMs: Long,
    ) {
        onFrame(
            stateValue = if (tracked) "Tracked" else null,
            isJank = isJank,
            nowMs = nowMs,
        )
    }

    internal fun onFrame(
        stateValue: String?,
        isJank: Boolean,
        nowMs: Long,
    ) {
        if (stateValue.isNullOrBlank()) {
            if (activeStateValue != null) finishActiveWindow(nowMs)
            return
        }

        if (activeStateValue != null && activeStateValue != stateValue) {
            finishActiveWindow(nowMs)
        }
        activeStateValue = stateValue
        windowFrameCount += 1
        if (isJank) windowJankyFrameCount += 1
    }

    fun discardActiveWindow() {
        clearActiveWindow()
    }

    private fun finishActiveWindow(nowMs: Long) {
        val frameCount = windowFrameCount
        val jankyFrameCount = windowJankyFrameCount
        clearActiveWindow()
        if (frameCount < minimumFrameCount) return

        val jankPercent = jankyFrameCount * 100f / frameCount
        val wasDowngraded = _decision.value.downgraded
        if (wasDowngraded) {
            val resolved = resolveRuntimeVisualGuardDecision(
                enabled = enabled,
                baseTier = baseTier,
                rollingJankPercent = jankPercent,
                consecutiveHighJankWindows = 0,
                lastDowngradeAtMs = lastDowngradeAtMs,
                nowMs = nowMs,
            )
            if (resolved.downgraded) {
                _decision.value = resolved
            } else {
                lastDowngradeAtMs = null
                consecutiveHighJankWindows = 0
                _decision.value = resolved.copy(nextLastDowngradeAtMs = null)
            }
            return
        }

        consecutiveHighJankWindows = if (isRuntimeVisualGuardHighJankWindow(jankPercent)) {
            consecutiveHighJankWindows + 1
        } else {
            0
        }
        val resolved = resolveRuntimeVisualGuardDecision(
            enabled = enabled,
            baseTier = baseTier,
            rollingJankPercent = jankPercent,
            consecutiveHighJankWindows = consecutiveHighJankWindows,
            lastDowngradeAtMs = null,
            nowMs = nowMs,
        )
        lastDowngradeAtMs = resolved.nextLastDowngradeAtMs
        _decision.value = resolved
    }

    private fun clearActiveWindow() {
        activeStateValue = null
        windowFrameCount = 0
        windowJankyFrameCount = 0
    }

    private fun normalDecision() = RuntimeVisualGuardDecision(
        effectiveMotionTier = baseTier,
        forceLowBlurBudget = false,
        downgraded = false,
        nextLastDowngradeAtMs = null,
    )
}

internal val AppRuntimeVisualGuardTracker = RuntimeVisualGuardTracker()
