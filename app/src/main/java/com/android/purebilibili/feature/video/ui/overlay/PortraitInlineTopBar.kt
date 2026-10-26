package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.feature.video.ui.overlay.portrait.resolvePortraitTopBarLayoutPolicy
import com.android.purebilibili.feature.video.ui.overlay.portrait.shouldApplyStatusBarPaddingToPortraitTopBar

/** Main's glass inline chrome; the more target now opens the shared native settings directly. */
@Composable
internal fun PortraitInlineTopBar(
    onlineCount: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit,
    onAudioMode: () -> Unit,
    isAudioOnly: Boolean,
    viewportWidthDpOverride: Int?,
    onCastClick: () -> Unit,
    showCastButton: Boolean,
    compactPlayerChrome: Boolean,
    statusBarVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val width = viewportWidthDpOverride ?: LocalConfiguration.current.screenWidthDp
    val layout = remember(width, compactPlayerChrome) {
        resolvePortraitTopBarLayoutPolicy(width.coerceAtLeast(1), compactPlayerChrome)
    }
    val moreIcon = rememberAppMoreIcon()
    val shareIcon = rememberAppShareIcon()
    Row(
        modifier.fillMaxWidth()
            .then(if (shouldApplyStatusBarPaddingToPortraitTopBar(statusBarVisible)) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = layout.horizontalPaddingDp.dp, vertical = layout.verticalPaddingDp.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.leftSectionSpacingDp.dp),
        ) {
            PlayerControlGlassDock(enabled = true, shape = CircleShape) {
                AppIconButton(onClick = onBack, modifier = Modifier.size(layout.buttonSizeDp.dp)) {
                    AppIcon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回", tint = Color.White,
                        modifier = Modifier.size(layout.iconSizeDp.dp))
                }
            }
            if (onlineCount.isNotEmpty()) {
                AppText(onlineCount, color = Color.White.copy(alpha = 0.7f),
                    fontSize = layout.onlineCountFontSp.sp, fontWeight = FontWeight.Medium)
            }
        }
        PlayerControlGlassDock(enabled = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(layout.rightSectionSpacingDp.dp)) {
                AppIconButton(
                    onClick = onAudioMode,
                    modifier = Modifier.size(layout.buttonSizeDp.dp)
                        .then(if (isAudioOnly) Modifier.background(AppContentColors.accent(), CircleShape) else Modifier),
                ) {
                    AppIcon(Icons.Outlined.Headphones, "听视频",
                        tint = if (isAudioOnly) AppContentColors.onAccent() else Color.White,
                        modifier = Modifier.size(layout.iconSizeDp.dp))
                }
                if (showCastButton) {
                    AppIconButton(onClick = onCastClick, modifier = Modifier.size(layout.buttonSizeDp.dp)) {
                        AppIcon(Icons.Outlined.Tv, "投屏", tint = Color.White, modifier = Modifier.size(layout.iconSizeDp.dp))
                    }
                }
                AppIconButton(onClick = onSettings, modifier = Modifier.size(layout.buttonSizeDp.dp)) {
                    AppIcon(moreIcon, "更多设置", tint = Color.White, modifier = Modifier.size(layout.iconSizeDp.dp))
                }
                if (!compactPlayerChrome) {
                    AppIconButton(onClick = onShare, modifier = Modifier.size(layout.buttonSizeDp.dp)) {
                        AppIcon(shareIcon, "分享", tint = Color.White, modifier = Modifier.size(layout.iconSizeDp.dp))
                    }
                }
            }
        }
    }
}
