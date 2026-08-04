package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MainBottomPagerPredictiveBackPolicyTest {

    @Test
    fun `predictive return maps system progress to a linear pager delta`() {
        assertEquals(
            -360f,
            resolvePredictivePagerScrollDeltaPx(
                startPage = 3,
                targetPage = 0,
                pageStepPx = 400f,
                previousProgress = 0.2f,
                progress = 0.5f,
            ),
        )
    }

    @Test
    fun `absolute predictive seek targets rightmost to home without depending on prior delta`() {
        // KernelSU / SeekableTransition style: each progress maps to absolute scroll.
        assertEquals(
            1600f,
            resolvePredictivePagerTargetScrollPx(
                startPage = 4,
                targetPage = 0,
                pageStepPx = 400f,
                progress = 0f,
            ),
        )
        assertEquals(
            800f,
            resolvePredictivePagerTargetScrollPx(
                startPage = 4,
                targetPage = 0,
                pageStepPx = 400f,
                progress = 0.5f,
            ),
        )
        assertEquals(
            0f,
            resolvePredictivePagerTargetScrollPx(
                startPage = 4,
                targetPage = 0,
                pageStepPx = 400f,
                progress = 1f,
            ),
        )
        assertEquals(
            1200f,
            resolvePredictivePagerCurrentScrollPx(
                currentPage = 3,
                currentPageOffsetFraction = 0f,
                pageStepPx = 400f,
            ),
        )
    }

    @Test
    fun `predictive return clamps out of range progress`() {
        assertEquals(
            -800f,
            resolvePredictivePagerScrollDeltaPx(
                startPage = 2,
                targetPage = 0,
                pageStepPx = 400f,
                previousProgress = -1f,
                progress = 2f,
            ),
        )
    }

    @Test
    fun `predictive return only settles its remaining distance after release`() {
        assertEquals(
            80,
            resolvePredictivePagerSettleDurationMillis(
                maxDurationMillis = 160,
                progressDistance = 0.5f,
            ),
        )
    }
}
