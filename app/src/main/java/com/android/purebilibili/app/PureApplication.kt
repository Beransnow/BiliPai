// 文件路径: app/PureApplication.kt
package com.android.purebilibili.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import androidx.profileinstaller.ProfileInstaller
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.android.purebilibili.core.coroutines.AppScope
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.DEFAULT_ANALYTICS_ENABLED
import com.android.purebilibili.core.store.DEFAULT_CRASH_TRACKING_ENABLED
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.store.allManagedAppIconLauncherAliases
import com.android.purebilibili.core.store.DEFAULT_APP_ICON_KEY
import com.android.purebilibili.core.store.AppIconAppearance
import com.android.purebilibili.core.store.normalizeAppIconKey
import com.android.purebilibili.core.store.resolveAppIconLauncherAlias
import com.android.purebilibili.core.store.supportsAppIconAppearance
import com.android.purebilibili.core.ui.performance.PerformanceStrictMode
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.settings.applyAppLanguage
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.resolveThemeModePreference
import com.android.purebilibili.feature.plugin.AdFilterPlugin
import com.android.purebilibili.feature.plugin.Anime4KPlugin
import com.android.purebilibili.feature.plugin.CdnRegionPlugin
import com.android.purebilibili.feature.plugin.DanmakuEnhancePlugin
import com.android.purebilibili.feature.plugin.EyeProtectionPlugin
import com.android.purebilibili.feature.plugin.HomeFeedAnonymizerPlugin
import com.android.purebilibili.feature.plugin.SponsorBlockPlugin
import com.android.purebilibili.feature.plugin.dlna.DlnaCastPlugin
import com.android.purebilibili.feature.plugin.googlecast.GoogleCastPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPlugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun shouldRefreshLauncherIconForNightModeChange(
    previousUiMode: Int,
    currentUiMode: Int
): Boolean {
    val previousNightMode = previousUiMode and Configuration.UI_MODE_NIGHT_MASK
    val currentNightMode = currentUiMode and Configuration.UI_MODE_NIGHT_MASK
    return previousNightMode != currentNightMode
}

//  实现 ImageLoaderFactory 以提供自定义 Coil 配置
//  实现 ComponentCallbacks2 响应系统内存警告
class PureApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {
    
    //  保存 ImageLoader 引用以便在 onTrimMemory 中使用
    private var _imageLoader: ImageLoader? = null
    private var launcherIconUiModeSnapshot: Int? = null

    private val telemetryListener =
        PureApplicationRuntimeConfig.createTelemetryBackgroundStateListener()

    private val startupOrchestrator by lazy {
        AppStartupOrchestrator { task, error ->
            Logger.e(
                PureApplicationRuntimeConfig.TAG,
                "Startup task failed: ${task.id}",
                error,
            )
        }
    }

    internal lateinit var startupSessionCoordinator: StartupSessionCoordinator
        private set
    
