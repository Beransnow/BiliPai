package com.android.purebilibili.core.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import androidx.media3.common.Player
import com.android.purebilibili.core.ui.performance.PerformanceDebugCounter
import com.android.purebilibili.core.ui.performance.PerformanceObservability
import com.android.purebilibili.core.ui.performance.PerformanceTraceSection
import java.lang.ref.WeakReference
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong

/** Generation-scoped ownership of a Player instance. */
internal data class PlayerOwnerToken internal constructor(
    internal val player: Player,
    val owner: String,
    val generation: Long,
)

internal data class ReleaseFenceSnapshot internal constructor(
    internal val navigationEpoch: Long,
    internal val capturedAtUptimeMs: Long,
)

/**
 * Navigation settlement fence used by heavyweight player release.
 *
 * A committed pop and the new top entry reaching RESUMED settle an epoch. Callers still have a
 * bounded fallback, so Activity destruction or a non-navigation disposal cannot retain a player.
 */
internal class ReleaseFence(
    private val maxWaitMs: Long = DEFAULT_MAX_WAIT_MS,
) {
    private val navigationEpoch = AtomicLong(1L)
    private val poppedEpoch = AtomicLong(0L)
    private val resumedEpoch = AtomicLong(0L)
    private val lastPopUptimeMs = AtomicLong(Long.MIN_VALUE)
    private val lastTopResumeUptimeMs = AtomicLong(Long.MIN_VALUE)

    fun onNavigationEpochAdvanced() {
        navigationEpoch.incrementAndGet()
        PerformanceObservability.increment(PerformanceDebugCounter.NAVIGATION_EPOCH_ADVANCED)
    }

    fun onEntryPopped() {
        poppedEpoch.set(navigationEpoch.get())
        lastPopUptimeMs.set(SystemClock.uptimeMillis())
    }

    fun onTopEntryResumed() {
        resumedEpoch.set(navigationEpoch.get())
        lastTopResumeUptimeMs.set(SystemClock.uptimeMillis())
    }

    fun capture(): ReleaseFenceSnapshot = ReleaseFenceSnapshot(
        navigationEpoch = navigationEpoch.get(),
        capturedAtUptimeMs = SystemClock.uptimeMillis(),
    )

    fun isSettled(snapshot: ReleaseFenceSnapshot, nowUptimeMs: Long): Boolean {
        val epochSettled = poppedEpoch.get() >= snapshot.navigationEpoch &&
            resumedEpoch.get() >= snapshot.navigationEpoch
        if (epochSettled) return true
        return isReleaseFenceSettled(
            capturedAtUptimeMs = snapshot.capturedAtUptimeMs,
            nowUptimeMs = nowUptimeMs,
            lastPopUptimeMs = lastPopUptimeMs.get(),
            lastTopResumeUptimeMs = lastTopResumeUptimeMs.get(),
            maxWaitMs = maxWaitMs,
        )
    }

    private companion object {
        const val DEFAULT_MAX_WAIT_MS = 1_000L
    }
}

internal fun isReleaseFenceSettled(
    capturedAtUptimeMs: Long,
    nowUptimeMs: Long,
    lastPopUptimeMs: Long,
    lastTopResumeUptimeMs: Long,
    maxWaitMs: Long = 1_000L,
): Boolean {
    val navigationSettled =
        lastPopUptimeMs >= capturedAtUptimeMs - 250L &&
            lastTopResumeUptimeMs >= lastPopUptimeMs
    return navigationSettled || nowUptimeMs - capturedAtUptimeMs >= maxWaitMs
}

internal object PlayerReleaseFence {
    val navigation = ReleaseFence()
}

/**
 * Serializes heavyweight release on [Player.getApplicationLooper] and rejects stale owners.
 *
 * The registry owns no CoroutineScope. Release work is an explicit looper message whose validity
 * is checked again after the navigation fence and immediately before release.
 */
internal object PlayerLeaseRegistry {
    private data class LeaseRecord(
        var owner: String,
        var generation: Long,
        var released: Boolean = false,
    )

    private data class PendingTransfer(
        val owner: String,
        val requestedAtUptimeMs: Long,
    )

    private val records = IdentityHashMap<Player, LeaseRecord>()
    private val pendingTransfers = IdentityHashMap<Player, PendingTransfer>()
    private val releasedPlayerTombstones = ArrayList<WeakReference<Player>>()

    @Synchronized
    fun acquire(player: Player, owner: String): PlayerOwnerToken {
        check(!isReleasedPlayer(player)) { "A released player cannot acquire a new lease" }
        val previous = records[player]
        val generation = (previous?.generation ?: 0L) + 1L
        records[player] = LeaseRecord(
            owner = owner,
            generation = generation,
        )
        return PlayerOwnerToken(player = player, owner = owner, generation = generation)
    }

    @Synchronized
    fun transfer(token: PlayerOwnerToken, newOwner: String): PlayerOwnerToken? {
        val record = records[token.player] ?: return null
        if (!record.matches(token) || record.released) return null
        record.owner = newOwner
        record.generation += 1L
        return PlayerOwnerToken(
            player = token.player,
            owner = newOwner,
            generation = record.generation,
        )
    }

    /** Defers mini-player/PiP ownership transfer until NavEntry onPop confirms a committed pop. */
    @Synchronized
    fun transferOnNextCommittedPop(player: Player, newOwner: String) {
        val record = records[player] ?: return
        if (record.released) return
        pendingTransfers[player] = PendingTransfer(
            owner = newOwner,
            requestedAtUptimeMs = SystemClock.uptimeMillis(),
        )
    }

