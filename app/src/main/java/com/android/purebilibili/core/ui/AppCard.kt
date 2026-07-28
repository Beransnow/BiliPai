package com.android.purebilibili.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

object AppCardDefaults {
    val tone: AppCardTone = AppCardTone.STANDARD
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: AppCardTone = AppCardDefaults.tone,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val visualSpec = resolveAppCardVisualSpec(
        uiStyle = LocalUiStyle.current,
        tone = tone,
    )
    val containerColor = resolveAppCardContainerColor(visualSpec)

    when (visualSpec.renderer) {
        AppCardRenderer.MIUIX_CARD -> MiuixCard(
            modifier = modifier,
            cornerRadius = AppShapes.containerCornerDp(ContainerLevel.Card),
            insideMargin = PaddingValues(0.dp),
            colors = MiuixCardDefaults.defaultColors(
                color = containerColor,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
            pressFeedbackType = if (onClick != null) PressFeedbackType.Sink else PressFeedbackType.None,
            onClick = onClick,
        ) {
            Box(modifier = Modifier.fillMaxWidth(), content = content)
        }

        AppCardRenderer.MATERIAL_SURFACE -> {
            val border = visualSpec.borderWidthDp
                .takeIf { it > 0f }
                ?.let { width ->
                    BorderStroke(
                        width = width.dp,
                        color = AppSurfaceTokens.divider().copy(alpha = visualSpec.borderAlpha),
                    )
                }
            val cardContent: @Composable () -> Unit = {
                Box(modifier = Modifier.fillMaxWidth(), content = content)
            }
            if (onClick != null) {
                Surface(
                    onClick = onClick,
                    modifier = modifier,
                    shape = AppShapes.borderedContainer(ContainerLevel.Card),
                    color = containerColor,
                    contentColor = AppSurfaceTokens.onSurface(),
                    border = border,
                    tonalElevation = visualSpec.tonalElevationDp.dp,
                    shadowElevation = visualSpec.shadowElevationDp.dp,
                    content = cardContent,
                )
            } else {
                Surface(
                    modifier = modifier,
                    shape = if (border == null) {
                        AppShapes.container(ContainerLevel.Card)
                    } else {
                        AppShapes.borderedContainer(ContainerLevel.Card)
                    },
                    color = containerColor,
                    contentColor = AppSurfaceTokens.onSurface(),
                    border = border,
                    tonalElevation = visualSpec.tonalElevationDp.dp,
                    shadowElevation = visualSpec.shadowElevationDp.dp,
                    content = cardContent,
                )
            }
        }
    }
}

@Composable
private fun resolveAppCardContainerColor(visualSpec: AppCardVisualSpec): Color {
    val baseColor = when (visualSpec.containerRole) {
        AppCardContainerRole.CARD -> AppSurfaceTokens.cardContainer()
        AppCardContainerRole.SURFACE -> AppSurfaceTokens.surface()
        AppCardContainerRole.SURFACE_VARIANT -> MaterialTheme.colorScheme.surfaceVariant
        AppCardContainerRole.SURFACE_CONTAINER -> AppSurfaceTokens.surfaceContainer()
    }
    return baseColor.copy(alpha = baseColor.alpha * visualSpec.containerAlpha)
}
