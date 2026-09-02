package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import com.android.purebilibili.core.ui.transition.VideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.VideoHeroMotionTokens
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

/** Coarse bridge to Miuix's lifecycle; it never supplies shared-element geometry. */
internal class MiuixVideoCardTransitionProgress {
    private var topScope: NavTransitionScope? by mutableStateOf(null)

    fun bind(scope: NavTransitionScope) {
        when (scope.role) {
            NavRole.Incoming,
            NavRole.Outgoing,
            -> topScope = scope
            NavRole.Top -> if (topScope == null || topScope?.role == NavRole.Covered) {
                topScope = scope
            }
            NavRole.Covered -> Unit
        }
    }

    fun isGestureInProgress(): Boolean = topScope?.let {
        it.gesture != null && it.settle == null
    } == true

    fun releaseVelocity(): Float = topScope?.settle?.releaseVelocity ?: 0f

    fun settleStateOrNull(): VideoCardTransitionSettleState? = topScope?.let { scope ->
        when {
            scope.settle?.phase == NavSettlePhase.Cancel ->
                VideoCardTransitionSettleState.CancelRestore
            isGestureInProgress() -> VideoCardTransitionSettleState.InteractiveSeek
            scope.role == NavRole.Outgoing && scope.relativeDepth <= -1f ->
                VideoCardTransitionSettleState.Idle
            scope.settle?.phase == NavSettlePhase.Commit || scope.role == NavRole.Outgoing ->
                VideoCardTransitionSettleState.AutoReturn
            scope.role == NavRole.Incoming -> VideoCardTransitionSettleState.AutoEnter
            else -> VideoCardTransitionSettleState.Held
        }
    }

    fun gestureBackProgress(): Float? =
        topScope?.gesture?.progress?.takeIf { isGestureInProgress() }
}

internal fun resolveVideoHeroNavMotion(
    spec: VideoHeroMotionSpec,
    returning: Boolean,
): NavMotion = NavMotion(
    commit = NavSettleSpec.Spring(
        dampingRatio = VideoHeroMotionTokens.SPRING_DAMPING,
        stiffness = spec.commitStiffness,
    ),
    cancel = NavSettleSpec.Spring(
        dampingRatio = VideoHeroMotionTokens.SPRING_DAMPING,
        stiffness = spec.cancelStiffness,
    ),
    programmatic = NavSettleSpec.Tween(
        durationMillis = if (returning) spec.returnDurationMillis else spec.enterDurationMillis,
        easing = if (returning) spec.returnSpatialSpec else spec.enterSpatialSpec,
    ),
)

/**
 * Lifecycle-only Miuix transition for video entries.
 *
 * Programmatic push/pop and a committed predictive back use an identity entry transform, leaving
 * geometry exclusively to Compose `sharedBounds`. While the gesture is directly manipulated (or
 * cancelling), the normal Miuix transition is delegated so the user still sees a realtime page
 * preview. No card bounds, inverse scaling, translation, clipping, or landing pulse is calculated
 * here.
 */
internal fun miuixSharedElementNavTransition(
    durationMillis: Int,
    fallback: NavTransition,
    progress: MiuixVideoCardTransitionProgress,
    heroMotionSpec: VideoHeroMotionSpec = resolveVideoHeroMotionSpec(durationMillis),
    returningProvider: () -> Boolean = { false },
): NavTransition {
    // NavDisplay owns entry retention. Keep its identity settle at least as long as sharedBounds so
    // it cannot unload the outgoing detail while the shared overlay still owns live media.
    val retentionMotionSpec = heroMotionSpec.copy(
        returnDurationMillis = maxOf(heroMotionSpec.returnDurationMillis, durationMillis),
        cancelDurationMillis = maxOf(heroMotionSpec.cancelDurationMillis, durationMillis),
    )
    val enterMotion = resolveVideoHeroNavMotion(retentionMotionSpec, returning = false)
    val returnMotion = resolveVideoHeroNavMotion(retentionMotionSpec, returning = true)

    return object : NavTransition {
        override val opaqueDepth: Float = fallback.opaqueDepth
        override val motion: NavMotion
            get() = if (returningProvider()) returnMotion else enterMotion

        private fun usesRealtimeMiuixPreview(scope: NavTransitionScope): Boolean =
            scope.gesture != null && scope.settle?.phase != NavSettlePhase.Commit

        override fun scrimFraction(scope: NavTransitionScope): Float =
            if (usesRealtimeMiuixPreview(scope)) fallback.scrimFraction(scope) else 0f

        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            progress.bind(scope)
            return if (usesRealtimeMiuixPreview(scope)) {
                with(fallback) { this@transformEntry.transformEntry(scope) }
            } else {
                this
            }
        }
    }
}
