package com.android.purebilibili.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/** A launch decision is immutable for the lifetime of one process launch. */
internal data class StartupLaunchDecision(
    val onboardingRequired: Boolean,
    val openPortraitFeedOnStartup: Boolean,
    val usedTimeoutFallback: Boolean = false,
)

/**
 * Resolves the DataStore-backed launch preference without ever blocking the main thread.
 *
 * The synchronous SharedPreferences value is only a launch-time mirror. DataStore is still read
 * on every process start and repairs that mirror for the next launch. Once [freezeDecision] has
 * returned, a late authoritative value cannot redirect the current navigation stack.
 */
internal class StartupSessionCoordinator(
    val sessionStartedAtMs: Long,
    private val readMirror: () -> Boolean?,
    private val readAuthoritative: suspend () -> Boolean,
    private val writeMirror: (Boolean) -> Unit,
) {
    private val started = AtomicBoolean(false)
    private val resolvedPreference = CompletableDeferred<Boolean>()
    private val decisionMutex = Mutex()

    @Volatile
    private var frozenDecision: StartupLaunchDecision? = null

    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                readMirror()?.let { mirrored ->
                    resolvedPreference.complete(mirrored)
                }

                val authoritative = readAuthoritative()
                writeMirror(authoritative)
                resolvedPreference.complete(authoritative)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // A missing/unreadable preference is equivalent to its safe default. Completing
                // here also prevents callers from needlessly holding the system splash to timeout.
                resolvedPreference.complete(false)
            }
        }
    }

    suspend fun freezeDecision(
        onboardingRequired: Boolean,
        timeoutMs: Long,
    ): StartupLaunchDecision = decisionMutex.withLock {
        frozenDecision?.let { return@withLock it }

        val decision = if (onboardingRequired) {
            // Onboarding owns the first route and never waits for an unrelated feed preference.
            StartupLaunchDecision(
                onboardingRequired = true,
                openPortraitFeedOnStartup = false,
            )
        } else {
            val immediatelyAvailable = if (resolvedPreference.isCompleted) {
                resolvedPreference.await()
            } else {
                null
            }
            val resolved = immediatelyAvailable ?: if (timeoutMs > 0L) {
                withTimeoutOrNull(timeoutMs) { resolvedPreference.await() }
            } else {
                null
            }
            StartupLaunchDecision(
                onboardingRequired = false,
                openPortraitFeedOnStartup = resolved ?: false,
                usedTimeoutFallback = resolved == null,
            )
        }

        frozenDecision = decision
        decision
    }

    fun remainingDecisionTimeMs(nowMs: Long, totalDeadlineMs: Long): Long {
        return (totalDeadlineMs - (nowMs - sessionStartedAtMs)).coerceAtLeast(0L)
    }

    fun currentDecisionOrNull(): StartupLaunchDecision? = frozenDecision
}
