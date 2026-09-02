package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

class MiuixVideoCardNavTransitionTest {
    @Test
    fun lifecycleMotionRetainsConfiguredDuration() {
        val spec = resolveVideoHeroMotionSpec(360)
        val entering = resolveVideoHeroNavMotion(spec, returning = false)
        val retentionSpec = spec.copy(returnDurationMillis = 360, cancelDurationMillis = 360)
        val returning = resolveVideoHeroNavMotion(
            retentionSpec,
            returning = true,
        )

        assertEquals(360, (entering.programmatic as NavSettleSpec.Tween).durationMillis)
        assertEquals(360, (returning.programmatic as NavSettleSpec.Tween).durationMillis)
        assertEquals(retentionSpec.commitStiffness, (returning.commit as NavSettleSpec.Spring).stiffness)
    }

    @Test
    fun retainedGestureMetadataDoesNotMisclassifyCommitOrCancelAsSeek() {
        val scope = object : NavTransitionScope {
            override var relativeDepth = -.4f
            override var role = NavRole.Top
            override val change = NavChange.Pop
            override val layoutSize = IntSize(1080, 2400)
            override val layoutDirection = LayoutDirection.Ltr
            override val density = Density(3f)
            override val gesture = NavGesture(.4f, NavSwipeEdge.Left, 500f)
            override var settle: NavSettle? = null
        }
        val progress = MiuixVideoCardTransitionProgress()
        progress.bind(scope)
        assertTrue(progress.isGestureInProgress())

        scope.settle = object : NavSettle {
            override val phase = NavSettlePhase.Cancel
            override val releaseVelocity = 0f
            override val elapsedMillis = 0f
        }
        assertFalse(progress.isGestureInProgress())
        assertEquals(VideoCardTransitionSettleState.CancelRestore, progress.settleStateOrNull())

        scope.settle = object : NavSettle {
            override val phase = NavSettlePhase.Commit
            override val releaseVelocity = 2f
            override val elapsedMillis = 0f
        }
        scope.role = NavRole.Outgoing
        assertEquals(VideoCardTransitionSettleState.AutoReturn, progress.settleStateOrNull())
    }

    @Test
    fun miuixTransitionContainsNoCardGeometryOrLandingPulse() {
        val source = File(
            "src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt",
        ).readText()

        assertTrue(source.contains("miuixSharedElementNavTransition"))
        assertTrue(source.contains("fallback.scrimFraction"))
        assertFalse(source.contains("sourceBounds"))
        assertFalse(source.contains("scaleX ="))
        assertFalse(source.contains("translationX ="))
        assertFalse(source.contains("resolveVideoHeroLandingScale"))
        assertFalse(source.contains("ContentCompensation"))
    }
}
