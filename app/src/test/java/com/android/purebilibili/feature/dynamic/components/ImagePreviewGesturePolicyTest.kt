package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImagePreviewGesturePolicyTest {

    @Test
    fun predictiveProgressUsesOneSnapshotCollectorAndPagerMotionReadsInLayers() {
        val path = "app/src/main/java/com/android/purebilibili/feature/dynamic/components/ImagePreviewDialog.kt"
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        val source = sourceFile.readText()

        assertTrue(source.contains("snapshotFlow {"))
        assertTrue(source.contains(".collectLatest { backProgress ->"))
        assertFalse(source.contains("LaunchedEffect(backProgress, isDismissing)"))
        assertTrue(source.contains("val pageOffset = pageOffsetProvider()"))
        assertTrue(source.contains("pageOffsetFraction = pagerState.currentPageOffsetFraction"))
    }

    @Test
    fun shouldEnableImagePreviewVerticalDismiss_allowsScaleAtRest() {
        assertTrue(shouldEnableImagePreviewVerticalDismiss(1f))
        assertTrue(shouldEnableImagePreviewVerticalDismiss(1.009f))
    }

    @Test
    fun shouldEnableImagePreviewVerticalDismiss_blocksZoomedImages() {
        assertFalse(shouldEnableImagePreviewVerticalDismiss(1.02f))
        assertFalse(shouldEnableImagePreviewVerticalDismiss(2f))
    }

    @Test
    fun resolveZoomableImageGestureMode_prefersVerticalDismissForSingleFingerSwipe() {
        assertEquals(
            ZoomableImageGestureMode.VERTICAL_DISMISS,
            resolveZoomableImageGestureMode(
                isMultiTouch = false,
                scale = 1f,
                panX = 8f,
                panY = 40f
            )
        )
        assertEquals(
            ZoomableImageGestureMode.HORIZONTAL_PAGER,
            resolveZoomableImageGestureMode(
                isMultiTouch = false,
                scale = 1f,
                panX = 40f,
                panY = 8f
            )
        )
        assertEquals(
            ZoomableImageGestureMode.IMAGE_INTERACTION,
            resolveZoomableImageGestureMode(
                isMultiTouch = true,
                scale = 1f,
                panX = 8f,
                panY = 40f
            )
        )
    }

    @Test
    fun resolveImagePreviewVerticalDismissDecision_dismissesLargeDownwardDrag() {
        val decision = resolveImagePreviewVerticalDismissDecision(
            dragOffsetYPx = 220f,
            containerHeightPx = 1200f
        )

        assertEquals(ImagePreviewVerticalDismissDecision.DISMISS, decision)
    }

    @Test
    fun resolveImagePreviewVerticalDismissDecision_dismissesLargeUpwardDrag() {
        val decision = resolveImagePreviewVerticalDismissDecision(
            dragOffsetYPx = -220f,
            containerHeightPx = 1200f
        )

        assertEquals(ImagePreviewVerticalDismissDecision.DISMISS, decision)
    }

    @Test
    fun resolveImagePreviewVerticalDismissDecision_snapsBackForSmallDrag() {
        val decision = resolveImagePreviewVerticalDismissDecision(
            dragOffsetYPx = 72f,
            containerHeightPx = 1200f
        )

        assertEquals(ImagePreviewVerticalDismissDecision.SNAP_BACK, decision)
    }

    @Test
    fun resolveImagePreviewVerticalDragFrame_reducesScaleAndBackdropAsDragGrows() {
        val start = resolveImagePreviewVerticalDragFrame(
            dragOffsetYPx = 0f,
            containerHeightPx = 1200f
        )
        val dragged = resolveImagePreviewVerticalDragFrame(
            dragOffsetYPx = 240f,
            containerHeightPx = 1200f
        )

        assertEquals(1f, start.scale, 0.0001f)
        assertTrue(dragged.scale < start.scale)
        assertTrue(dragged.backdropAlphaMultiplier < start.backdropAlphaMultiplier)
        assertTrue(dragged.progress > 0f)
    }
}
