package com.android.purebilibili.navigation

import androidx.compose.foundation.MutatePriority
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class PredictivePagerReturnSession(
    val startPage: Int,
    val targetPage: Int,
    val pageStepPx: Float,
    var lastProgress: Float,
)

internal class MainBottomPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var navigationStartPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var navJob: Job? = null
    private var predictiveReturnSession: PredictivePagerReturnSession? = null

    fun switchToPage(targetIndex: Int) {
        predictiveReturnSession = null
        val lastPage = pagerState.pageCount - 1
        if (lastPage < 0) return
        val safeTargetIndex = targetIndex.coerceIn(0, lastPage)
        if (safeTargetIndex == selectedPage) return

        val previousJob = navJob
        navJob = null
        previousJob?.cancel()

        navigationStartPage = pagerState.currentPage
        selectedPage = safeTargetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                previousJob?.join()
                awaitScrollIdle()
                awaitNextFrame()
                if (!animatePageChange(safeTargetIndex)) {
                    pagerState.scrollToPage(safeTargetIndex)
                }
                delay(BOTTOM_TAB_RENDER_BUDGET_HOLD_MILLIS)
            } catch (_: IllegalStateException) {
                // Pager 在测量竞争期间可能拒绝切页，保持当前页并避免快速点击闪退。
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                    navigationStartPage = pagerState.currentPage
                    navJob = null
                }
            }
        }
    }

    /**
     * KernelSU / Navigation3 style: seek the pager to an **absolute** position derived from
     * system predictive progress (like SeekableTransitionState), not a fragile delta chain.
     *
     * Far tabs (rightmost → home) are the worst case: each progress tick used to cancel the
     * previous suspend `scrollBy` when progress was a LaunchedEffect key. Absolute correction
     * from current offset keeps the gesture responsive even when frames drop.
     */
    suspend fun seekPredictiveReturnToPage(
        targetIndex: Int,
        progress: Float,
    ) {
        val safeTargetIndex = targetIndex.coerceIn(0, pagerState.pageCount - 1)
        val session = predictiveReturnSession ?: run {
            val pageStepPx = (
                pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
                ).toFloat()
            if (pageStepPx <= 0f || safeTargetIndex == pagerState.currentPage) return

            val previousJob = navJob
            navJob = null
            previousJob?.cancel()
            navigationStartPage = pagerState.currentPage
            // Pre-select home so only start + target need real content during the far jump.
            selectedPage = safeTargetIndex
            isNavigating = true
            PredictivePagerReturnSession(
                startPage = pagerState.currentPage,
                targetPage = safeTargetIndex,
                pageStepPx = pageStepPx,
                lastProgress = 0f,
            ).also { predictiveReturnSession = it }
        }
        if (session.targetPage != safeTargetIndex) return

        val normalizedProgress = progress.coerceIn(0f, 1f)
        val pageStepPx = session.pageStepPx.takeIf { it > 0f } ?: (
            pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
            ).toFloat()
        if (pageStepPx <= 0f) return

        val targetScrollPx = resolvePredictivePagerTargetScrollPx(
            startPage = session.startPage,
            targetPage = session.targetPage,
            pageStepPx = pageStepPx,
            progress = normalizedProgress,
        )
        val currentScrollPx = resolvePredictivePagerCurrentScrollPx(
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            pageStepPx = pageStepPx,
        )
        val deltaPx = targetScrollPx - currentScrollPx
        if (abs(deltaPx) > 0.5f) {
            try {
                pagerState.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(deltaPx)
                }
            } catch (_: IllegalStateException) {
                // Measurement races can reject scroll; leave lastProgress so the next tick retries.
                return
            }
        }
        session.lastProgress = normalizedProgress
    }

    /** Returns true only when a predictive session was active and was committed. */
    fun commitPredictiveReturnToPage(targetIndex: Int): Boolean {
        val session = predictiveReturnSession ?: return false
        predictiveReturnSession = null
        val safeTargetIndex = targetIndex.coerceIn(0, pagerState.pageCount - 1)
        settlePredictiveReturn(
            targetPage = safeTargetIndex,
            fallbackPage = session.targetPage,
            maxDurationMillis = PREDICTIVE_RETURN_COMPLETE_MAX_MILLIS,
            progressDistance = 1f - session.lastProgress,
        )
        return true
    }

    fun cancelPredictiveReturn() {
        val session = predictiveReturnSession ?: return
        predictiveReturnSession = null
        settlePredictiveReturn(
            targetPage = session.startPage,
            fallbackPage = session.startPage,
            maxDurationMillis = PREDICTIVE_RETURN_CANCEL_MAX_MILLIS,
            progressDistance = session.lastProgress,
        )
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }

    private fun settlePredictiveReturn(
        targetPage: Int,
        fallbackPage: Int,
        maxDurationMillis: Int,
        progressDistance: Float,
    ) {
        val previousJob = navJob
        navJob = null
        previousJob?.cancel()
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                previousJob?.join()
                val settleDurationMillis = resolvePredictivePagerSettleDurationMillis(
                    maxDurationMillis = maxDurationMillis,
                    progressDistance = progressDistance,
                )
                if (settleDurationMillis == 0) {
                    pagerState.scrollToPage(targetPage)
                } else {
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(
                            durationMillis = settleDurationMillis,
                            easing = LinearOutSlowInEasing,
                        ),
                    )
                }
                delay(BOTTOM_TAB_RENDER_BUDGET_HOLD_MILLIS)
            } catch (_: IllegalStateException) {
                pagerState.scrollToPage(fallbackPage)
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                    navigationStartPage = pagerState.currentPage
                    navJob = null
                }
            }
        }
    }

    private suspend fun animatePageChange(targetIndex: Int): Boolean {
        val layoutInfo = pagerState.layoutInfo
        if (layoutInfo.pageSize <= 0) return false

        val currentPage = pagerState.currentPage
        if (targetIndex == currentPage && abs(pagerState.currentPageOffsetFraction) < 0.001f) {
            return false
        }

        val durationMillis = resolveBottomPagerNavigationDurationMillis(
            pageDistance = abs(targetIndex - currentPage)
        ).coerceAtMost(BOTTOM_PAGER_ANIMATED_SCROLL_MAX_MILLIS)
        pagerState.run {
            animateScrollToPage(
                page = targetIndex,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = LinearOutSlowInEasing,
                ),
            )
        }
        return true
    }

    private suspend fun awaitScrollIdle() {
        if (pagerState.isScrollInProgress) {
            snapshotFlow { pagerState.isScrollInProgress }.first { !it }
        }
    }

    private suspend fun awaitNextFrame() {
        withFrameNanos { }
    }
}

