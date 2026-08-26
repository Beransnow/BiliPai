package com.android.purebilibili.feature.plugin

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Assert.assertTrue
import org.junit.Test

class AdModePluginPolicyTest {
    @Test
    fun marketingSignalsRankAboveNeutralLongFormVideo() {
        val marketing = VideoItem(
            bvid = "BV1AD",
            title = "限时优惠新品开箱测评",
            duration = 45,
            owner = Owner(name = "今日严选"),
            stat = Stat(view = 100_000, like = 300),
        )
        val neutral = VideoItem(
            bvid = "BV1OK",
            title = "城市历史影像修复纪录片",
            duration = 2_400,
            owner = Owner(name = "城市档案馆"),
            stat = Stat(view = 100_000, like = 12_000),
        )

        assertTrue(marketingScore(marketing) > marketingScore(neutral))
    }
}
