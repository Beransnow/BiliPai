package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.PluginVideoCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class AdModePluginPolicyTest {
    @Test
    fun marketingSignalsRankAboveNeutralLongFormVideo() {
        val marketing = PluginVideoCandidate(
            bvid = "BV1AD",
            title = "限时优惠新品开箱测评",
            durationSeconds = 45,
            playCount = 100_000,
            likeCount = 300,
        )
        val neutral = PluginVideoCandidate(
            bvid = "BV1OK",
            title = "城市历史影像修复纪录片",
            durationSeconds = 2_400,
            playCount = 100_000,
            likeCount = 12_000,
        )

        assertTrue(marketingScore(marketing) > marketingScore(neutral))
    }
}
