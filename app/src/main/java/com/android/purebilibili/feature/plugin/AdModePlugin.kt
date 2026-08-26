package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.plugin.FeedKind
import com.android.purebilibili.core.plugin.FeedTransformPlugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.RecommendationPluginApi
import com.android.purebilibili.core.plugin.RecommendationRequest
import com.android.purebilibili.core.plugin.RecommendationResult
import com.android.purebilibili.core.plugin.RecommendedVideo
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.data.model.response.PromotionBannerTarget
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.VideoPromotionPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val AD_MODE_PLUGIN_ID = "ad_mode_simulator"
@Serializable
data class AdModeConfig(
    val showSplashAd: Boolean = true,
    val advertiseCards: Boolean = true,
    val showPageBanners: Boolean = true,
)

object AdModeRuntime {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()
    private val _config = MutableStateFlow(AdModeConfig())
    val config = _config.asStateFlow()
    private val _splashCandidate = MutableStateFlow<VideoItem?>(null)
    val splashCandidate = _splashCandidate.asStateFlow()
    private val splashSession = AdModeSplashSession()

    internal suspend fun enable() {
        _config.value = migrateAdModeConfig(
            PluginStore.getConfigJson(PluginManager.getContext(), AD_MODE_PLUGIN_ID),
            json,
        )
        splashSession.reset()
        _splashCandidate.value = null
        _enabled.value = true
    }

    internal fun disable() {
        _enabled.value = false
        _splashCandidate.value = null
        splashSession.reset()
    }

    fun updateConfig(transform: (AdModeConfig) -> AdModeConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        if (!updated.showSplashAd) {
            splashSession.consume()
            _splashCandidate.value = null
        }
        ioScope.launch {
            PluginStore.setConfigJson(
                PluginManager.getContext(),
                AD_MODE_PLUGIN_ID,
                json.encodeToString(updated),
            )
        }
    }

    internal fun offerSplashCandidate(candidate: VideoItem?) {
        if (!_enabled.value || !splashSession.canOffer() || !_config.value.showSplashAd) return
        if (_splashCandidate.value == null && candidate?.bvid?.isNotBlank() == true) {
            _splashCandidate.value = candidate
        }
    }

    fun dismissSplashCandidate() {
        splashSession.consume()
        _splashCandidate.value = null
    }
}

internal class AdModeSplashSession {
    private var consumed = false

    fun canOffer(): Boolean = !consumed

    fun consume() {
        consumed = true
    }

    fun reset() {
        consumed = false
    }
}

class AdModePlugin : RecommendationPluginApi, FeedTransformPlugin {
    override val id: String = AD_MODE_PLUGIN_ID
    override val name: String = "广告模式"
    override val description: String = "全站营销内容重排、开屏广告和页面原生广告位"
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
    override suspend fun onDisable() = AdModeRuntime.disable()

