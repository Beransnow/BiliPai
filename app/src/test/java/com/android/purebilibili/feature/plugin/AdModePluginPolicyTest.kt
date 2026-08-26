package com.android.purebilibili.feature.plugin

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RecommendationFeedbackMetadata
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdModePluginPolicyTest {
    @Test
    fun serverCommercialSignalRanksAboveHealthyOrganicVideo() {
        val commercial = video(
            bvid = "BV1AD",
            title = "普通标题",
            stat = Stat(view = 100_000, like = 8_000),
            feedback = RecommendationFeedbackMetadata(
                goto = "av",
                reasons = listOf(RecommendationFeedbackReason(name = "减少商业推广")),
            ),
        )
        val organic = video(
            bvid = "BV1OK",
            title = "普通标题",
            stat = Stat(view = 100_000, like = 12_000, coin = 5_000, favorite = 8_000),
        )

        assertTrue(marketingScore(commercial) > marketingScore(organic))
    }

    @Test
    fun serverAdFlagIsAuthoritative() {
        val flagged = video("BV1FLAGGED").copy(isAd = true)
        val organic = video("BV1ORGANIC")

        assertEquals(100, marketingScore(flagged))
        assertTrue(marketingScore(flagged) > marketingScore(organic))
    }

    @Test
    fun manuallyCuratedKeywordIsCommercialEvidence() {
        val trafficCard = video("BV1CARD", title = "19元流量卡全国通用")
        assertTrue(containsManualAdKeyword(trafficCard))
        assertTrue(marketingScore(trafficCard) >= 85)
    }

    @Test
    fun trafficCardOfferPatternIsRecognizedWithoutSingleKeywordRule() {
        val trafficCard = video(
            bvid = "BV1CARD",
            title = "长期19元直接终结比赛",
            stat = Stat(view = 30_000, like = 2_000),
        ).copy(
            tname = "流量卡测评局",
        )
        val organic = video(bvid = "BV1ORG", title = "长期城市影像记录")

        assertTrue(resolveTrafficCardScore(trafficCard) >= 68)
        assertTrue(marketingScore(trafficCard) > marketingScore(organic))
    }

    @Test
    fun trafficCardNeedsOfferBundleNotJustTheWordTraffic() {
        val generic = video(bvid = "BV1GEN", title = "流量卡发展史")

        assertEquals(0, resolveTrafficCardScore(generic))
    }

    @Test
    fun titleKeywordsDoNotDriveClassification() {
        val keywordHeavy = video(
            bvid = "BV1KEY",
            title = "必买 测评 种草 优惠 限时 新品 同款",
            stat = Stat(view = 100_000, like = 12_000, coin = 5_000, favorite = 8_000),
        )
        val neutral = keywordHeavy.copy(bvid = "BV1N", title = "普通纪录片")

        assertEquals(marketingScore(neutral), marketingScore(keywordHeavy))
    }

    @Test
    fun unknownEngagementDoesNotCountAsLowQuality() {
        val unknown = video(bvid = "BV1ZERO", stat = Stat(view = 1_000_000))

        assertEquals(0, marketingScore(unknown))
    }

    @Test
    fun scoreUsesLongArithmeticForLargeCounters() {
        val large = video(
            bvid = "BV1MAX",
            stat = Stat(
                view = Int.MAX_VALUE,
                like = Int.MAX_VALUE,
                coin = Int.MAX_VALUE,
                favorite = Int.MAX_VALUE,
            ),
        )

        assertEquals(0, marketingScore(large))
    }

    @Test
    fun equalScoresPreserveOriginalOrder() {
        val items = listOf("BV1A", "BV1B", "BV1C").map { video(it) }

        assertEquals(items.map { it.bvid }, rankMarketingVideos(items).map { it.bvid })
    }

    @Test
    fun transformKeepsAllItemsAndMarksAtMostTwoHighConfidenceAds() {
        val items = (1..7).map { index -> video("BV$index") }

        val result = transformAdModeFeed(items, AdModeConfig())

        assertEquals(items.size, result.size)
        assertTrue(result.count { it.promotion != null } <= 2)
    }

    @Test
    fun enabledPresentationInjectsCuratedAdCandidatesAheadOfOrganicItems() {
        val organic = video("BVORG")
        val curated = video("BVCURATED", title = "19元流量卡全国通用")

        val result = transformAdModeFeed(
            items = listOf(organic),
            config = AdModeConfig(),
            curatedAds = listOf(curated),
        )

        assertEquals("BVCURATED", result.first().bvid)
        assertTrue(result.first().promotion != null)
    }

    @Test
    fun disabledPresentationReturnsUndecoratedItems() {
        val items = listOf(
            video("BV1OFF", feedback = RecommendationFeedbackMetadata(goto = "mall")),
            video("BV2OFF"),
        )

        val result = transformAdModeFeed(
            items,
            AdModeConfig(advertiseCards = false, showPageBanners = false),
        )

        assertEquals(items, result)
    }

    @Test
    fun weakEngagementAndShortDurationAreNotCommercialEvidence() {
        val ordinary = video(
            bvid = "BV1ORDINARY",
            stat = Stat(view = 1_000_000, like = 1),
        ).copy(duration = 30)

        assertEquals(0, marketingScore(ordinary))
    }

    @Test
    fun splashCandidateRequiresCommercialEvidence() {
        val ordinary = video("BV1ORDINARY")
        val commercial = video(
            "BV1COMMERCIAL",
            feedback = RecommendationFeedbackMetadata(goto = "mall"),
        )

        assertEquals(commercial, resolveAdModeSplashCandidate(listOf(ordinary, commercial)))
        assertEquals(null, resolveAdModeSplashCandidate(listOf(ordinary)))
    }

    @Test
    fun oldConfigMigratesWithNewCardSettingEnabled() {
        val migrated = migrateAdModeConfig(
            raw = """{"showSplashAd":false,"showPageBanners":false,"prioritizeMarketingVideos":true}""",
            json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        )

        assertFalse(migrated.showSplashAd)
        assertFalse(migrated.showPageBanners)
        assertTrue(migrated.advertiseCards)
    }

    @Test
    fun splashSessionCanOnlyBeConsumedOnceUntilReset() {
        val session = AdModeSplashSession()

        assertTrue(session.canOffer())
        session.consume()
        assertFalse(session.canOffer())
        session.reset()
        assertTrue(session.canOffer())
    }

    private fun video(
        bvid: String,
        title: String = "普通视频",
        stat: Stat = Stat(),
        feedback: RecommendationFeedbackMetadata? = null,
    ) = VideoItem(
        bvid = bvid,
        title = title,
        owner = Owner(name = "UP"),
        stat = stat,
        duration = 600,
        recommendationFeedback = feedback,
    )
}
