// 文件路径: feature/dynamic/components/DynamicTopBar.kt
package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  Material Icons
import com.android.purebilibili.core.ui.rememberAppGridLayoutIcon
import com.android.purebilibili.core.ui.rememberAppListLayoutIcon
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarHorizontalPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarLiquidTabSpec
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl

//  动态页面布局模式
enum class DynamicDisplayMode {
    SIDEBAR,
    SIDEBAR_RIGHT,
    HORIZONTAL,
    DRAWER_LEFT,
    DRAWER_RIGHT
}

internal fun DynamicDisplayMode.isHorizontalUserList(): Boolean = this == DynamicDisplayMode.HORIZONTAL

internal fun DynamicDisplayMode.isFixedSidebar(): Boolean =
    this == DynamicDisplayMode.SIDEBAR || this == DynamicDisplayMode.SIDEBAR_RIGHT

internal fun DynamicDisplayMode.isRightAligned(): Boolean =
    this == DynamicDisplayMode.SIDEBAR_RIGHT || this == DynamicDisplayMode.DRAWER_RIGHT

internal fun DynamicDisplayMode.isDrawer(): Boolean =
    this == DynamicDisplayMode.DRAWER_LEFT || this == DynamicDisplayMode.DRAWER_RIGHT

internal fun resolveDynamicDisplayModeLabel(mode: DynamicDisplayMode): String = when (mode) {
    DynamicDisplayMode.SIDEBAR -> "左侧竖条"
    DynamicDisplayMode.SIDEBAR_RIGHT -> "右侧竖条"
    DynamicDisplayMode.HORIZONTAL -> "顶部横条"
    DynamicDisplayMode.DRAWER_LEFT -> "左侧抽屉"
    DynamicDisplayMode.DRAWER_RIGHT -> "右侧抽屉"
}

/**
 *  带Tab的顶栏
 */
@Composable
fun DynamicTopBarWithTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: DynamicDisplayMode = DynamicDisplayMode.SIDEBAR,
    onDisplayModeChange: (DynamicDisplayMode) -> Unit = {},
    onPublishClick: (() -> Unit)? = null,
    indicatorPositionProvider: (() -> Float)? = null,
    isScrollInProgressProvider: () -> Boolean = { false },
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val liquidTabSpec = resolveDynamicTopBarLiquidTabSpec()

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(statusBarHeight))

        // 悬浮 Dock 与内容彻底解耦：不铺满顶部、不读取 haze，也不绘制毛玻璃背景。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(liquidTabSpec.heightDp.dp)
                .padding(horizontal = resolveDynamicTopBarHorizontalPadding()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSurface(
                modifier = Modifier.weight(1f),
                shape = AppShapes.container(ContainerLevel.Pill),
                color = AppSurfaceTokens.surfaceContainerHigh(),
                shadowElevation = AppSpacingTokens.ExtraSmall,
            ) {
                BottomBarLiquidSegmentedControl(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelected = onTabSelected,
                    modifier = Modifier.fillMaxWidth(),
                    height = liquidTabSpec.heightDp.dp,
                    indicatorHeight = liquidTabSpec.indicatorHeightDp.dp,
                    labelFontSize = liquidTabSpec.labelFontSizeSp.sp,
                    indicatorPositionProvider = indicatorPositionProvider,
                    isScrollInProgressProvider = isScrollInProgressProvider,
                    forceLiquidChrome = true,
                    liquidGlassEffectsEnabled = true,
                    miuixBackdrop = null,
                    containerColorOverride = AppSurfaceTokens.surfaceContainerHigh(),
                )
            }

            AppSurface(
                shape = AppShapes.container(ContainerLevel.Pill),
                color = AppSurfaceTokens.surfaceContainerHigh(),
                shadowElevation = AppSpacingTokens.ExtraSmall,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showLayoutMenu by remember { mutableStateOf(false) }
                    Box {
                        AppIconButton(
                            onClick = { showLayoutMenu = true },
                            modifier = Modifier.size(AppChromeSizeTokens.MinimumTouchTarget)
                        ) {
                            AppIcon(
                                imageVector = if (displayMode.isHorizontalUserList())
                                    rememberAppGridLayoutIcon() else rememberAppListLayoutIcon(),
                                contentDescription = "关注列表位置",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(AppSpacingTokens.ExtraLarge - AppSpacingTokens.Micro)
                            )
                        }
                        AppDropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false }
                        ) {
                            DynamicDisplayMode.entries.forEach { mode ->
                                AppDropdownMenuItem(
                                    text = { AppText(resolveDynamicDisplayModeLabel(mode)) },
                                    onClick = {
                                        showLayoutMenu = false
                                        onDisplayModeChange(mode)
                                    }
                                )
                            }
                        }
                    }

                    //  发布动态入口（对齐 BiliPai AppBar actions 的发布按钮）
                    if (onPublishClick != null) {
                        AppIconButton(
                            onClick = onPublishClick,
                            modifier = Modifier.size(AppChromeSizeTokens.MinimumTouchTarget)
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "发布动态",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(AppSpacingTokens.ExtraLarge - AppSpacingTokens.Micro)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun resolveDynamicTabSelectedColor(primaryColor: Color): Color = primaryColor
