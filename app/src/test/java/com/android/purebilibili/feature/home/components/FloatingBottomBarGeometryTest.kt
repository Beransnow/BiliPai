package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloatingBottomBarGeometryTest {

    @Test
    fun `home-sized slots keep the 56dp resting indicator`() {
        assertEquals(
            56f,
            resolveFloatingDockIndicatorHeightDp(requestedHeightDp = 56f, tabWidthDp = 78f),
        )
    }

    @Test
    fun `narrow dynamic slots flatten the indicator instead of drawing a circle`() {
        val height = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 44f,
            tabWidthDp = 48f,
        )
        assertEquals(48f / FLOATING_DOCK_MIN_INDICATOR_ASPECT, height, 0.001f)
        assertTrue(height < 44f)
        assertTrue(48f / height >= FLOATING_DOCK_MIN_INDICATOR_ASPECT - 0.001f)
    }

    @Test
    fun `drag is rejected in the predictive-back edge bands`() {
        assertFalse(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 10f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
        assertFalse(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 1070f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
        assertTrue(
            shouldAcceptFloatingDockDragAtWindowX(
                windowX = 540f,
                screenWidthPx = 1080f,
                leftInsetPx = 72f,
                rightInsetPx = 72f,
            )
        )
    }

    @Test
    fun `short search and top docks scale lens so refraction cannot meet in the middle`() {
        assertEquals(1f, resolveCompactDockShellLensIntensity(shellHeightDp = 64f))
        assertEquals(36f / 64f, resolveCompactDockShellLensIntensity(shellHeightDp = 36f), 0.001f)
        val shortLens = FLOATING_DOCK_SHELL_LENS_DP * resolveCompactDockShellLensIntensity(36f)
        assertTrue(shortLens * 2f < 36f)
    }

    @Test
    fun `compact docks reuse bottom-bar highlight motion at scaled size`() {
        assertEquals(24f, resolveCompactDockLensDp(64f), 0.001f)
        assertEquals(16f, resolveCompactDockPressBloomDp(64f), 0.001f)
        assertEquals(10f, resolveCompactDockIndicatorLensHeightDp(64f), 0.001f)
        assertEquals(14f, resolveCompactDockIndicatorLensAmountDp(64f), 0.001f)
        assertEquals(8f, resolveCompactDockInnerShadowRadiusDp(64f), 0.001f)
        assertEquals(1.2f, resolveCompactDockTabPressScale(64f), 0.001f)

        val compact = 40f
        val intensity = 40f / 64f
        assertEquals(24f * intensity, resolveCompactDockLensDp(compact), 0.001f)
        assertEquals(16f * intensity, resolveCompactDockPressBloomDp(compact), 0.001f)
        assertEquals(10f * intensity, resolveCompactDockIndicatorLensHeightDp(compact), 0.001f)
        assertTrue(resolveCompactDockLensDp(compact) * 2f < compact)
        assertTrue(resolveCompactDockTabPressScale(compact) < 1.2f)
        assertTrue(resolveCompactDockTabPressScale(compact) > 1f)
    }

    @Test
    fun `pressed indicator overflow is reserved so compact docks are not clipped`() {
        val homeOverflow = resolveCompactDockScaleOverflowDp(
            shellHeightDp = 64f,
            indicatorHeightDp = 56f,
        )
        val compactOverflow = resolveCompactDockScaleOverflowDp(
            shellHeightDp = 40f,
            indicatorHeightDp = 35f,
        )
        assertEquals((78f - 64f) / 2f, homeOverflow, 0.001f)
        assertEquals((40f * 78f / 64f - 40f) / 2f, compactOverflow, 0.001f)
        assertTrue(compactOverflow > 0f)
    }

    @Test
    fun `narrow fitted indicator still grows beyond the dock while dragging`() {
        val fittedHeight = resolveFloatingDockIndicatorHeightDp(
            requestedHeightDp = 39f,
            tabWidthDp = 42f,
        )
        val geometry = com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 44f,
            indicatorHeightDp = fittedHeight,
        )

        assertTrue(fittedHeight < 39f)
        assertTrue(geometry.pressedHeightDp > geometry.dockHeightDp)
    }

    @Test
    fun `system gesture inset never shrinks below the fallback edge`() {
        assertEquals(24f, resolveFloatingDockDragEdgeInsetPx(systemInsetPx = 0f, fallbackPx = 24f))
        assertEquals(80f, resolveFloatingDockDragEdgeInsetPx(systemInsetPx = 80f, fallbackPx = 24f))
    }
}
