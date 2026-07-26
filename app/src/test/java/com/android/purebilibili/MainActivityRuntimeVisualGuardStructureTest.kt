package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MainActivityRuntimeVisualGuardStructureTest {

    @Test
    fun jankStatsFollowsActivityStartedLifecycleAndDiscardsInterruptedWindow() {
        val source = mainActivitySource()
        val onStartBlock = source.substringAfter("override fun onStart()").substringBefore("override fun onStop()")
        val onStopBlock = source.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        val onDestroyBlock = source.substringAfter("override fun onDestroy()").substringBefore("\n    }")

        assertTrue(onStartBlock.contains("JankStats.createAndTrack(window)"))
        assertTrue(onStartBlock.contains("SystemClock.uptimeMillis()"))
        assertTrue(onStopBlock.contains("AppRuntimeVisualGuardTracker.discardActiveWindow()"))
        assertTrue(onStopBlock.contains("isTrackingEnabled = false"))
        assertTrue(onDestroyBlock.contains("runtimeJankStats = null"))
    }

    private fun mainActivitySource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/MainActivity.kt"),
            File("src/main/java/com/android/purebilibili/MainActivity.kt"),
        ).first { it.exists() }.readText()
    }
}
