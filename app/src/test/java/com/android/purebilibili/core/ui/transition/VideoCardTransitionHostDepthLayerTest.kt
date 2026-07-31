package com.android.purebilibili.core.ui.transition

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardTransitionHostDepthLayerTest {

    @Test
    fun hostLayerPaintsSettledBackPreviewRestoringAndReturning() {
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Restoring,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        // BackPreview / Returning：Host 继续画冻结层（满糊起跟手消糊），源页 yield 同 layer。
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.BackPreview,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Returning,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Opening,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
    }

    @Test
    fun hostLayerNeverPaintsStaleOrMissingDisplayList() {
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                displayListStale = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = false,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Reduced,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = false,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                displayListStale = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 30,
            ),
        )
    }

    @Test
    fun snapshotDrawableRequiresFreshDisplayList() {
        assertTrue(isVideoCardTransitionSnapshotDrawable(hasRecordedContent = true, displayListStale = false))
        assertFalse(isVideoCardTransitionSnapshotDrawable(hasRecordedContent = true, displayListStale = true))
        assertFalse(isVideoCardTransitionSnapshotDrawable(hasRecordedContent = false, displayListStale = false))
    }

    @Test
    fun settledHiddenForcesFullDepthForPredictiveReadyState() {
        assertEquals(
            1f,
            resolveHostOwnedDepthProgress(
                exposure = VideoCardTransitionExposure.SettledHidden,
                liveProgress = 0.2f,
            ),
        )
        assertEquals(
            0.35f,
            resolveHostOwnedDepthProgress(
                exposure = VideoCardTransitionExposure.BackPreview,
                liveProgress = 0.35f,
            ),
        )
    }

    @Test
    fun hostSnapshotSurvivesSourceDisposeAndOnlyReleasesOnIdle() {
        assertFalse(shouldInvalidateSnapshotOnSourceDispose(isHostOwnedSnapshot = true))
        assertTrue(shouldInvalidateSnapshotOnSourceDispose(isHostOwnedSnapshot = false))
        assertTrue(shouldReleaseHostOwnedDepthLayer(VideoCardTransitionExposure.Idle))
        assertFalse(shouldReleaseHostOwnedDepthLayer(VideoCardTransitionExposure.SettledHidden))
        assertFalse(shouldReleaseHostOwnedDepthLayer(VideoCardTransitionExposure.BackPreview))
    }

    @Test
    fun sourceYieldsOnlyWhenSettledHiddenUnderHost() {
        // 仅 SettledHidden：详情盖住时源可跳过；预测/返回源必须自己画糊。
        assertTrue(
            shouldSourceYieldDepthLayerToHost(
                isHostOwnedSnapshot = true,
                exposure = VideoCardTransitionExposure.SettledHidden,
            ),
        )
        assertFalse(
            shouldSourceYieldDepthLayerToHost(
                isHostOwnedSnapshot = true,
                exposure = VideoCardTransitionExposure.BackPreview,
            ),
        )
        assertFalse(
            shouldSourceYieldDepthLayerToHost(
                isHostOwnedSnapshot = true,
                exposure = VideoCardTransitionExposure.Returning,
            ),
        )
        assertFalse(
            shouldSourceYieldDepthLayerToHost(
                isHostOwnedSnapshot = false,
                exposure = VideoCardTransitionExposure.SettledHidden,
            ),
        )
    }

    @Test
    fun hostOwnedDisposeKeepsFrozenSnapshotNotStale() {
        assertFalse(shouldMarkDisplayListStaleOnHostOwnedSourceDispose())
    }
}
