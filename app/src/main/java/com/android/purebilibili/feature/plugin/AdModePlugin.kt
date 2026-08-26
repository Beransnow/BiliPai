package com.android.purebilibili.feature.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.RecommendationPluginApi
import com.android.purebilibili.core.plugin.RecommendationRequest
import com.android.purebilibili.core.plugin.RecommendationResult
import com.android.purebilibili.core.plugin.RecommendedVideo
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

internal const val AD_MODE_PLUGIN_ID = "ad_mode_simulator"

@Serializable
data class AdModeConfig(
    val showSplashAd: Boolean = true,
    val showPageBanners: Boolean = true,
    val prioritizeMarketingVideos: Boolean = true,
)

object AdModeRuntime {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _config = MutableStateFlow(AdModeConfig())
    val config = _config.asStateFlow()
    private var splashShownForCurrentEnableSession = false

    internal fun setEnabled(value: Boolean) {
        _enabled.value = value
        if (!value) splashShownForCurrentEnableSession = false
    }

    internal fun consumeSplashOpportunity(): Boolean {
        if (splashShownForCurrentEnableSession) return false
        splashShownForCurrentEnableSession = true
        return true
    }

    fun updateConfig(transform: (AdModeConfig) -> AdModeConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        ioScope.launch {
            PluginStore.setConfigJson(
                PluginManager.getContext(),
                AD_MODE_PLUGIN_ID,
                json.encodeToString(updated),
            )
        }
    }

    internal suspend fun enable() {
        PluginStore.getConfigJson(PluginManager.getContext(), AD_MODE_PLUGIN_ID)
            ?.let { stored -> runCatching { json.decodeFromString<AdModeConfig>(stored) }.getOrNull() }
            ?.let { _config.value = it }
        _enabled.value = true
    }
}

class AdModePlugin : RecommendationPluginApi {
    override val id: String = AD_MODE_PLUGIN_ID
    override val name: String = "广告模式"
    override val description: String = "开屏广告、营销内容优先和页面广告横幅"
    override val version: String = "1.0.0"
    override val author: String = "BiliPai项目组"
    override val icon: ImageVector = Icons.Outlined.Campaign
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.AdModePlugin",
        capabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES),
    )

    override suspend fun onEnable() = AdModeRuntime.enable()

    override suspend fun onDisable() = AdModeRuntime.setEnabled(false)

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val config = AdModeRuntime.config.value
        val ranked = if (config.prioritizeMarketingVideos) {
            request.candidateVideos.sortedByDescending(::marketingScore)
        } else {
            request.candidateVideos
        }
        return RecommendationResult(
            sourcePluginId = id,
            mode = request.mode,
            items = ranked.take(request.queueLimit).mapIndexed { index, video ->
                RecommendedVideo(
                    video = video,
                    score = (100 - index).toDouble(),
                    confidence = 0.45f,
                    explanation = "营销特征优先",
                )
            },
            historySampleCount = request.historyVideos.size,
            sceneSignals = request.sceneSignals,
        )
    }

    @Composable
    override fun SettingsContent() {
        val config by AdModeRuntime.config.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSwitchPreference(
                title = "开屏广告",
                subtitle = "每次启用后显示一次，可立即跳过",
                checked = config.showSplashAd,
                onCheckedChange = { checked -> AdModeRuntime.updateConfig { it.copy(showSplashAd = checked) } },
            )
            AppSwitchPreference(
                title = "页面广告横幅",
                subtitle = "在首页和视频详情等页面底部显示演示横幅",
                checked = config.showPageBanners,
                onCheckedChange = { checked -> AdModeRuntime.updateConfig { it.copy(showPageBanners = checked) } },
            )
            AppSwitchPreference(
                title = "营销内容优先",
                subtitle = "在插件推荐队列中优先排列带营销特征的候选视频",
                checked = config.prioritizeMarketingVideos,
                onCheckedChange = { checked ->
                    AdModeRuntime.updateConfig { it.copy(prioritizeMarketingVideos = checked) }
                },
            )
        }
    }
}

internal fun marketingScore(video: VideoItem): Int {
    val text = "${video.title} ${video.owner.name} ${video.tname} ${video.contentType}".lowercase()
    val keywordScore = MARKETING_KEYWORDS.count(text::contains) * 20
    val lowEngagementScore = if (
        video.stat.view > 10_000 &&
        video.stat.like.toLong() * 100L < video.stat.view.toLong()
    ) {
        12
    } else {
        0
    }
    val shortVideoScore = if (video.duration in 1..90) 6 else 0
    return keywordScore + lowEngagementScore + shortVideoScore
}

private val MARKETING_KEYWORDS = listOf(
    "必买", "测评", "种草", "优惠", "限时", "新品", "同款", "开箱", "推荐", "避坑", "直播间",
)

@Composable
fun AdModeOverlayHost(currentRoute: String?, modifier: Modifier = Modifier) {
    val enabled by AdModeRuntime.enabled.collectAsStateWithLifecycle()
    val config by AdModeRuntime.config.collectAsStateWithLifecycle()
    var splashVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled, config.showSplashAd) {
        if (enabled && config.showSplashAd && AdModeRuntime.consumeSplashOpportunity()) {
            splashVisible = true
            delay(3_500)
            splashVisible = false
        } else {
            splashVisible = false
        }
    }

    if (!enabled) return
    Box(modifier = modifier.fillMaxSize()) {
        if (config.showPageBanners && !splashVisible) {
            AdModeBanner(
                placement = if (currentRoute?.substringBefore("?") == "video") "视频详情" else "信息流",
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 88.dp),
            )
        }
        AnimatedVisibility(
            visible = splashVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF7D54A)).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppText("精选推荐", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    AppText("全场限时 · 错过再等五分钟", style = MaterialTheme.typography.titleLarge)
                    AppTextButton(onClick = { splashVisible = false }) { AppText("跳过广告") }
                }
            }
        }
    }
}

@Composable
private fun AdModeBanner(placement: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF201F1A), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.82f)) {
            AppText("广告模式 · $placement", color = Color.White, fontWeight = FontWeight.Bold)
            AppText("你刚好需要的，算法刚好知道", color = Color(0xFFE6E1D5))
        }
        AppText("广告", color = Color(0xFFF7D54A), style = MaterialTheme.typography.labelMedium)
    }
}
