package com.android.purebilibili.core.ui.performance

import android.os.StrictMode
import android.os.Trace
import com.android.purebilibili.BuildConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicLongArray

internal fun shouldEnablePerformanceStrictMode(buildType: String): Boolean {
    return when (buildType.trim().lowercase(Locale.ROOT)) {
        "debug", "dev" -> true
        else -> false
    }
}

internal fun shouldEnableLocalPerformanceCounters(buildType: String): Boolean {
    return when (buildType.trim().lowercase(Locale.ROOT)) {
        "debug", "dev", "smooth" -> true
        else -> false
    }
}

/** Debug/dev-only StrictMode policies. No death penalties are installed. */
object PerformanceStrictMode {
    @Volatile
    private var installed = false

    @JvmStatic
    fun installIfEnabled(buildType: String = BuildConfig.BUILD_TYPE) {
        if (!shouldEnablePerformanceStrictMode(buildType) || installed) return
        synchronized(this) {
            if (installed) return
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectResourceMismatches()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
            installed = true
        }
    }
}

enum class PerformanceTraceSection(internal val traceName: String) {
    STARTUP_TASK("perf.startup.task"),
    DANMAKU_CLOCK("perf.danmaku.clock"),
    PLAYER_RELEASE_REQUEST("perf.player.release.request"),
    PLAYER_RELEASE_EXECUTION("perf.player.release.execute"),
    NAVIGATION_EPOCH("perf.navigation.epoch"),
}

enum class PerformanceDebugCounter {
    STARTUP_TASK_TRIGGERED,
    STARTUP_TASK_COMPLETED,
    DANMAKU_CLOCK_STARTED,
    DANMAKU_CLOCK_STOPPED,
    PLAYER_RELEASE_REQUESTED,
    PLAYER_RELEASE_EXECUTED,
    NAVIGATION_EPOCH_ADVANCED,
}

/** Fixed-name trace sections and allocation-free process-local counters for performance diagnostics. */
object PerformanceObservability {
    private val counters = AtomicLongArray(PerformanceDebugCounter.entries.size)

    @JvmStatic
    fun begin(section: PerformanceTraceSection) {
        Trace.beginSection(section.traceName)
    }

    @JvmStatic
    fun end() {
        Trace.endSection()
    }

    inline fun <T> trace(section: PerformanceTraceSection, block: () -> T): T {
        begin(section)
        return try {
            block()
        } finally {
            end()
        }
    }

    @JvmStatic
    fun increment(
        counter: PerformanceDebugCounter,
        delta: Long = 1L,
        buildType: String = BuildConfig.BUILD_TYPE,
    ): Long {
        if (!shouldEnableLocalPerformanceCounters(buildType)) return 0L
        return counters.addAndGet(counter.ordinal, delta)
    }

    @JvmStatic
    fun value(counter: PerformanceDebugCounter): Long {
        return counters.get(counter.ordinal)
    }
}
