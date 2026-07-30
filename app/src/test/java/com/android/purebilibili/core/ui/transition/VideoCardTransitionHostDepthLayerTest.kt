package com.android.purebilibili.core.ui.transition

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardTransitionHostDepthLayerTest {

    @Test
    fun hostLayerPaintsOnlyWhenSourceWouldNotDrawSameGraphicsLayer() {
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertTrue(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Restoring,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        // 源页 effect 会 drawLayer，Host 让位避免同一 GraphicsLayer 双画
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.BackPreview,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Returning,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.Opening,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
    }

    @Test
    fun hostLayerRequiresRecordedContentAndRealtimeBlur() {
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = false,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                motionTier = MotionTier.Reduced,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = false,
                sdkInt = 35,
            ),
        )
        assertFalse(
            shouldPaintHostOwnedDepthLayer(
                exposure = VideoCardTransitionExposure.SettledHidden,
                hasRecordedContent = true,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 30,
            ),
        )
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
}
