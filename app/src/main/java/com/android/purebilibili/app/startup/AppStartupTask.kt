package com.android.purebilibili.app

import android.os.Build

internal enum class StartupTrigger {
    APP_CREATE,
    FIRST_INTERACTIVE,
    ON_DEMAND,
}

internal enum class StartupDispatcher {
    MAIN,
    IO,
    DEFAULT,
}

internal data class AppStartupTask(
    val id: String,
    val trigger: StartupTrigger,
    val dispatcher: StartupDispatcher,
    val delayMs: Long = 0L,
    val dependencies: Set<String> = emptySet(),
)

internal fun defaultAppStartupTasks(
    sdkInt: Int = Build.VERSION.SDK_INT,
    deferredDelayMs: Long = PureApplicationRuntimeConfig.deferredNonCriticalStartupDelayMs(),
    dex2OatDelayMs: Long = PureApplicationRuntimeConfig.dex2OatProfileInstallDelayMs(),
): List<AppStartupTask> {
    val playlistTrigger = if (PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup()) {
        StartupTrigger.FIRST_INTERACTIVE
    } else {
        StartupTrigger.APP_CREATE
    }
    val telemetryTrigger = if (PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup()) {
        StartupTrigger.FIRST_INTERACTIVE
    } else {
        StartupTrigger.APP_CREATE
    }

    val tasks = mutableListOf(
        AppStartupTask("network_module_init", StartupTrigger.APP_CREATE, StartupDispatcher.MAIN),
        AppStartupTask("token_manager_init", StartupTrigger.APP_CREATE, StartupDispatcher.IO),
        AppStartupTask(
            id = "wbi_key_restore",
            trigger = StartupTrigger.APP_CREATE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("token_manager_init"),
        ),
        AppStartupTask(
            id = "video_repository_init",
            trigger = StartupTrigger.APP_CREATE,
            dispatcher = StartupDispatcher.MAIN,
            dependencies = setOf("network_module_init", "wbi_key_restore"),
        ),
        AppStartupTask("background_manager_init", StartupTrigger.APP_CREATE, StartupDispatcher.MAIN),
        AppStartupTask("player_settings_cache_init", StartupTrigger.APP_CREATE, StartupDispatcher.IO),
        AppStartupTask("home_visual_defaults_restore", StartupTrigger.APP_CREATE, StartupDispatcher.IO),
        AppStartupTask("plugin_manager_configure", StartupTrigger.APP_CREATE, StartupDispatcher.MAIN),
        AppStartupTask(
            id = "home_feed_policy_restore",
            trigger = StartupTrigger.APP_CREATE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("plugin_manager_configure"),
        ),
        AppStartupTask("download_manager_configure", StartupTrigger.APP_CREATE, StartupDispatcher.IO),
        AppStartupTask(
            id = "notification_channel_init",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.MAIN,
        ),
        AppStartupTask(
            id = "playlist_restore",
            trigger = playlistTrigger,
            dispatcher = StartupDispatcher.IO,
            delayMs = if (playlistTrigger == StartupTrigger.FIRST_INTERACTIVE) deferredDelayMs else 0L,
        ),
        AppStartupTask(
            id = "telemetry_init",
            trigger = telemetryTrigger,
            dispatcher = StartupDispatcher.MAIN,
            delayMs = if (telemetryTrigger == StartupTrigger.FIRST_INTERACTIVE) deferredDelayMs else 0L,
            dependencies = setOf("background_manager_init"),
        ),
        AppStartupTask(
            id = "built_in_plugin_restore",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("home_feed_policy_restore"),
        ),
        AppStartupTask(
            id = "json_plugin_restore",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("plugin_manager_configure"),
        ),
        AppStartupTask(
            id = "download_restore",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("download_manager_configure"),
        ),
        AppStartupTask(
            id = "plugin_preferences_sync",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.IO,
            dependencies = setOf("built_in_plugin_restore"),
        ),
        AppStartupTask(
            id = "launcher_icon_sync",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.IO,
        ),
    )

    if (PureApplicationRuntimeConfig.shouldRequestDex2OatProfileInstall(sdkInt)) {
        tasks += AppStartupTask(
            id = "dex2oat_profile_install",
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            dispatcher = StartupDispatcher.DEFAULT,
            delayMs = dex2OatDelayMs,
        )
    }

    return tasks
}

internal fun validateStartupTaskGraph(tasks: List<AppStartupTask>): Boolean {
    val ids = tasks.map { it.id }
    if (ids.any(String::isBlank) || ids.distinct().size != ids.size) return false
    val knownIds = ids.toSet()
    return tasks.all { task ->
        task.delayMs >= 0L && task.id !in task.dependencies && task.dependencies.all(knownIds::contains)
    }
}
