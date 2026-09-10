package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private val LargeScreenDockCrossAxisSize = 64.dp
private val LargeScreenDockSlotSize = 56.dp
private val LargeScreenDockMainAxisPadding = 4.dp

/**
 * The home floating dock rotated onto its vertical axis. Shell, indicator, capture layers,
 * drag kernel, enlargement, velocity deformation and chromatic dispersion all remain owned by
 * [FloatingBottomBar]; this wrapper only swaps the visual axis and restores upright item content.
 */
@Composable
fun LargeScreenFloatingDock(
    currentItem: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit,
    visibleItems: List<BottomNavItem>,
    itemLabels: Map<String, String>,
    homeSettings: HomeSettings,
    dynamicUnreadCount: Int = 0,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dockItems = remember(
        visibleItems,
        homeSettings.isBottomBarSearchEnabled,
        homeSettings.bottomBarSearchLayoutMode,
    ) {
        resolveBottomBarVisibleItemsForSearchMode(
            visibleItems = visibleItems,
            bottomBarSearchEnabled = homeSettings.isBottomBarSearchEnabled,
            searchLayoutMode = homeSettings.bottomBarSearchLayoutMode,
        )
    }
    if (dockItems.isEmpty()) return

    val searchIncluded = homeSettings.isBottomBarSearchEnabled
    val itemCount = dockItems.size + if (searchIncluded) 1 else 0
    val selectedIndex = dockItems.indexOf(currentItem).coerceAtLeast(0)
    val dockLength = LargeScreenDockSlotSize * itemCount + LargeScreenDockMainAxisPadding * 2
    val dockBackdrop = rememberLayerBackdrop()
    val tuning = remember(
        homeSettings.liquidGlassProgress,
        homeSettings.liquidGlassAdvancedSettings,
        homeSettings.liquidGlassReadabilityMode,
    ) {
        resolveLiquidGlassTuning(
            homeSettings.liquidGlassProgress,
            homeSettings.liquidGlassAdvancedSettings,
            homeSettings.liquidGlassReadabilityMode,
        )
    }
    val haptic = rememberHapticFeedback()
    val selectIndex: (Int) -> Unit = { index ->
        if (index == dockItems.size && searchIncluded) {
            haptic(HapticType.LIGHT)
            onSearchClick()
        } else {
            dockItems.getOrNull(index)?.let { item ->
                haptic(HapticType.LIGHT)
                onItemClick(item)
            }
        }
    }

    Box(
        modifier = modifier
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
            .size(width = LargeScreenDockCrossAxisSize, height = dockLength),
        contentAlignment = Alignment.Center,
    ) {
        // Match PartitionSideRail: provide a neutral local page capture instead of sampling the
        // video/feed behind the dock. FloatingBottomBar still owns the second content capture.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f)
                .layerBackdrop(dockBackdrop)
                .background(AppSurfaceTokens.background())
        )

        Box(
            modifier = Modifier
                .size(width = dockLength, height = LargeScreenDockCrossAxisSize)
                .graphicsLayer {
                    rotationZ = 90f
                    clip = false
                },
            contentAlignment = Alignment.Center,
        ) {
            FloatingBottomBar(
                selectedIndex = { selectedIndex },
                onSelected = selectIndex,
                onReselected = { dockItems.getOrNull(selectedIndex)?.let(onItemClick) },
                backdrop = dockBackdrop,
                tabsCount = itemCount,
                modifier = Modifier.fillMaxSize(),
                mode = FloatingBottomBarMode.LiquidGlass,
                shellHeight = LargeScreenDockCrossAxisSize,
                indicatorHeight = resolveBiliPaiBottomBarIndicatorHeight(
                    LargeScreenDockCrossAxisSize
                ),
                contentHorizontalPadding = LargeScreenDockMainAxisPadding,
                contentVerticalPadding = LargeScreenDockMainAxisPadding,
                dragSelectionEnabled = true,
                liquidGlassTuning = tuning,
            ) {
                dockItems.forEachIndexed { index, item ->
                    LargeScreenRotatedDockItem(
                        index = index,
                        item = item,
                        selectedIndex = selectedIndex,
                        itemLabels = itemLabels,
                        dynamicUnreadCount = dynamicUnreadCount,
                        onClick = { selectIndex(index) },
                    )
                }
                if (searchIncluded) {
                    FloatingBottomBarItem(
                        onClick = { selectIndex(dockItems.size) },
                        selected = false,
                        itemIndex = dockItems.size,
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer { rotationZ = -90f },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.LargeScreenRotatedDockItem(
    index: Int,
    item: BottomNavItem,
    selectedIndex: Int,
    itemLabels: Map<String, String>,
    dynamicUnreadCount: Int,
    onClick: () -> Unit,
) {
    val selected = index == selectedIndex || LocalFloatingBottomBarActiveContent.current
    FloatingBottomBarItem(
        onClick = onClick,
        selected = index == selectedIndex,
        itemIndex = index,
        iconCrossScaleEnabled = true,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    rotationZ = -90f
                    clip = false
                },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = resolveHomeNavigationBarIcon(item, selected),
                contentDescription = resolveBottomNavItemLabel(item, itemLabels),
                modifier = Modifier.size(26.dp),
            )
            if (item == BottomNavItem.DYNAMIC && dynamicUnreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .background(Color.Red, resolveSharedBottomBarCapsuleShape())
                )
            }
        }
    }
}
