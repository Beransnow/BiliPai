package com.android.purebilibili.core.ui.performance

import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

private const val RELEASE_SAMPLE_RATE = 0.01
internal const val WINDOW_JANK_MIN_FRAME_COUNT = 60L
internal const val WINDOW_JANK_ROUTE_WINDOW_MS = 10_000L
internal const val WINDOW_JANK_MAX_REPORTS_PER_PROCESS = 12

private val ROUTE_TOKEN_SEPARATOR = Regex("[^a-z0-9_]+")

/** Controls whether a window records locally or is eligible for anonymous upload. */
enum class WindowJankCollectionMode {
    DISABLED,
    LOCAL_ONLY,
    UPLOAD,
}

/**
 * Upload-safe summary. It intentionally contains no raw route, URL, title, content ID, user data,
 * or raw timing value.
 */
data class WindowJankSummary(
    val route: String,
    val interaction: String,
    val frameCount: Long,
    val jankCount: Long,
    val averageFrameDurationBucket: String,
    val maxFrameDurationBucket: String,
    val refreshRateBucket: String,
)

fun interface WindowJankReporter {
    fun report(mode: WindowJankCollectionMode, summary: WindowJankSummary)
}

internal fun resolveWindowJankCollectionMode(
    buildType: String,
    analyticsEnabled: Boolean,
    releaseSampleValue: Double,
): WindowJankCollectionMode {
    return when (buildType.trim().lowercase(Locale.ROOT)) {
        "debug", "dev", "smooth" -> WindowJankCollectionMode.LOCAL_ONLY
        "release" -> {
            if (analyticsEnabled && releaseSampleValue in 0.0..<RELEASE_SAMPLE_RATE) {
                WindowJankCollectionMode.UPLOAD
            } else {
                WindowJankCollectionMode.DISABLED
            }
        }
        else -> WindowJankCollectionMode.DISABLED
    }
}

/**
 * Maps a potentially parameterized navigation route onto a small fixed vocabulary. Returning null
 * disables collection for live destinations.
 */
internal fun normalizeWindowJankRoute(rawRoute: String?): String? {
    val routePath = rawRoute
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.trim('/')
        ?.lowercase(Locale.ROOT)
        ?: return null

    val token = routePath
        .replace(ROUTE_TOKEN_SEPARATOR, "_")
        .trim('_')

    if (
        routePath == "live" ||
        routePath.startsWith("live/") ||
        token == "live" ||
        token.startsWith("live_") ||
        token.startsWith("liveroute") ||
        token.startsWith("liveroom")
    ) {
        return null
    }

    return when {
        routePath == "home" || token.startsWith("home") -> "home"
        routePath == "dynamic" || token == "dynamickey" -> "dynamic"
        routePath.startsWith("dynamic_detail") || token.startsWith("dynamicdetail") -> "dynamic_detail"
        routePath == "search" || token == "searchkey" -> "search"
        routePath.startsWith("search_trending") || token.startsWith("searchtrending") -> "search_trending"
        routePath == "profile" || token.startsWith("profile") -> "profile"
        routePath.startsWith("space/") || token.startsWith("space") -> "space"
        routePath == "video_detail" ||
            routePath.startsWith("video/") ||
            routePath.startsWith("video_player/") ||
            token.startsWith("videoplayer") -> "video_detail"
        routePath.startsWith("offline_video/") || token.startsWith("offlinevideo") -> "offline_player"
        routePath.startsWith("external_media/") || token.startsWith("externalmedia") -> "external_player"
        routePath.startsWith("story") || token.startsWith("story") -> "story_player"
        routePath.startsWith("bangumi/play/") || token.startsWith("bangumiplayer") -> "bangumi_player"
        routePath.startsWith("bangumi/") || token.startsWith("bangumidetail") -> "bangumi_detail"
        routePath == "bangumi" || token == "bangumikey" -> "bangumi"
        routePath.startsWith("web") || token.startsWith("webroute") -> "web"
        routePath.startsWith("article/") || token.startsWith("article") -> "article"
        routePath == "partition" || token.startsWith("partition") -> "partition"
        routePath.startsWith("category/") || token.startsWith("category") -> "category"
        routePath == "onboarding" || token.startsWith("onboarding") -> "onboarding"
        routePath.startsWith("appearance_settings") -> "appearance_settings"
        routePath.startsWith("playback_settings") -> "playback_settings"
        routePath.endsWith("settings") || routePath == "settings" -> "settings"
        routePath.startsWith("download") || token.startsWith("download") -> "downloads"
        routePath.startsWith("history") -> "history"
        routePath.startsWith("favorite") -> "favorites"
        routePath.startsWith("watch_later") -> "watch_later"
        routePath.startsWith("inbox") || routePath.startsWith("message/") -> "messages"
        routePath.startsWith("chat/") -> "chat"
        routePath.startsWith("music/") || routePath.startsWith("native_music") -> "music"
        else -> "other"
    }
}

internal enum class WindowJankInteraction(val wireName: String) {
    IDLE("idle"),
    SCROLL("scroll"),
    NAVIGATION("navigation"),
    PREDICTIVE_BACK("predictive_back"),
    PLAYER_CONTROLS("player_controls"),
    DANMAKU("danmaku"),
    IMAGE_PREVIEW("image_preview"),
    PULL_REFRESH("pull_refresh"),
    OTHER("other"),
}

