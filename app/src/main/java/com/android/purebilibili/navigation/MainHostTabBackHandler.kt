package com.android.purebilibili.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 主页底栏 Tab 二级返回：栈顶为 [com.android.purebilibili.navigation3.BiliPaiNavKey.MainHost]
 * 且当前不在首页 Tab 时，边缘返回手势回到首页 Tab（而非直接退出应用）。
 *
 * 对齐 KernelSU / Navigation3 的预测返回驱动方式：
 * - 使用 [NavigationBackHandler] 上报系统进度；
 * - 用 [snapshotFlow] 连续收集进度并 [collectLatest] 驱动 seek，
 *   **不要** `LaunchedEffect(progress)`（每次进度变化会取消上一帧的 suspend scroll，
 *   最右 Tab → 首页跨页距离大时几乎每帧都被取消，表现为手势卡住不动）。
 *
 * 进度到目标页的映射由 [MainBottomPagerState.seekPredictiveReturnToPage] 做绝对定位 seek
 * （类似 SeekableTransitionState），而不是依赖易被打断的 delta 链。
 */
@Composable
internal fun MainHostTabBackHandler(
    enabled: Boolean,
    onPredictiveProgress: suspend (Float) -> Unit,
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
            .collectLatest { progress ->
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
