package com.android.purebilibili.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 主页底栏 Tab 二级返回：栈顶为 [com.android.purebilibili.navigation3.BiliPaiNavKey.MainHost]
 * 且当前不在首页 Tab 时，边缘返回手势回到首页 Tab（而非直接退出应用）。
 *
 * 对齐 KernelSU / Navigation3 预测返回：
 * - [NavigationBackHandler] 上报系统进度；
 * - [snapshotFlow] 读 InProgress.progress，**同步**交给 [MainBottomPagerState] 做绝对 seek
 *   （dispatchRawDelta），避免把 progress 当作 LaunchedEffect 的 key 从而取消未完成滚动，
 *   导致最右 Tab 跨页时“完全不动、底栏先跳首页再弹回”。
 */
@Composable
internal fun MainHostTabBackHandler(
    enabled: Boolean,
    onPredictiveProgress: (Float) -> Unit,
    onPredictiveCancelled: () -> Unit,
    onPredictiveCompleted: () -> Boolean,
    onReturnToHomeTab: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackGestureEnabled = LocalPredictiveBackGestureEnabled.current

    LaunchedEffect(enabled, predictiveBackGestureEnabled, navEventState) {
        if (!enabled || !predictiveBackGestureEnabled) return@LaunchedEffect
        snapshotFlow {
            (navEventState.transitionState as? NavigationEventTransitionState.InProgress)
                ?.latestEvent
                ?.progress
        }
            .distinctUntilChanged()
            .collect { progress ->
                // Non-suspend seek: each tick applies absolute correction and must not cancel
                // the previous tick via collectLatest.
                if (progress != null) {
                    onPredictiveProgress(progress)
                }
            }
    }

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        reportPredictiveProgress = predictiveBackGestureEnabled,
        onBackCancelled = { commitTransition ->
            onPredictiveCancelled()
            commitTransition()
        },
        onBackCompleted = { commitTransition ->
            if (!onPredictiveCompleted()) {
                onReturnToHomeTab()
            }
            commitTransition()
        },
    )
}