    @Synchronized
    fun commitPendingPopTransfers(nowUptimeMs: Long = SystemClock.uptimeMillis()) {
        val iterator = pendingTransfers.entries.iterator()
        while (iterator.hasNext()) {
            val (player, pending) = iterator.next()
            iterator.remove()
            if (nowUptimeMs - pending.requestedAtUptimeMs > PENDING_TRANSFER_TTL_MS) continue
            val record = records[player] ?: continue
            if (record.released) continue
            record.owner = pending.owner
            record.generation += 1L
        }
    }

    @Synchronized
    fun cancelPendingTransfer(player: Player) {
        pendingTransfers.remove(player)
    }

    fun requestReleaseCurrentOwner(
        player: Player,
        fence: ReleaseFence = PlayerReleaseFence.navigation,
        onReleased: (() -> Unit)? = null,
    ): Boolean {
        val token = synchronized(this) {
            val record = records[player]
            if (record == null || record.released) null else {
                PlayerOwnerToken(
                    player = player,
                    owner = record.owner,
                    generation = record.generation,
                )
            }
        } ?: return false
        requestRelease(token = token, fence = fence, onReleased = onReleased)
        return true
    }

    fun requestRelease(
        token: PlayerOwnerToken,
        fence: ReleaseFence = PlayerReleaseFence.navigation,
        snapshot: ReleaseFenceSnapshot = fence.capture(),
        onReleased: (() -> Unit)? = null,
    ) {
        PerformanceObservability.increment(PerformanceDebugCounter.PLAYER_RELEASE_REQUESTED)
        PerformanceObservability.trace(PerformanceTraceSection.PLAYER_RELEASE_REQUEST) {
            scheduleFencePoll(
                token = token,
                fence = fence,
                snapshot = snapshot,
                onReleased = onReleased,
            )
        }
    }

    private fun scheduleFencePoll(
        token: PlayerOwnerToken,
        fence: ReleaseFence,
        snapshot: ReleaseFenceSnapshot,
        onReleased: (() -> Unit)?,
    ) {
        val handler = Handler(token.player.applicationLooper)
        val poll = object : Runnable {
            override fun run() {
                if (!isCurrent(token)) return
                if (!fence.isSettled(snapshot, SystemClock.uptimeMillis())) {
                    handler.postDelayed(this, FENCE_POLL_INTERVAL_MS)
                    return
                }
                postAfterFrameOrLoop(handler) {
                    executeRelease(token = token, onReleased = onReleased)
                }
            }
        }
        handler.post(poll)
    }

    private fun postAfterFrameOrLoop(handler: Handler, block: () -> Unit) {
        handler.post {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Choreographer.getInstance().postFrameCallback {
                    handler.post(block)
                }
            } else {
                handler.post(block)
            }
        }
    }

    private fun executeRelease(token: PlayerOwnerToken, onReleased: (() -> Unit)?) {
        val shouldRelease = synchronized(this) {
            val record = records[token.player]
            if (record == null || record.released || !record.matches(token)) {
                false
            } else {
                record.released = true
                records.remove(token.player)
                pendingTransfers.remove(token.player)
                releasedPlayerTombstones += WeakReference(token.player)
                true
            }
        }
        if (!shouldRelease) return

        PerformanceObservability.trace(PerformanceTraceSection.PLAYER_RELEASE_EXECUTION) {
            token.player.release()
        }
        PerformanceObservability.increment(PerformanceDebugCounter.PLAYER_RELEASE_EXECUTED)
        onReleased?.invoke()
    }

    @Synchronized
    private fun isCurrent(token: PlayerOwnerToken): Boolean {
        val record = records[token.player] ?: return false
        return !record.released && record.matches(token)
    }

    private fun LeaseRecord.matches(token: PlayerOwnerToken): Boolean =
        owner == token.owner && generation == token.generation

    private fun isReleasedPlayer(player: Player): Boolean {
        val iterator = releasedPlayerTombstones.iterator()
        while (iterator.hasNext()) {
            val releasedPlayer = iterator.next().get()
            if (releasedPlayer == null) {
                iterator.remove()
            } else if (releasedPlayer === player) {
                return true
            }
        }
        return false
    }

    private const val FENCE_POLL_INTERVAL_MS = 16L
    private const val PENDING_TRANSFER_TTL_MS = 1_000L
}

/** Pure generation policy used by unit tests and debug assertions. */
internal class PlayerLeaseGenerationState {
    private var owner: String? = null
    private var generation = 0L
    private var released = false

    fun acquire(newOwner: String): LeaseGeneration {
        check(!released) { "A released generation is a tombstone" }
        generation += 1L
        owner = newOwner
        released = false
        return LeaseGeneration(newOwner, generation)
    }

    fun transfer(current: LeaseGeneration, newOwner: String): LeaseGeneration? {
        if (!isCurrent(current)) return null
        generation += 1L
        owner = newOwner
        return LeaseGeneration(newOwner, generation)
    }

    fun release(current: LeaseGeneration): Boolean {
        if (!isCurrent(current)) return false
        released = true
        return true
    }

    fun isCurrent(candidate: LeaseGeneration): Boolean =
        !released && owner == candidate.owner && generation == candidate.generation
}

internal data class LeaseGeneration(
    val owner: String,
    val generation: Long,
)
