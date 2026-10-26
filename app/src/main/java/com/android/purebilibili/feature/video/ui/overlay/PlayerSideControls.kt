package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.android.purebilibili.core.ui.LocalAppButtonSlotColorCorrectionEnabled
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.components.AppSurface

/** Side actions share geometry and hit bounds; only their material follows the glass preference. */
@Composable
internal fun PlayerSideButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 24,
    iconSizeDp: Int = 24,
) {
    PlayerControlTouchBounds(height = PlayerChromeLayout.CircularButtonSizeDp.dp) {
        PlayerControlGlassDock(
            enabled = true,
            shape = CircleShape,
            minimumReadabilityScrimAlpha = if (LocalPlayerControlGlassEnabled.current) 0f else 0.6f,
            modifier = modifier.size(PlayerChromeLayout.CircularButtonSizeDp.dp),
        ) {
            CompositionLocalProvider(LocalAppButtonSlotColorCorrectionEnabled provides false) {
                AppSurface(
                    onClick = onClick,
                    color = Color.Transparent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(cornerRadiusDp.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppIcon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            tint = Color.White,
                            modifier = Modifier.size(iconSizeDp.dp),
                        )
                    }
                }
            }
        }
    }
}
