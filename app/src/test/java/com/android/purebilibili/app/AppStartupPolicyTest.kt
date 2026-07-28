package com.android.purebilibili.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStartupPolicyTest {

    @Test
    fun startupPlanHasValidExplicitDependencyGraph() {
        val tasks = defaultAppStartupTasks(
            sdkInt = 34,
            deferredDelayMs = 900L,
            dex2OatDelayMs = 2_500L,
        )

        assertTrue(tasks.isNotEmpty())
        assertTrue(validateStartupTaskGraph(tasks))
        assertTrue(tasks.all { it.id.isNotBlank() && it.delayMs >= 0L })
    }

    @Test
    fun appCreatePlanMovesDiskRestoreOffMainAndOrdersFeedPolicyBeforePreload() {
        val tasks = defaultAppStartupTasks(sdkInt = 34).associateBy { it.id }

        assertEquals(StartupTrigger.APP_CREATE, tasks.getValue("network_module_init").trigger)
        assertEquals(StartupDispatcher.IO, tasks.getValue("token_manager_init").dispatcher)
        assertEquals(StartupDispatcher.IO, tasks.getValue("wbi_key_restore").dispatcher)
        assertEquals(setOf("token_manager_init"), tasks.getValue("wbi_key_restore").dependencies)
        assertEquals(StartupDispatcher.IO, tasks.getValue("player_settings_cache_init").dispatcher)
        assertEquals(StartupTrigger.APP_CREATE, tasks.getValue("home_feed_policy_restore").trigger)
        assertEquals(
            setOf("plugin_manager_configure"),
            tasks.getValue("home_feed_policy_restore").dependencies,
        )
        assertEquals(StartupTrigger.APP_CREATE, tasks.getValue("download_manager_configure").trigger)
    }

    @Test
    fun firstInteractivePlanDefersHeavyRestoresWithoutIdleHandler() {
        val tasks = defaultAppStartupTasks(
            sdkInt = 34,
            deferredDelayMs = 900L,
            dex2OatDelayMs = 2_500L,
        ).associateBy { it.id }

        assertEquals(StartupTrigger.FIRST_INTERACTIVE, tasks.getValue("playlist_restore").trigger)
        assertEquals(StartupDispatcher.IO, tasks.getValue("playlist_restore").dispatcher)
        assertEquals(900L, tasks.getValue("playlist_restore").delayMs)
        assertEquals(StartupTrigger.FIRST_INTERACTIVE, tasks.getValue("telemetry_init").trigger)
        assertEquals(StartupDispatcher.MAIN, tasks.getValue("telemetry_init").dispatcher)
        assertEquals(StartupTrigger.FIRST_INTERACTIVE, tasks.getValue("built_in_plugin_restore").trigger)
        assertEquals(StartupTrigger.FIRST_INTERACTIVE, tasks.getValue("json_plugin_restore").trigger)
        assertEquals(StartupTrigger.FIRST_INTERACTIVE, tasks.getValue("download_restore").trigger)
        assertEquals(
            setOf("download_manager_configure"),
            tasks.getValue("download_restore").dependencies,
        )
        assertEquals(StartupDispatcher.DEFAULT, tasks.getValue("dex2oat_profile_install").dispatcher)
        assertEquals(2_500L, tasks.getValue("dex2oat_profile_install").delayMs)
    }

    @Test
    fun startupPlanSkipsDex2OatInstallWhenSdkTooLow() {
        val taskIds = defaultAppStartupTasks(sdkInt = 23).map { it.id }

        assertTrue("dex2oat_profile_install" !in taskIds)
    }
}
