package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import dev.chrisbanes.haze.HazeState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private val LargeScreenDockWidth = 64.dp
private val LargeScreenDockItemSize = 56.dp
private val LargeScreenDockPadding = 4.dp
private val LargeScreenDockGap = 2.dp

/** A floating vertical liquid-glass dock; deliberately separate from the full-height tablet rail. */
@Composable
fun LargeScreenFloatingDock(
    currentItem: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit,
    visibleItems: List<BottomNavItem>,
    itemLabels: Map<String, String>,
    homeSettings: HomeSettings,
    miuixBackdrop: LayerBackdrop?,
    hazeState: HazeState?,
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
    val selectedIndex = dockItems.indexOf(currentItem).coerceAtLeast(0)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHapticFeedback()
    val onItemClickLatest by rememberUpdatedState(onItemClick)
    val itemsLatest by rememberUpdatedState(dockItems)
    val slotPx = with(density) { (LargeScreenDockItemSize + LargeScreenDockGap).toPx() }
    val indicatorOffset = remember { Animatable(selectedIndex.toFloat()) }
    var directDragPosition by remember { mutableStateOf<Float?>(null) }
    val indicatorPosition = directDragPosition ?: indicatorOffset.value
    val motionSpec = remember { resolveSegmentedControlMotionSpec() }
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
    val shellShape = resolveSharedBottomBarCapsuleShape()
    val shellColor = AppSurfaceTokens.chromeBackground().copy(alpha = tuning.surfaceAlpha)
    val lastIndex = dockItems.lastIndex

    LaunchedEffect(selectedIndex) {
        indicatorOffset.animateTo(
            selectedIndex.toFloat(),
            animationSpec = motionSpec.drag.selectionSpring.toSpringSpec(),
        )
    }

    Box(
        modifier = modifier
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
            .width(LargeScreenDockWidth)
            .biliPaiMiuixFloatingDockSurface(
                shape = shellShape,
                backdrop = miuixBackdrop,
                containerColor = shellColor,
                blurEnabled = hazeState != null,
                glassEnabled = true,
                blurRadius = tuning.backdropBlurRadius.dp,
                hazeState = hazeState,
                motionTier = MotionTier.Normal,
                isTransitionRunning = false,
                forceLowBlurBudget = false,
                liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset,
                liquidGlassTuning = tuning,
            )
            .clip(shellShape)
            .padding(LargeScreenDockPadding)
            .pointerInput(dockItems) {
                var dragStart = indicatorOffset.value
                detectDragGestures(
                    onDragStart = {
                        dragStart = indicatorPosition
                        directDragPosition = indicatorPosition
                        haptic(HapticType.LIGHT)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        directDragPosition = (
                            (directDragPosition ?: indicatorOffset.value) + amount.y / slotPx
                            ).coerceIn(0f, lastIndex.toFloat())
                    },
                    onDragEnd = {
                        val releasePosition = indicatorPosition
                        val target = releasePosition.roundToInt().coerceIn(0, lastIndex)
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            indicatorOffset.snapTo(releasePosition)
                            directDragPosition = null
                            indicatorOffset.animateTo(
                                target.toFloat(),
                                animationSpec = motionSpec.drag.selectionSpring.toSpringSpec(),
                            )
                        }
                        itemsLatest.getOrNull(target)?.let {
                            haptic(HapticType.MEDIUM)
                            onItemClickLatest(it)
                        }
                    },
                    onDragCancel = {
                        val cancelPosition = indicatorPosition
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            indicatorOffset.snapTo(cancelPosition)
                            directDragPosition = null
                            indicatorOffset.animateTo(
                                dragStart,
                                animationSpec = motionSpec.drag.selectionSpring.toSpringSpec(),
                            )
                        }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = indicatorPosition * slotPx }
                .size(LargeScreenDockItemSize)
                .biliPaiMiuixFloatingDockSurface(
                    shape = shellShape,
                    backdrop = miuixBackdrop,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    blurEnabled = false,
                    glassEnabled = true,
                    blurRadius = tuning.backdropBlurRadius.dp,
                    hazeState = null,
                    motionTier = MotionTier.Normal,
                    isTransitionRunning = false,
                    forceLowBlurBudget = false,
                    liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset,
                    liquidGlassTuning = tuning,
                ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(LargeScreenDockGap)) {
            dockItems.forEachIndexed { index, item ->
                val selectionProgress = (1f - abs(index - indicatorPosition)).coerceIn(0f, 1f)
                val visuallySelected = selectionProgress > 0.5f
                val isSelected = item == currentItem
                val label = resolveBottomNavItemLabel(item, itemLabels)
                Box(
                    modifier = Modifier
                        .size(LargeScreenDockItemSize)
                        .semantics {
                            selected = isSelected
                        }
                        .clickable(role = Role.Tab) {
                            haptic(HapticType.LIGHT)
                            onItemClick(item)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = resolveHomeNavigationBarIcon(item, visuallySelected),
                        contentDescription = label,
                        tint = lerp(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            MaterialTheme.colorScheme.primary,
                            selectionProgress,
                        ),
                        modifier = Modifier.size(26.dp),
                    )
                    if (item == BottomNavItem.DYNAMIC && dynamicUnreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(7.dp)
                                .background(Color.Red, resolveSharedBottomBarCapsuleShape())
                        )
                    }
                }
            }
            if (homeSettings.isBottomBarSearchEnabled) {
                Box(
                    modifier = Modifier
                        .size(LargeScreenDockItemSize)
                        .clickable(role = Role.Button) {
                            haptic(HapticType.LIGHT)
                            onSearchClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = AppSurfaceTokens.onSurfaceVariantSummary(),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
