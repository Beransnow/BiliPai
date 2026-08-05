package com.android.purebilibili.feature.live

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiStyle
import com.android.purebilibili.core.theme.resolveUiStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText

/**
 * PiliPlus 风格的直播首页分区/标签 chip，按当前 UI 预设分发原生组件：
 * - Material 3：FilterChip
 * - Miuix：圆角 Surface + Miuix 色板（KMP 无独立 Chip API 时的原生桥接）
 * - iOS：胶囊 Surface（Cupertino 风格轻量 chip）
 */
@Composable
fun LiveHomeSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val uiStyle = resolveUiStyle(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current,
    )
    when (uiStyle) {
        UiStyle.MATERIAL3 -> {
            AppFilterChip(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                label = {
                    AppText(
                        text = label,
                        style = if (compact) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
        UiStyle.MIUIX -> {
            val container = if (selected) {
                AppSurfaceTokens.secondaryContainer()
            } else {
                Color.Transparent
            }
            val content = if (selected) {
                AppSurfaceTokens.onSecondaryContainer()
            } else {
                AppSurfaceTokens.onSurfaceVariantSummary()
            }
            AppSurface(
                onClick = onClick,
                modifier = modifier,
                color = container,
                contentColor = content,
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                AppText(
                    text = label,
                    color = content,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(
                        horizontal = AppSpacingTokens.Small,
                        vertical = if (compact) 5.dp else AppSpacingTokens.ExtraSmall,
                    ),
                )
            }
        }
        UiStyle.IOS -> {
            val container = if (selected) {
                AppSurfaceTokens.secondaryContainer()
            } else {
                Color.Transparent
            }
            val content = if (selected) {
                AppSurfaceTokens.onSecondaryContainer()
            } else {
                AppSurfaceTokens.onSurfaceVariantSummary()
            }
            AppSurface(
                onClick = onClick,
                modifier = modifier,
                color = container,
                contentColor = content,
                shape = AppShapes.container(ContainerLevel.Pill),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                AppText(
                    text = label,
                    color = content,
                    style = if (compact) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(
                        PaddingValues(
                            horizontal = AppSpacingTokens.Small,
                            vertical = if (compact) 5.dp else AppSpacingTokens.ExtraSmall,
                        )
                    ),
                )
            }
        }
    }
}
