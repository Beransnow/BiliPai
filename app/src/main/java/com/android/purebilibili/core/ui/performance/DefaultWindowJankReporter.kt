package com.android.purebilibili.core.ui.performance

import android.util.Log

/**
 * Default delivery split: local builds log only the fixed anonymous aggregate, while sampled
 * release builds delegate to the app's Analytics entry point.
 */
class DefaultWindowJankReporter(
    private val upload: (WindowJankSummary) -> Unit,
    private val localLog: (WindowJankSummary) -> Unit = ::logWindowJankSummary,
) : WindowJankReporter {

    override fun report(mode: WindowJankCollectionMode, summary: WindowJankSummary) {
        when (mode) {
            WindowJankCollectionMode.DISABLED -> Unit
            WindowJankCollectionMode.LOCAL_ONLY -> localLog(summary)
            WindowJankCollectionMode.UPLOAD -> upload(summary)
        }
    }
}

private fun logWindowJankSummary(summary: WindowJankSummary) {
    Log.d(
        "WindowJankController",
        buildString(capacity = 192) {
            append("route=")
            append(summary.route)
            append(" interaction=")
            append(summary.interaction)
            append(" frames=")
            append(summary.frameCount)
            append(" jank=")
            append(summary.jankCount)
            append(" avg=")
            append(summary.averageFrameDurationBucket)
            append(" max=")
            append(summary.maxFrameDurationBucket)
            append(" refresh=")
            append(summary.refreshRateBucket)
        }
    )
}
