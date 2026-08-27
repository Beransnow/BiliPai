package com.android.purebilibili.feature.space

import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.SpaceArticleItem

internal fun buildSpaceArticleStatsText(article: SpaceArticleItem): String {
    return buildList {
        add(article.category?.name?.takeIf { it.isNotBlank() } ?: "图文")
        article.stats?.view
            ?.takeIf { it > 0 }
            ?.let { add("${FormatUtils.formatStat(it.toLong())}阅读") }
        add("${FormatUtils.formatStat(article.stats?.like?.toLong() ?: 0)}点赞")
    }.joinToString(" · ")
}
