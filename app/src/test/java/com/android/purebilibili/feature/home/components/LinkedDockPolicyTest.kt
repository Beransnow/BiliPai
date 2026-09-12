package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkedDockPolicyTest {
    @Test
    fun expandedAudioOccupiesItsOwnRow() {
        val geometry = geometry(merge = 0f, search = 0f)
        assertEquals(336, geometry.audioWidth)
        assertEquals(0, geometry.audioX)
        assertEquals(0, geometry.audioY)
        assertEquals(72, geometry.top)
        assertEquals(136, geometry.height)
    }

    @Test
    fun mergedPlaybackFitsBetweenNavigationAndSearch() {
        val geometry = geometry(merge = 1f, search = 0f)
        assertEquals(56, geometry.audioX)
        assertEquals(224, geometry.audioWidth)
        assertEquals(336, geometry.audioX + geometry.audioWidth + geometry.searchWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun searchLeavesOneAccessibleArtworkTarget() {
        val geometry = geometry(merge = 1f, search = 1f)
        assertEquals(56, geometry.audioWidth)
        assertEquals(224, geometry.searchWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun narrowAndWideLayoutsDoNotOverlapAtRest() {
        for (width in listOf(240, 296, 336, 600)) {
            for (hasAudio in listOf(false, true)) {
                val geometry = resolveLinkedDockGeometry(width, 56, 64, 8, hasAudio, true, 1f, 1f)
                assertTrue(geometry.searchWidth >= 56)
                assertTrue(geometry.audioWidth >= 0)
                assertEquals(width, 56 + geometry.audioWidth + geometry.searchWidth)
            }
        }
    }

    @Test
    fun springOvershootCannotProduceNegativeSizes() {
        val geometry = geometry(merge = 1.05f, search = 1.04f)
        assertEquals(0, geometry.top)
        assertEquals(56, geometry.audioWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun directionChangeStartsANewScrollThreshold() {
        assertEquals(16f, accumulateDockScroll(10f, 6f))
        assertEquals(-3f, accumulateDockScroll(16f, -3f))
        assertEquals(-13f, accumulateDockScroll(-3f, -10f))
    }

    private fun geometry(merge: Float, search: Float) =
        resolveLinkedDockGeometry(336, 56, 64, 8, true, true, merge, search)
}
