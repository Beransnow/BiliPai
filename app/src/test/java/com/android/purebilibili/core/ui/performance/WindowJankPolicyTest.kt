package com.android.purebilibili.core.ui.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class WindowJankPolicyTest {

    @Test
    fun releaseRequiresAnalyticsConsentAndOnePercentSample() {
        assertEquals(
            WindowJankCollectionMode.UPLOAD,
            resolveWindowJankCollectionMode("release", analyticsEnabled = true, releaseSampleValue = 0.0099),
        )
        assertEquals(
            WindowJankCollectionMode.DISABLED,
            resolveWindowJankCollectionMode("release", analyticsEnabled = true, releaseSampleValue = 0.01),
        )
        assertEquals(
            WindowJankCollectionMode.DISABLED,
            resolveWindowJankCollectionMode("release", analyticsEnabled = false, releaseSampleValue = 0.0),
        )
    }

    @Test
    fun localBuildsCollectAtOneHundredPercentWithoutUpload() {
        listOf("debug", "dev", "smooth").forEach { buildType ->
            assertEquals(
                WindowJankCollectionMode.LOCAL_ONLY,
                resolveWindowJankCollectionMode(
                    buildType = buildType,
                    analyticsEnabled = false,
                    releaseSampleValue = 0.999,
                ),
            )
        }
    }

    @Test
    fun routeNormalizationDropsParametersAndAllLiveDestinations() {
        assertEquals("video_detail", normalizeWindowJankRoute("video/BV-secret?cid=42&title=secret"))
        assertEquals("video_detail", normalizeWindowJankRoute("video_detail"))
        assertEquals("video_detail", normalizeWindowJankRoute("video_player/BV-secret?cid=42&title=secret"))
        assertEquals("web", normalizeWindowJankRoute("web?url=https%3A%2F%2Fexample.com%2Flive%2F42"))
        assertEquals("space", normalizeWindowJankRoute("space/123456?targetBvid=BV-secret"))
        assertEquals("other", normalizeWindowJankRoute("unknown/123/private-title"))
        assertNull(normalizeWindowJankRoute("live/123?title=secret&uname=user"))
        assertNull(normalizeWindowJankRoute("live_list"))
        assertNull(normalizeWindowJankRoute("live_area_detail/1/2?title=secret"))
        assertNull(normalizeWindowJankRoute("LiveRoomRoute(roomId=123)"))
        assertNull(normalizeWindowJankRoute(null))
    }

    @Test
    fun interactionNormalizationUsesFixedVocabulary() {
        assertEquals(WindowJankInteraction.SCROLL, normalizeWindowJankInteraction("HomeFeed Scrolling=true"))
        assertEquals(WindowJankInteraction.PREDICTIVE_BACK, normalizeWindowJankInteraction("predictive back:42"))
        assertEquals(WindowJankInteraction.DANMAKU, normalizeWindowJankInteraction("danmaku-bv-secret"))
        assertEquals(WindowJankInteraction.OTHER, normalizeWindowJankInteraction("search term: private"))
    }

    @Test
    fun primitiveAccumulatorBucketsAndResetsAtFlushBoundary() {
        val accumulator = WindowJankPrimitiveAccumulator()
        repeat(40) {
            accumulator.record(
                frameDurationUiNanos = 7_000_000L,
                isJank = false,
                interactionOrdinal = WindowJankInteraction.SCROLL.ordinal,
            )
        }
        repeat(20) {
            accumulator.record(
                frameDurationUiNanos = 30_000_000L,
                isJank = true,
                interactionOrdinal = WindowJankInteraction.NAVIGATION.ordinal,
            )
        }

        val summary = requireNotNull(
            accumulator.snapshotAndReset(
                route = "home",
                refreshRateHz = 120f,
            )
        )
        assertEquals("home", summary.route)
        assertEquals("scroll", summary.interaction)
        assertEquals(60L, summary.frameCount)
        assertEquals(20L, summary.jankCount)
        assertEquals("12_17ms", summary.averageFrameDurationBucket)
        assertEquals("24_40ms", summary.maxFrameDurationBucket)
        assertEquals("91_120hz", summary.refreshRateBucket)
        assertNull(
            accumulator.snapshotAndReset(
                route = "home",
                refreshRateHz = 60f,
            )
        )
    }

    @Test
    fun timingAndRefreshBucketsExposeNoRawMeasurements() {
        assertEquals("under_8ms", resolveFrameDurationBucket(7_999_999L))
        assertEquals("8_12ms", resolveFrameDurationBucket(8_000_000L))
        assertEquals("over_64ms", resolveFrameDurationBucket(64_000_000L))
        assertEquals("up_to_60hz", resolveRefreshRateBucket(60f))
        assertEquals("61_90hz", resolveRefreshRateBucket(90f))
        assertEquals("unknown", resolveRefreshRateBucket(Float.NaN))
    }

    @Test
    fun reportLimiterDropsShortSamplesAndCapsTheProcessAtTwelve() {
        val limiter = WindowJankReportLimiter()
        assertEquals(false, limiter.tryAcquire(frameCount = 59L))
        repeat(12) {
            assertEquals(true, limiter.tryAcquire(frameCount = 60L))
        }
        assertEquals(false, limiter.tryAcquire(frameCount = 60L))
    }

    @Test
    fun defaultReporterKeepsLocalAndUploadDeliverySeparated() {
        val summary = WindowJankSummary(
            route = "home",
            interaction = "scroll",
            frameCount = 600L,
            jankCount = 3L,
            averageFrameDurationBucket = "under_8ms",
            maxFrameDurationBucket = "17_24ms",
            refreshRateBucket = "91_120hz",
        )
        var localSummary: WindowJankSummary? = null
        var uploadedSummary: WindowJankSummary? = null
        val reporter = DefaultWindowJankReporter(
            upload = { uploadedSummary = it },
            localLog = { localSummary = it },
        )

        reporter.report(WindowJankCollectionMode.LOCAL_ONLY, summary)
        assertSame(summary, localSummary)
        assertNull(uploadedSummary)

        localSummary = null
        reporter.report(WindowJankCollectionMode.UPLOAD, summary)
        assertNull(localSummary)
        assertSame(summary, uploadedSummary)
    }
}
