package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.android.purebilibili.core.ui.motion.resolveSettingsIosPredictivePopContentTransform
import com.android.purebilibili.core.ui.motion.resolveSettingsIosPushPopContentTransform
import com.android.purebilibili.navigation3.BiliPaiNavKey

/**
 * 设置树预测式返回：手势预览保持目标页全屏、只横滑顶页，避免 parallax 入场露出灰缝；
 * 松手提交仍用完整 iOS push/pop 视差，与普通返回一致。
 */
internal class BiliPaiSettingsIosPredictiveBackAnimation : BiliPaiPredictiveBackAnimationHandler {
    private var committedExitDirectionSign: Int? = null

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: BiliPaiNavKey?,
    ) {
        committedExitDirectionSign = (transitionState as? InProgress)
            ?.latestEvent
            ?.swipeEdge
            ?.let { swipeEdge -> resolveBiliPaiPredictiveBackExitDirectionSign(swipeEdge) }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: BiliPaiNavKey?,
    ): Modifier = this

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec(
        @NavigationEvent.SwipeEdge swipeEdge: Int,
    ): ContentTransform = resolveSettingsIosPredictivePopContentTransform(
        exitDirectionSign = resolveBiliPaiPredictiveBackExitDirectionSign(swipeEdge),
    )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec(): ContentTransform =
        resolveSettingsIosPushPopContentTransform(
            exitDirectionSign = committedExitDirectionSign ?: 1,
        )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)
}
