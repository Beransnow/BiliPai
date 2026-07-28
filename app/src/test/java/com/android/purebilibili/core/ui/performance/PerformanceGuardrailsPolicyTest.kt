package com.android.purebilibili.core.ui.performance

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerformanceGuardrailsPolicyTest {

    @Test
    fun strictModeRunsOnlyInDebugAndDev() {
        assertTrue(shouldEnablePerformanceStrictMode("debug"))
        assertTrue(shouldEnablePerformanceStrictMode("dev"))
        assertFalse(shouldEnablePerformanceStrictMode("smooth"))
        assertFalse(shouldEnablePerformanceStrictMode("release"))
    }

    @Test
    fun localCountersIncludeSmoothButExcludeRelease() {
        assertTrue(shouldEnableLocalPerformanceCounters("debug"))
        assertTrue(shouldEnableLocalPerformanceCounters("dev"))
        assertTrue(shouldEnableLocalPerformanceCounters("smooth"))
        assertFalse(shouldEnableLocalPerformanceCounters("release"))
    }

    @Test
    fun strictModeUsesLoggingPenaltiesOnly() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/performance/PerformanceGuardrails.kt"
        )
        assertTrue(source.contains(".detectDiskReads()"))
        assertTrue(source.contains(".detectDiskWrites()"))
        assertTrue(source.contains(".detectNetwork()"))
        assertTrue(source.contains(".detectLeakedClosableObjects()"))
        assertTrue(source.contains(".penaltyLog()"))
        assertFalse(source.contains("penaltyDeath"))
    }

    @Test
    fun composeReportsAreOptInByGradleProperty() {
        val source = loadSource("app/build.gradle.kts")
        assertTrue(source.contains("gradleProperty(\"bili.composeReports\")"))
        assertTrue(source.contains("if (composeReportsEnabled)"))
        assertTrue(source.contains("reportsDestination = layout.buildDirectory.dir(\"compose_reports\")"))
        assertTrue(source.contains("metricsDestination = layout.buildDirectory.dir(\"compose_metrics\")"))
    }

    @Test
    fun composeReportsWorkflowChecksSecretsBeforeTheConditionalJob() {
        val source = loadSource(".github/workflows/ComposeReports.yml")
        assertTrue(source.contains("check-fcm:"))
        assertTrue(source.contains("needs: check-fcm"))
        assertTrue(source.contains("if: needs.check-fcm.outputs.has_fcm == 'true'"))
        assertFalse(source.contains("if: \${{ secrets.FCM"))
    }

    private fun loadSource(path: String): String {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized), File("../$path"))
            .first { it.exists() }
            .readText()
    }
}
