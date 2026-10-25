package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 播放叠层使用固定弹幕字形，与页面主题的评论图标分开。 */
@Composable
internal fun LiveDanmakuControlIcon(
    enabled: Boolean = true,
    settings: Boolean = false,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(19.dp).border(1.5.dp, tint, RoundedCornerShape(4.dp))
                .drawWithContent {
                    drawContent()
                    if (!enabled) drawLine(tint, Offset(1f, size.height - 1f), Offset(size.width - 1f, 1f), 1.5.dp.toPx())
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("弹", color = tint, fontSize = 12.sp, lineHeight = 14.sp)
        }
        if (settings) Icon(
            Icons.Outlined.Settings, contentDescription = null, tint = tint,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp)
                .background(Color.Black, RoundedCornerShape(3.dp)).size(10.dp),
        )
    }
}
