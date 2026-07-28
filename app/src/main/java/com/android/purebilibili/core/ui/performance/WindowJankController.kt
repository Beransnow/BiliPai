package com.android.purebilibili.core.ui.performance

import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.annotation.MainThread
import androidx.metrics.performance.JankStats
import java.util.concurrent.ThreadLocalRandom

private object WindowJankProcessSampling {
    private val releaseSampleValue: Double by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ThreadLocalRandom.current().nextDouble()
    }

    fun resolveMode(buildType: String, analyticsEnabled: Boolean): WindowJankCollectionMode {
        return resolveWindowJankCollectionMode(
            buildType = buildType,
            analyticsEnabled = analyticsEnabled,
            releaseSampleValue = releaseSampleValue,
        )
    }
}

private object WindowJankProcessReportQuota {
    private val limiter = WindowJankReportLimiter()

    fun tryAcquire(frameCount: Long): Boolean {
        return limiter.tryAcquire(frameCount)
    }
}

/**
 * Owns one JankStats listener for one Window. Activity lifecycle and route changes must be forwarded
 * through the methods below; no Activity or navigation object is retained.
 */
class WindowJankController private constructor(
    private val window: Window,
    private val collectionMode: WindowJankCollectionMode,
    private val reporter: WindowJankReporter,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val accumulator = WindowJankPrimitiveAccumulator()
    private val routeWindowFlush = Runnable {
        flush()
        scheduleRouteWindowFlushIfNeeded()
    }

    private var jankStats: JankStats? = null
    private var normalizedRoute: String? = null
    private var interactionOrdinal = WindowJankInteraction.IDLE.ordinal
    private var isWindowResumed = false
    private var isClosed = false

    @MainThread
    fun onWindowResumed(route: String?) {
        if (isClosed) return
        isWindowResumed = true
        updateRoute(route)
        updateTrackingState()
        scheduleRouteWindowFlushIfNeeded()
    }

    @MainThread
    fun onRouteChanged(route: String?) {
        if (isClosed) return
        updateRoute(route)
    }

    @MainThread
    fun onInteractionChanged(interaction: String?) {
        interactionOrdinal = normalizeWindowJankInteraction(interaction).ordinal
    }

    @MainThread
    fun onWindowPaused() {
        if (isClosed) return
        isWindowResumed = false
        cancelRouteWindowFlush()
        flush()
        updateTrackingState()
    }

    @MainThread
    fun onAppBackgrounded() {
        onWindowPaused()
    }

    @MainThread
    fun close() {
        if (isClosed) return
        isClosed = true
        isWindowResumed = false
        cancelRouteWindowFlush()
        flush()
        jankStats?.isTrackingEnabled = false
        jankStats = null
    }

    private fun updateRoute(rawRoute: String?) {
        val nextRoute = normalizeWindowJankRoute(rawRoute)
        if (nextRoute == normalizedRoute) return

        cancelRouteWindowFlush()
        flush()
        normalizedRoute = nextRoute
        interactionOrdinal = WindowJankInteraction.IDLE.ordinal
        updateTrackingState()
        scheduleRouteWindowFlushIfNeeded()
    }

    private fun ensureJankStats(): JankStats? {
        if (collectionMode == WindowJankCollectionMode.DISABLED) return null
        jankStats?.let { return it }
        return try {
            JankStats.createAndTrack(window) { frameData ->
                // Keep this callback allocation-free: only primitive fields are read and updated.
                accumulator.record(
                    frameDurationUiNanos = frameData.frameDurationUiNanos,
                    isJank = frameData.isJank,
                    interactionOrdinal = interactionOrdinal,
                )
            }.also { stats ->
                stats.jankHeuristicMultiplier = 2f
                jankStats = stats
            }
        } catch (_: IllegalStateException) {
            // A decor view may not exist before the Window is attached. The next resume retries.
            null
        }
    }

    private fun updateTrackingState() {
        val shouldTrack =
            !isClosed &&
                isWindowResumed &&
                normalizedRoute != null &&
                collectionMode != WindowJankCollectionMode.DISABLED
        if (shouldTrack) {
            ensureJankStats()?.isTrackingEnabled = true
        } else {
            jankStats?.isTrackingEnabled = false
        }
    }

    private fun scheduleRouteWindowFlushIfNeeded() {
        cancelRouteWindowFlush()
        if (
            !isClosed &&
            isWindowResumed &&
            normalizedRoute != null &&
            collectionMode != WindowJankCollectionMode.DISABLED
        ) {
            mainHandler.postDelayed(routeWindowFlush, WINDOW_JANK_ROUTE_WINDOW_MS)
        }
    }

    private fun cancelRouteWindowFlush() {
        mainHandler.removeCallbacks(routeWindowFlush)
    }

    private fun flush() {
        val route = normalizedRoute
        if (route == null) {
            accumulator.reset()
            return
        }
        val summary = accumulator.snapshotAndReset(
            route = route,
            refreshRateHz = window.decorView.display?.refreshRate ?: 0f,
        ) ?: return
        if (!WindowJankProcessReportQuota.tryAcquire(summary.frameCount)) return
        reporter.report(collectionMode, summary)
    }

    companion object {
        /**
         * Creates a controller with a process-stable sampling decision. Release collection requires
         * Analytics consent and a 1% sample; debug/dev/smooth are local-only at 100%.
         */
        @JvmStatic
        fun create(
            window: Window,
            buildType: String,
            analyticsEnabled: Boolean,
            reporter: WindowJankReporter,
        ): WindowJankController {
            return WindowJankController(
                window = window,
                collectionMode = WindowJankProcessSampling.resolveMode(buildType, analyticsEnabled),
                reporter = reporter,
            )
        }
    }
}
