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
import com.android.purebilibili.data.repository.SearchRepository
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
import kotlin.random.Random

internal const val AD_MODE_PLUGIN_ID = "ad_mode_simulator"

/** Manually curated commercial terms used when an API response has no ad flag. */
internal val MANUAL_AD_KEYWORDS = listOf(
    "流量卡", "电话卡", "号卡", "通信卡", "拼多多", "淘宝", "天猫", "京东",
    "唯品会", "优惠券", "开卡", "办卡", "充值", "下单", "领券",
)
private val CURATED_AD_SEARCH_KEYWORDS = listOf("流量卡", "拼多多", "淘宝", "京东")
@Serializable
data class AdModeConfig(
    val showSplashAd: Boolean = true,
    val advertiseCards: Boolean = true,
    val showPageBanners: Boolean = true,
    val shakeToExitSplash: Boolean = false,
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
    private val _curatedAds = MutableStateFlow<List<VideoItem>>(emptyList())
    val curatedAds = _curatedAds.asStateFlow()
    private val splashSession = AdModeSplashSession()

    internal suspend fun enable() {
        _config.value = migrateAdModeConfig(
            PluginStore.getConfigJson(PluginManager.getContext(), AD_MODE_PLUGIN_ID),
            json,
        )
        _curatedAds.value = loadCuratedAds()
        splashSession.reset()
        _splashCandidate.value = null
        _enabled.value = true
    }

    internal fun disable() {
        _enabled.value = false
        _curatedAds.value = emptyList()
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
        if (_splashCandidate.value == null && candidate?.bvid?.isNotBlank() == true &&
            marketingScore(candidate) >= 60
        ) {
            _splashCandidate.value = candidate
        }
    }

    private suspend fun loadCuratedAds(): List<VideoItem> {
        return CURATED_AD_SEARCH_KEYWORDS
            .flatMap { keyword ->
                SearchRepository.search(keyword).getOrNull()?.first.orEmpty()
            }
            .filter { containsManualAdKeyword(it) || it.isAd }
            .distinctBy { it.bvid.ifBlank { "aid:${it.aid}" } }
            .take(6)
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
        if (!AdModeRuntime.enabled.value) return items
        if (feedKind !in AD_MODE_FEED_KINDS) return items
        val transformed = transformAdModeFeed(
            items = items,
            config = AdModeRuntime.config.value,
            curatedAds = AdModeRuntime.curatedAds.value,
        )
        if (feedKind == FeedKind.HOME_RECOMMEND) {
            AdModeRuntime.offerSplashCandidate(resolveAdModeSplashCandidate(transformed))
        }
        return transformed
    }

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        if (!AdModeRuntime.enabled.value) {
            return RecommendationResult(
                sourcePluginId = id,
                mode = request.mode,
                items = request.candidateVideos.take(request.queueLimit).mapIndexed { index, video ->
                    RecommendedVideo(
                        video = video,
                        score = (request.queueLimit - index).toDouble(),
                        confidence = 0f,
                        explanation = "广告模式未启用",
                    )
                },
                historySampleCount = request.historyVideos.size,
                sceneSignals = request.sceneSignals,
            )
        }
        val recommendationCandidates = (AdModeRuntime.curatedAds.value + request.candidateVideos)
            .distinctBy { it.bvid.ifBlank { "aid:${it.aid}" } }
        val ranked = if (!AdModeRuntime.config.value.advertiseCards &&
            !AdModeRuntime.config.value.showPageBanners
        ) {
            request.candidateVideos
        } else {
            rankMarketingVideos(recommendationCandidates)
        }
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
                subtitle = "在视频详情的相关推荐中插入已识别的广告横幅",
                checked = config.showPageBanners,
                onCheckedChange = { value -> AdModeRuntime.updateConfig { it.copy(showPageBanners = value) } },
            )
            AppSwitchPreference(
                title = "摇动退出开屏广告",
                subtitle = "开屏广告期间连续强烈摇动手机可退出本次广告（默认关闭）",
                checked = config.shakeToExitSplash,
                onCheckedChange = { value -> AdModeRuntime.updateConfig { it.copy(shakeToExitSplash = value) } },
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

internal fun transformAdModeFeed(
    items: List<VideoItem>,
    config: AdModeConfig,
    curatedAds: List<VideoItem> = emptyList(),
): List<VideoItem> {
    // Turning off both presentation features must be a true no-op. In
    // particular, do not reorder the user's feed just because the plugin is
    // enabled in the background.
    if (!config.advertiseCards && !config.showPageBanners) return items
    val candidates = (curatedAds + items)
        .distinctBy { it.bvid.ifBlank { "aid:${it.aid}" } }
    val ranked = rankMarketingVideos(candidates)
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

internal fun resolveAdModeSplashCandidate(items: List<VideoItem>): VideoItem? =
    items.firstOrNull { it.bvid.isNotBlank() && marketingScore(it) >= 60 }

internal fun marketingScore(video: VideoItem): Int {
    // Preserve the server's authoritative classification above all heuristics.
    if (video.isAd) return 100
    if (containsManualAdKeyword(video)) return 85
    val feedbackText = video.recommendationFeedback?.reasons
        .orEmpty()
        .joinToString(" ") { "${it.name} ${it.toast}" }
        .lowercase()
    val goto = video.recommendationFeedback?.goto.orEmpty().lowercase()
    val routeText = "${video.contentType} ${video.navigationUrl}".lowercase()
    val trafficCardScore = resolveTrafficCardScore(video)
    val serverCommercialScore = when {
        COMMERCIAL_FEEDBACK_MARKERS.any(feedbackText::contains) -> 90
        goto.isNotBlank() && goto !in PLAYABLE_VIDEO_GOTO_TYPES -> 70
        COMMERCIAL_ROUTE_MARKERS.any(routeText::contains) -> 60
        else -> 0
    }
    // Engagement and duration are audience signals, not evidence of a
    // commercial placement. Using them here promoted ordinary low-engagement
    // or short videos to the front and made the splash ad prone to false
    // positives. Only explicit server/route/offer signals classify content as
    // marketing.
    return serverCommercialScore + trafficCardScore
}

internal fun containsManualAdKeyword(video: VideoItem): Boolean {
    val text = "${video.title} ${video.owner.name} ${video.tname}".lowercase()
    return MANUAL_AD_KEYWORDS.any(text::contains)
}

/**
 * Traffic-card ads share a bundle of offer mechanics rather than one title keyword:
 * telecom product noun + quota/unit + price/term, or national data + call minutes.
 */
internal fun resolveTrafficCardScore(video: VideoItem): Int {
    val text = "${video.title} ${video.owner.name} ${video.tname} ${video.contentType}".lowercase()
    val telecomProduct = listOf("流量卡", "电话卡", "套餐", "月租", "号卡", "通信卡").count(text::contains)
    val quota = Regex("\\b\\d{2,4}\\s*(g|gb|兆|m)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
        text.contains("全国流量") || text.contains("通用流量")
    val price = Regex("(?:￥|¥|\\b\\d{1,3})\\s*元").containsMatchIn(text) || text.contains("元/月")
    val minutes = Regex("\\b\\d{2,4}\\s*分钟").containsMatchIn(text)
    val reviewOrSalesContext = listOf("测评局", "评测", "开卡", "办卡", "激活", "运营商", "移动", "联通", "电信")
        .any(text::contains)
    return when {
        telecomProduct > 0 && quota && price && (minutes || reviewOrSalesContext) -> 92
        telecomProduct > 0 && quota && (minutes || reviewOrSalesContext) -> 68
        else -> 0
    }
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

internal fun resolveAdModeRelatedBannerTarget(
    videos: List<RelatedVideo>,
    curatedAds: List<VideoItem> = emptyList(),
    random: Random = Random.Default,
): PromotionBannerTarget? {
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
                isAd = related.isAd,
            )
        },
    )
    // Never label an arbitrary related video as an ad just because it is the
    // first result. A banner requires a positive commercial signal.
    val candidates = ranked.filter {
        it.bvid.isNotBlank() && (containsManualAdKeyword(it) || it.isAd)
    }
    val fallbackCandidates = curatedAds.filter {
        it.bvid.isNotBlank() && (containsManualAdKeyword(it) || it.isAd)
    }
    val pool = (candidates + fallbackCandidates).distinctBy { it.bvid }
    if (pool.isEmpty()) return null
    return pool[random.nextInt(pool.size)].toPromotionBannerTarget()
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
