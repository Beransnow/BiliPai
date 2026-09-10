package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.feature.home.components.liquid.rememberCombinedBackdrop
import com.android.purebilibili.feature.home.components.miuix.DampedDragAnimation
import com.android.purebilibili.feature.home.components.miuix.DampedDragTrackingMode

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
    val fallbackPageBackdrop = rememberLayerBackdrop()
    val pageBackdrop = miuixBackdrop ?: fallbackPageBackdrop
    val dockContentBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(pageBackdrop, dockContentBackdrop)
    val dragAnimation = remember(scope, dockItems.size, slotPx) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..lastIndex.toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = FloatingBottomBarPressedScale,
            trackingMode = DampedDragTrackingMode.DIRECT,
            onDragStarted = { haptic(HapticType.LIGHT) },
            onDragStopped = {
                val target = targetValue.roundToInt().coerceIn(0, lastIndex)
                animateToValue(target.toFloat(), animatePress = false)
                itemsLatest.getOrNull(target)?.let {
                    haptic(HapticType.MEDIUM)
                    onItemClickLatest(it)
                }
            },
            onDrag = { _, amount ->
                updateValue(
                    (targetValue + amount.y / slotPx).coerceIn(0f, lastIndex.toFloat())
                )
            },
        )
    }
    val indicatorPosition = dragAnimation.value

    LaunchedEffect(selectedIndex, dragAnimation) {
        if (!dragAnimation.isDragging) {
            dragAnimation.animateToValue(selectedIndex.toFloat())
        }
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
            .padding(LargeScreenDockPadding),
    ) {
        Column(
            modifier = Modifier.layerBackdrop(dockContentBackdrop),
            verticalArrangement = Arrangement.spacedBy(LargeScreenDockGap),
        ) {
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

        val pressProgress = dragAnimation.pressProgress
        val refractionMotion = resolveBottomBarRefractionMotionProfile(
            position = dragAnimation.value,
            velocity = dragAnimation.velocity,
            isDragging = dragAnimation.isDragging,
            motionSpec = motionSpec,
        )
        val motionProgress = resolveSegmentedControlMotionProgress(
            pressProgress = pressProgress,
            refractionProgress = refractionMotion.progress,
            tapPressRefractionEnabled = true,
        )
        val dragScaleProgress = rememberBottomBarIndicatorDragScaleProgress(
            isDragging = dragAnimation.isDragging
        )
        val isDarkTheme = resolveBottomBarDarkTheme(AppSurfaceTokens.background())
        BottomBarMatchedLiquidIndicator(
            visible = true,
            dockContentAlpha = 1f,
            indicatorTranslationXPx = 0f,
            indicatorTranslationYPx = indicatorPosition * slotPx,
            indicatorPanelOffsetPx = 0f,
            indicatorWidth = LargeScreenDockItemSize,
            indicatorHeight = LargeScreenDockItemSize,
            shellShape = shellShape,
            liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset,
            contentBackdrop = combinedBackdrop,
            backdrop = pageBackdrop,
            indicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(pressProgress),
            liquidGlassTuning = tuning,
            effectivePressProgress = pressProgress,
            indicatorIdleSurfaceColor = resolveAndroidNativeIdleIndicatorSurfaceColor(isDarkTheme),
            glassEnabled = true,
            motionProgress = motionProgress,
            velocityItemsPerSecond = dragAnimation.velocity,
            isDragging = dragAnimation.isDragging,
            indicatorLayerScaleProgress = maxOf(dragScaleProgress, pressProgress),
            dragScaleTarget = FloatingBottomBarPressedScale,
            bottomBarMotionSpec = motionSpec,
            isDarkTheme = isDarkTheme,
            orientation = BottomBarLiquidOrientation.VERTICAL,
            indicatorAlignment = Alignment.TopStart,
            interactionModifier = dragAnimation.modifier,
        )
    }
}