private const val BOTTOM_PAGER_ANIMATED_SCROLL_MAX_MILLIS = 280
private const val PREDICTIVE_RETURN_COMPLETE_MAX_MILLIS = 160
private const val PREDICTIVE_RETURN_CANCEL_MAX_MILLIS = 180

internal fun resolvePredictivePagerScrollDeltaPx(
    startPage: Int,
    targetPage: Int,
    pageStepPx: Float,
    previousProgress: Float,
    progress: Float,
): Float {
    val progressDelta = progress.coerceIn(0f, 1f) - previousProgress.coerceIn(0f, 1f)
    return (targetPage - startPage) * pageStepPx * progressDelta
}

/** Absolute scroll position for predictive seek (KernelSU / SeekableTransition style). */
internal fun resolvePredictivePagerTargetScrollPx(
    startPage: Int,
    targetPage: Int,
    pageStepPx: Float,
    progress: Float,
): Float {
    if (pageStepPx <= 0f) return 0f
    return (startPage + (targetPage - startPage) * progress.coerceIn(0f, 1f)) * pageStepPx
}

internal fun resolvePredictivePagerCurrentScrollPx(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageStepPx: Float,
): Float {
    if (pageStepPx <= 0f) return 0f
    return (currentPage + currentPageOffsetFraction) * pageStepPx
}

internal fun resolvePredictivePagerSettleDurationMillis(
    maxDurationMillis: Int,
    progressDistance: Float,
): Int = (maxDurationMillis.coerceAtLeast(0) * progressDistance.coerceIn(0f, 1f)).toInt()

@Composable
internal fun rememberMainBottomPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainBottomPagerState {
    return remember(pagerState, coroutineScope) {
        MainBottomPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope
        )
    }
}
