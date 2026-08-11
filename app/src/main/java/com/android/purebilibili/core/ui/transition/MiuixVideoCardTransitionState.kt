package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * Miuix Nav card-morph state exposed to video-detail children.
 *
 * Both geometry and depth effects read the same deferred Miuix navigation driver. This keeps
 * secondary content, source-page blur, predictive back, and cancellation on one timeline without
 * restoring the removed AndroidX Navigation3 bridge.
 */
@Immutable
internal data class MiuixVideoCardTransitionState(
    val enabled: Boolean = false,
    val progressProvider: () -> Float = { 1f },
    val isGestureInProgressProvider: () -> Boolean = { false },
    /** 点击时冻结的整卡落点，供飞行详情壳内构造来源卡内容。 */
    val sourceBoundsProvider: () -> Rect? = { null },
)

internal val LocalMiuixVideoCardTransitionState = compositionLocalOf {
    MiuixVideoCardTransitionState()
}