    override fun transformFeedItems(items: List<VideoItem>, feedKind: FeedKind): List<VideoItem> {
        if (feedKind !in AD_MODE_FEED_KINDS || items.isEmpty()) return items
        val transformed = transformAdModeFeed(items, AdModeRuntime.config.value)
        if (feedKind == FeedKind.HOME_RECOMMEND) {
            AdModeRuntime.offerSplashCandidate(transformed.firstOrNull())
        }
        return transformed
    }

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val ranked = rankMarketingVideos(request.candidateVideos)
        return RecommendationResult(
            sourcePluginId = id,
            mode = request.mode,
            items = ranked.take(request.queueLimit).mapIndexed { index, video ->
                RecommendedVideo(
                    video = video,
                    score = (100 - index).toDouble(),
                    confidence = 0.55f,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSwitchPreference(
                title = "开屏广告",
                subtitle = "使用首页营销分最高的视频",
                checked = config.showSplashAd,
                onCheckedChange = { value -> AdModeRuntime.updateConfig { it.copy(showSplashAd = value) } },
            )
            AppSwitchPreference(
                title = "卡片广告化",
                subtitle = "全站视频卡片显示广告标签和营销文案",
                checked = config.advertiseCards,
                onCheckedChange = { value -> AdModeRuntime.updateConfig { it.copy(advertiseCards = value) } },
            )
            AppSwitchPreference(
                title = "页面广告位",
                subtitle = "每 6 条内容和视频详情中插入广告横幅",
                checked = config.showPageBanners,
                onCheckedChange = { value -> AdModeRuntime.updateConfig { it.copy(showPageBanners = value) } },
            )
        }
    }
}

internal val AD_MODE_FEED_KINDS = setOf(
    FeedKind.HOME_RECOMMEND,
    FeedKind.HOME_POPULAR,
    FeedKind.HOME_RANK,
    FeedKind.HOME_REGION,
    FeedKind.SEARCH,
)

internal fun transformAdModeFeed(items: List<VideoItem>, config: AdModeConfig): List<VideoItem> {
    val ranked = rankMarketingVideos(items)
    val markedBvids = ranked
        .filter { marketingScore(it) >= 60 }
        .take(2)
        .map { it.bvid }
        .toSet()
    return ranked.mapIndexed { index, video ->
        if (!config.advertiseCards && !config.showPageBanners) return@mapIndexed video
        video.copy(
            promotion = VideoPromotionPresentation(
                badgeLabel = "广告",
                supportingText = resolveMarketingSupportingText(video),
                actionLabel = "立即观看",
                marketingScore = marketingScore(video),
            ).takeIf { config.advertiseCards && video.bvid in markedBvids },
        )
    }
}

internal fun rankMarketingVideos(items: List<VideoItem>): List<VideoItem> = items
    .withIndex()
    .sortedWith(
        compareByDescending<IndexedValue<VideoItem>> { marketingScore(it.value) }
            .thenBy { it.index },
    )
    .map { it.value }

internal fun marketingScore(video: VideoItem): Int {
    val feedbackText = video.recommendationFeedback?.reasons
        .orEmpty()
        .joinToString(" ") { "${it.name} ${it.toast}" }
        .lowercase()
    val goto = video.recommendationFeedback?.goto.orEmpty().lowercase()
    val routeText = "${video.contentType} ${video.navigationUrl}".lowercase()
    val serverCommercialScore = when {
        COMMERCIAL_FEEDBACK_MARKERS.any(feedbackText::contains) -> 90
        goto.isNotBlank() && goto !in PLAYABLE_VIDEO_GOTO_TYPES -> 70
        COMMERCIAL_ROUTE_MARKERS.any(routeText::contains) -> 60
        else -> 0
    }
    val viewCount = video.stat.view.toLong().coerceAtLeast(0L)
    val likeCount = video.stat.like.toLong().coerceAtLeast(0L)
    val knownEngagement = likeCount > 0L || video.stat.coin > 0 || video.stat.favorite > 0 ||
        video.stat.reply > 0 || video.stat.share > 0
    val lowEngagementScore = if (
        viewCount > 10_000L && knownEngagement && likeCount * 1_000L < viewCount * 3L
    ) {
        28
    } else {
        0
    }
    val lowConversionScore = if (
        viewCount > 20_000L && knownEngagement &&
        video.stat.coin.toLong() * 2_000L < viewCount &&
        video.stat.favorite.toLong() * 1_000L < viewCount
    ) {
        18
    } else {
        0
    }
    val shortVideoScore = if (video.duration in 1..75) 8 else 0
    return serverCommercialScore + lowEngagementScore + lowConversionScore + shortVideoScore
}

private fun resolveMarketingSupportingText(video: VideoItem): String = when {
    marketingScore(video) >= 70 -> "商业推广"
    marketingScore(video) >= 40 -> "高曝光推广内容"
    else -> "为你推荐"
}

private fun VideoItem.toPromotionBannerTarget() = PromotionBannerTarget(
    bvid = bvid,
    cid = cid,
    title = title,
    ownerName = owner.name,
    coverUrl = pic,
)

internal fun resolveAdModeRelatedBannerTarget(videos: List<RelatedVideo>): PromotionBannerTarget? {
    val ranked = rankMarketingVideos(
        videos.map { related ->
            VideoItem(
                id = related.aid,
                aid = related.aid,
                bvid = related.bvid,
                cid = related.cid,
                title = related.title,
                pic = related.pic,
                owner = related.owner,
                stat = related.stat,
                duration = related.duration,
            )
        },
    )
    return ranked.firstOrNull { it.bvid.isNotBlank() }?.toPromotionBannerTarget()
}

internal fun migrateAdModeConfig(raw: String?, json: Json): AdModeConfig {
    if (raw.isNullOrBlank()) return AdModeConfig()
    return runCatching { json.decodeFromString<AdModeConfig>(raw) }.getOrDefault(AdModeConfig())
}

private val PLAYABLE_VIDEO_GOTO_TYPES = setOf("av", "video", "")
private val COMMERCIAL_FEEDBACK_MARKERS = listOf("广告", "推广", "营销", "商业")
private val COMMERCIAL_ROUTE_MARKERS = listOf(
    "mall.bilibili.com",
    "show.bilibili.com",
    "activity.bilibili.com",
    "commercial",
    "product",
    "campaign",
)
