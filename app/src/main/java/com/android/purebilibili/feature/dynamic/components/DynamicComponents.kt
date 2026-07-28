package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppCard
import com.android.purebilibili.core.ui.AppCardTone

/**
 *  Dynamic 模块专用的 GlassCard 组件
 *  针对列表性能进行了微调，减少过多层级
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AppCard(modifier = modifier, tone = AppCardTone.GLASS, content = content)
}