    //  Coil 图片加载器 - 优化内存和磁盘缓存
    override fun newImageLoader(): ImageLoader {
        val memoryCachePercent = PureApplicationRuntimeConfig.resolveImageMemoryCachePercent()
        val diskCacheBytes = 150L * 1024 * 1024
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            //  内存缓存预算（移动/平板主仓）
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .build()
            }
            //  磁盘缓存预算（移动/平板主仓）
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheBytes)
                    .build()
            }
            .okHttpClient { NetworkModule.okHttpClient } // 🔥 [Fix] 共享 OkHttpClient 以获得 DNS 修复
            //  优先使用缓存
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            //  启用 Bitmap 复用减少内存分配
            .allowRgb565(true)
            .crossfade(true)
            .build()
            .also { _imageLoader = it }  // 保存引用
    }
    
    override fun onCreate() {
        val startupSessionStartedAtMs = SystemClock.elapsedRealtime()
        PerformanceStrictMode.installIfEnabled()
        //  [关键] 必须在 super.onCreate() 之前设置！
        // 这样系统在初始化时就能读取到正确的夜间模式配置
        applyThemePreference()
        
        super.onCreate()
        launcherIconUiModeSnapshot = resources.configuration.uiMode
        Logger.init(this)
        CrashReporter.installGlobalExceptionHandler()

        startupSessionCoordinator = StartupSessionCoordinator(
            sessionStartedAtMs = startupSessionStartedAtMs,
            readMirror = {
                SettingsManager.readLaunchToPortraitFeedOnStartupMirror(this@PureApplication)
            },
            readAuthoritative = {
                SettingsManager.readLaunchToPortraitFeedOnStartup(this@PureApplication)
            },
            writeMirror = { enabled ->
                SettingsManager.writeLaunchToPortraitFeedOnStartupMirror(
                    this@PureApplication,
                    enabled,
                )
            },
        ).also { coordinator ->
            coordinator.start(AppScope.ioScope)
        }

        startupOrchestrator.start(
            trigger = StartupTrigger.APP_CREATE,
            scope = AppScope.ioScope,
            taskRunner = ::runStartupTask,
        )
    }

    internal fun onFirstInteractive() {
        startupOrchestrator.start(
            trigger = StartupTrigger.FIRST_INTERACTIVE,
            scope = AppScope.ioScope,
            taskRunner = ::runStartupTask,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val previousUiMode = launcherIconUiModeSnapshot
        super.onConfigurationChanged(newConfig)
        launcherIconUiModeSnapshot = newConfig.uiMode
        if (
            previousUiMode != null &&
            shouldRefreshLauncherIconForNightModeChange(previousUiMode, newConfig.uiMode)
        ) {
            refreshActiveLauncherAliasForNightMode()
        }
    }

    private suspend fun runStartupTask(task: AppStartupTask) {
        when (task.id) {
            "network_module_init" -> NetworkModule.init(this)
            "token_manager_init" -> TokenManager.init(this)
            "wbi_key_restore" -> WbiKeyManager.restoreFromStorage(this)
            "video_repository_init" -> com.android.purebilibili.data.repository.VideoRepository.init(this)
            "background_manager_init" -> BackgroundManager.init(this)
            "player_settings_cache_init" -> com.android.purebilibili.core.store.PlayerSettingsCache.init(this)
            "home_visual_defaults_restore" -> SettingsManager.ensureHomeVisualDefaults(this)
            "plugin_manager_configure" -> PluginManager.initialize(this)
            "home_feed_policy_restore" -> PluginManager.register(HomeFeedAnonymizerPlugin())
            "download_manager_configure" -> com.android.purebilibili.feature.download.DownloadManager
                .configure(this, AppScope.ioScope)
            "notification_channel_init" -> createNotificationChannel()
            "playlist_restore" -> initPlaylistRestoreNow()
            "telemetry_init" -> initTelemetryNow()
            "built_in_plugin_restore" -> restoreBuiltInPluginsNow()
            "json_plugin_restore" -> {
                com.android.purebilibili.core.plugin.json.JsonPluginManager.initialize(this)
            }
            "download_restore" -> {
                if (!com.android.purebilibili.feature.download.DownloadManager.ensureRestored()) {
                    Logger.w(PureApplicationRuntimeConfig.TAG, "Download restore failed; persisted state preserved")
                }
            }
            "plugin_preferences_sync" -> syncPluginPreferencesNow()
            "launcher_icon_sync" -> syncAppIconState()
            "dex2oat_profile_install" -> requestDex2OatProfileInstallNow()
        }
    }

    private fun initPlaylistRestoreNow() {
        com.android.purebilibili.feature.video.player.PlaylistManager.init(this@PureApplication)
    }

    private fun initTelemetryNow() {
        initCrashlytics()
        initAnalytics()
        attachTelemetryListener()
    }

    private suspend fun restoreBuiltInPluginsNow() {
        PluginManager.registerAll(
            listOf(
                SponsorBlockPlugin(),
                AdFilterPlugin(),
                Anime4KPlugin(),
                DanmakuEnhancePlugin(),
                EyeProtectionPlugin(),
                TodayWatchPlugin(),
                CdnRegionPlugin(),
                DlnaCastPlugin(),
                GoogleCastPlugin(),
            )
        )
        Logger.d(PureApplicationRuntimeConfig.TAG, " Plugin system restored and published")
    }

    private suspend fun syncPluginPreferencesNow() {
        val sponsorBlockEnabled = SettingsManager
            .getSponsorBlockEnabled(this@PureApplication)
            .first()
        PluginManager.setEnabled("sponsor_block", sponsorBlockEnabled)
        Logger.d(PureApplicationRuntimeConfig.TAG, " SponsorBlock plugin synced: enabled=$sponsorBlockEnabled")
        SettingsManager.forceDanmakuDefaults(this@PureApplication)
    }

    private fun requestDex2OatProfileInstallNow() {
        runCatching {
            ProfileInstaller.writeProfile(this)
        }.onSuccess {
            Logger.d(PureApplicationRuntimeConfig.TAG, "📦 Requested ART profile installation for dex2oat")
        }.onFailure { throwable ->
            Logger.w(PureApplicationRuntimeConfig.TAG, "⚠️ ART profile installation request failed", throwable)
        }
    }

    private fun attachTelemetryListener() {
        // 监听全局前后台状态，增强会话与崩溃上下文
        BackgroundManager.addListener(telemetryListener)
        if (!BackgroundManager.isInBackground) {
            AnalyticsHelper.onAppForeground()
            CrashReporter.setAppForegroundState(true)
        }
    }
    
    //  初始化 Firebase Crashlytics
    private fun initCrashlytics() {
        try {
            //  读取用户设置（默认开启）
            val prefs = getSharedPreferences("crash_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", DEFAULT_CRASH_TRACKING_ENABLED)
            
            CrashReporter.init(this)
            CrashReporter.installGlobalExceptionHandler()
            CrashReporter.setEnabled(enabled)
            
            if (enabled) {
                CrashReporter.syncUserContext(
                    mid = TokenManager.midCache,
                    isVip = TokenManager.isVipCache,
                    privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(this)
                )
            }
            
            Logger.d(PureApplicationRuntimeConfig.TAG, " Firebase Crashlytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(PureApplicationRuntimeConfig.TAG, "Failed to init Crashlytics", e)
        }
    }
    
    // � 初始化 Firebase Analytics
    private fun initAnalytics() {
        try {
            // 初始化 AnalyticsHelper
            AnalyticsHelper.init(this)
            
            //  读取用户设置（默认开启）
            val prefs = getSharedPreferences("analytics_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", DEFAULT_ANALYTICS_ENABLED)
            
            //  根据用户设置启用/禁用 Analytics
            AnalyticsHelper.setEnabled(enabled)
            
            if (enabled) {
                AnalyticsHelper.syncUserContext(
                    mid = TokenManager.midCache,
                    isVip = TokenManager.isVipCache,
                    privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(this)
                )
                // 记录应用打开事件
                AnalyticsHelper.logAppOpen()
                AnalyticsHelper.logDailyActive(source = "app_start")
            }
            
            Logger.d(PureApplicationRuntimeConfig.TAG, " Firebase Analytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(PureApplicationRuntimeConfig.TAG, "Failed to init Analytics", e)
        }
    }
    
    // [后台内存优化] 响应系统内存警告
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val plan = PureApplicationRuntimeConfig.resolveBackgroundMemoryTrimPlan(level)
        if (plan.imageCacheTrimLevel != null) {
            _imageLoader?.memoryCache?.trimMemory(plan.imageCacheTrimLevel)
            if (plan.requestGcHint) {
                System.gc()
            }
            when {
                plan.clearImageMemoryCache -> {
                    Logger.d(PureApplicationRuntimeConfig.TAG, "🚨 trim(level=$level), released image memory cache")
                }
                level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    Logger.d(PureApplicationRuntimeConfig.TAG, " UI hidden, trimmed image memory cache for background")
                }
                else -> {
                    Logger.d(PureApplicationRuntimeConfig.TAG, " Low memory trim(level=$level), trimmed image memory cache")
                }
            }
        }
        if (plan.notifyPlayerHeavyOptimization) {
            com.android.purebilibili.feature.video.player.MiniPlayerManager
                .getInstanceOrNull()
                ?.onMemoryPressureTrim(
                    level = level,
                    requestIdlePlaybackRelease = plan.requestIdlePlaybackRelease
                )
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        val plan = PureApplicationRuntimeConfig.resolveBackgroundMemoryTrimPlan(
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        )
        _imageLoader?.memoryCache?.clear()
        if (plan.notifyPlayerHeavyOptimization) {
            com.android.purebilibili.feature.video.player.MiniPlayerManager
                .getInstanceOrNull()
                ?.onMemoryPressureTrim(
                    level = ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                    requestIdlePlaybackRelease = plan.requestIdlePlaybackRelease
                )
        }
        Logger.d(PureApplicationRuntimeConfig.TAG, "🚨 onLowMemory, cleared all caches")
    }

    private fun createNotificationChannel() {
        // 仅在 Android 8.0 (API 26) 及以上需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            resolveAppNotificationChannels().forEach { spec ->
                val channel = NotificationChannel(spec.id, spec.name, spec.importance).apply {
                    description = spec.description
                    setShowBadge(spec.showBadge)
                    if (spec.silent) {
                        setSound(null, null)
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     *  应用主题偏好 - 在 Splash Screen 显示前调用
     * 
     * 这解决了：用户在应用内强制深色模式，但系统是浅色时，启动屏仍然是白色的问题。
     * 通过 AppCompatDelegate.setDefaultNightMode() 强制系统使用正确的深色/浅色模式。
     */
    private fun applyThemePreference() {
        // 同步读取保存的主题设置（必须同步，因为 Splash Screen 马上就会显示）
        val prefs = getSharedPreferences("theme_cache", Context.MODE_PRIVATE)
        val themeModeValue = prefs.getInt("theme_mode", 0)  // 0 = FOLLOW_SYSTEM
        val appLanguage = SettingsManager.getAppLanguageSync(this)
        val themeMode = resolveThemeModePreference(themeModeValue)
        
        val nightMode = when (themeMode) {
            AppThemeMode.FOLLOW_SYSTEM -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        }
        
        applyAppLanguage(appLanguage)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
        Logger.d(
            PureApplicationRuntimeConfig.TAG,
            " Applied launch preferences: themeMode=$themeModeValue -> resolvedMode=$themeMode, nightMode=$nightMode, appLanguage=${appLanguage.name}"
        )
    }
    
    /**
     *  同步应用图标状态
     * 
     * 在 Application.onCreate 时调用，确保启动器图标与用户偏好一致。
     * 
     * 修复：重装后检测 icon 偏好与 Manifest 默认状态冲突，自动重置为默认图标。
     */
    private suspend fun syncAppIconState() {
        try {
                val pm = packageManager
                val packageName = this@PureApplication.packageName
                // 读取用户保存的图标偏好
                val currentIcon = normalizeAppIconKey(
                    SettingsManager.getAppIcon(this@PureApplication).first()
                )
                val appearance = SettingsManager.getAppIconAppearance(this@PureApplication).first()
                val defaultLauncherAlias = resolveAppIconLauncherAlias(
                    packageName = packageName,
                    rawKey = DEFAULT_APP_ICON_KEY,
                    appearance = appearance
                )
                val splashIconVisible = SettingsManager.isSplashIconAnimationEnabledSync(this@PureApplication)
                val cacheSynced = this@PureApplication
                    .getSharedPreferences("app_icon_cache", Context.MODE_PRIVATE)
                    .edit()
                    .putString("current_icon", currentIcon)
                    .putInt("appearance", appearance.storedValue)
                    .commit()
                Logger.d(PureApplicationRuntimeConfig.TAG, " Synced app icon cache from DataStore: $currentIcon (success=$cacheSynced)")

                val allUniqueAliases = allManagedAppIconLauncherAliases(packageName)
                val targetAlias = resolveAppIconLauncherAlias(
                    packageName = packageName,
                    rawKey = currentIcon,
                    splashIconVisible = splashIconVisible,
                    appearance = appearance
                )
                
                val targetAliasComponent = android.content.ComponentName(packageName, targetAlias)
                val targetState = pm.getComponentEnabledSetting(targetAliasComponent)

                // 如果目标 alias 是 disabled（说明之前被禁用了，可能是重装），强制重置为默认图标。
                if (currentIcon != DEFAULT_APP_ICON_KEY && targetState == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    Logger.d(PureApplicationRuntimeConfig.TAG, " Detected reinstall: target icon '$currentIcon' is disabled, resetting to '$DEFAULT_APP_ICON_KEY'")
                    
                    SettingsManager.setAppIcon(this@PureApplication, DEFAULT_APP_ICON_KEY)
                    
                    // 确保默认图标被启用
                    val aliasDefault = android.content.ComponentName(packageName, defaultLauncherAlias)
                    pm.setComponentEnabledSetting(
                        aliasDefault,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    // 禁用其他所有alias
                    allUniqueAliases.filter { it != defaultLauncherAlias }.forEach { aliasFullName ->
                        pm.setComponentEnabledSetting(
                            android.content.ComponentName(packageName, aliasFullName),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                    }
                    Logger.d(PureApplicationRuntimeConfig.TAG, " Reset to default icon: $DEFAULT_APP_ICON_KEY")
                    return
                }
                
                // 同步所有 alias 状态：只有目标启用，其他禁用
                allUniqueAliases.forEach { aliasFullName ->
                    try {
                        val currentState = pm.getComponentEnabledSetting(
                            android.content.ComponentName(packageName, aliasFullName)
                        )
                        val shouldBeEnabled = aliasFullName == targetAlias
                        val targetState = if (shouldBeEnabled) {
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        }
                        
                        // 只在状态不一致时修改，减少不必要的操作
                        if (currentState != targetState) {
                            pm.setComponentEnabledSetting(
                                android.content.ComponentName(packageName, aliasFullName),
                                targetState,
                                android.content.pm.PackageManager.DONT_KILL_APP
                            )
                        }
                    } catch (e: Exception) {
                        //  [容错] 忽略不存在的组件，防止崩溃
                        Logger.d(PureApplicationRuntimeConfig.TAG, "⚠️ Component $aliasFullName not found, skipping")
                    }
                }
                
                Logger.d(PureApplicationRuntimeConfig.TAG, " Synced app icon state: $currentIcon")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            android.util.Log.e(PureApplicationRuntimeConfig.TAG, "Failed to sync app icon state", e)
        }
    }

    private fun refreshActiveLauncherAliasForNightMode() {
        AppScope.ioScope.launch {
            val appearance = SettingsManager.getAppIconAppearanceSync(this@PureApplication)
            if (appearance != AppIconAppearance.FOLLOW_SYSTEM) return@launch
            val currentIcon = SettingsManager.getAppIconSync(this@PureApplication)
            if (!supportsAppIconAppearance(currentIcon)) return@launch
            val splashIconVisible = SettingsManager.isSplashIconAnimationEnabledSync(this@PureApplication)
            val alias = resolveAppIconLauncherAlias(
                packageName = packageName,
                rawKey = currentIcon,
                splashIconVisible = splashIconVisible,
                appearance = appearance
            )
            val component = ComponentName(packageName, alias)
            val pm = packageManager
            var aliasDisabled = false
            try {
                if (
                    pm.getComponentEnabledSetting(component) ==
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) {
                    Logger.w(
                        PureApplicationRuntimeConfig.TAG,
                        "Launcher icon refresh skipped because alias is disabled: $alias"
                    )
                    return@launch
                }
                pm.setComponentEnabledSetting(
                    component,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                aliasDisabled = true
                delay(100)
            } catch (throwable: Exception) {
                Logger.e(
                    PureApplicationRuntimeConfig.TAG,
                    "Failed to invalidate launcher icon after night mode change",
                    throwable
                )
            } finally {
                if (aliasDisabled) {
                    runCatching {
                        pm.setComponentEnabledSetting(
                            component,
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                    }.onSuccess {
                        Logger.d(
                            PureApplicationRuntimeConfig.TAG,
                            "Launcher icon refreshed after night mode change: $alias"
                        )
                    }.onFailure { throwable ->
                        Logger.e(
                            PureApplicationRuntimeConfig.TAG,
                            "Failed to restore launcher icon alias after night mode change",
                            throwable
                        )
                    }
                }
            }
        }
    }
}
