package com.android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypographyTokens {
    val ZeroLetterSpacing = 0.sp
}

/** Typography roles shared by feed cards regardless of their visual composition. */
data class FeedContentTypography(
    val title: TextStyle,
    val author: TextStyle,
    val statistic: TextStyle,
    val coverBadge: TextStyle,
)

@Composable
fun feedContentTypography(): FeedContentTypography {
    return FeedContentTypography(
        title = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        author = MaterialTheme.typography.labelMedium,
        statistic = MaterialTheme.typography.labelSmall,
        coverBadge = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
    )
}