internal fun normalizeWindowJankInteraction(rawInteraction: String?): WindowJankInteraction {
    val value = rawInteraction
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    return when {
        value.isEmpty() || value == "idle" -> WindowJankInteraction.IDLE
        "predictive" in value && "back" in value -> WindowJankInteraction.PREDICTIVE_BACK
        "scroll" in value || "fling" in value -> WindowJankInteraction.SCROLL
        "navigation" in value || value == "navigate" || value == "route" -> WindowJankInteraction.NAVIGATION
        "player" in value || "control" in value -> WindowJankInteraction.PLAYER_CONTROLS
        "danmaku" in value -> WindowJankInteraction.DANMAKU
        "image" in value && ("preview" in value || "zoom" in value || "drag" in value) -> {
            WindowJankInteraction.IMAGE_PREVIEW
        }
        "pull" in value && "refresh" in value -> WindowJankInteraction.PULL_REFRESH
        else -> WindowJankInteraction.OTHER
    }
}

internal fun resolveFrameDurationBucket(durationNanos: Long): String {
    val durationMicros = durationNanos.coerceAtLeast(0L) / 1_000L
    return when {
        durationMicros < 8_000L -> "under_8ms"
        durationMicros < 12_000L -> "8_12ms"
        durationMicros < 16_700L -> "12_17ms"
        durationMicros < 24_000L -> "17_24ms"
        durationMicros < 40_000L -> "24_40ms"
        durationMicros < 64_000L -> "40_64ms"
        else -> "over_64ms"
    }
}

internal fun resolveRefreshRateBucket(refreshRateHz: Float): String {
    if (!refreshRateHz.isFinite() || refreshRateHz <= 0f) return "unknown"
    return when {
        refreshRateHz <= 60.5f -> "up_to_60hz"
        refreshRateHz <= 90.5f -> "61_90hz"
        refreshRateHz <= 120.5f -> "91_120hz"
        else -> "over_120hz"
    }
}

/** Mutable primitive-only storage used directly from the per-frame listener. */
internal class WindowJankPrimitiveAccumulator {
    private var frameCount = 0L
    private var jankCount = 0L
    private var totalFrameDurationNanos = 0L
    private var maxFrameDurationNanos = 0L
    private val interactionFrameCounts = LongArray(WindowJankInteraction.entries.size)

    fun record(
        frameDurationUiNanos: Long,
        isJank: Boolean,
        interactionOrdinal: Int,
    ) {
        val duration = frameDurationUiNanos.coerceAtLeast(0L)
        frameCount += 1L
        if (isJank) jankCount += 1L
        totalFrameDurationNanos = saturatedAdd(totalFrameDurationNanos, duration)
        if (duration > maxFrameDurationNanos) maxFrameDurationNanos = duration
        val safeInteractionOrdinal = if (
            interactionOrdinal >= 0 && interactionOrdinal < interactionFrameCounts.size
        ) {
            interactionOrdinal
        } else {
            WindowJankInteraction.OTHER.ordinal
        }
        interactionFrameCounts[safeInteractionOrdinal] += 1L
    }

    fun snapshotAndReset(
        route: String,
        refreshRateHz: Float,
    ): WindowJankSummary? {
        if (frameCount == 0L) return null

        var dominantInteractionOrdinal = WindowJankInteraction.IDLE.ordinal
        var dominantInteractionFrames = Long.MIN_VALUE
        interactionFrameCounts.forEachIndexed { ordinal, count ->
            if (count > dominantInteractionFrames) {
                dominantInteractionOrdinal = ordinal
                dominantInteractionFrames = count
            }
        }
        val averageDurationNanos = (totalFrameDurationNanos.toDouble() / frameCount).roundToLong()
        val result = WindowJankSummary(
            route = route,
            interaction = WindowJankInteraction.entries[dominantInteractionOrdinal].wireName,
            frameCount = frameCount,
            jankCount = jankCount,
            averageFrameDurationBucket = resolveFrameDurationBucket(averageDurationNanos),
            maxFrameDurationBucket = resolveFrameDurationBucket(maxFrameDurationNanos),
            refreshRateBucket = resolveRefreshRateBucket(refreshRateHz),
        )
        reset()
        return result
    }

    fun reset() {
        frameCount = 0L
        jankCount = 0L
        totalFrameDurationNanos = 0L
        maxFrameDurationNanos = 0L
        interactionFrameCounts.fill(0L)
    }

    private fun saturatedAdd(left: Long, right: Long): Long {
        return if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
    }
}

/** Enforces both the minimum useful sample size and the process report budget. */
internal class WindowJankReportLimiter(
    private val minimumFrameCount: Long = WINDOW_JANK_MIN_FRAME_COUNT,
    private val maximumReportCount: Int = WINDOW_JANK_MAX_REPORTS_PER_PROCESS,
) {
    private val reportCount = AtomicInteger(0)

    init {
        require(minimumFrameCount > 0L)
        require(maximumReportCount > 0)
    }

    fun tryAcquire(frameCount: Long): Boolean {
        if (frameCount < minimumFrameCount) return false
        while (true) {
            val current = reportCount.get()
            if (current >= maximumReportCount) return false
            if (reportCount.compareAndSet(current, current + 1)) return true
        }
    }
}
