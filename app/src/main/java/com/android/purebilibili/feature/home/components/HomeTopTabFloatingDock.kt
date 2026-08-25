package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import com.android.purebilibili.core.ui.AppTopTabPresentation
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import top.yukonga.miuix.kmp.blur.Backdrop

internal fun shouldHomeTopTabUseFloatingBottomBarDock(
    skinPlainStyle: Boolean,
    hasSkinStickerIcons: Boolean,
    presentation: AppTopTabPresentation,
    liquidGlassEnabled: Boolean,
    selectionIndicatorStyle: HomeSelectionIndicatorStyle,
): Boolean {
    if (skinPlainStyle || hasSkinStickerIcons) return false
    if (presentation == AppTopTabPresentation.MOVING_CAPSULE) return true
    if (selectionIndicatorStyle == HomeSelectionIndicatorStyle.CAPSULE) return true
    return liquidGlassEnabled
}

internal fun shouldHomeTopTabChromeDrawOuterShell(
    drawOuterChrome: Boolean,
    innerOwnsFloatingDock: Boolean,
): Boolean = drawOuterChrome && !innerOwnsFloatingDock

/**
 * Same compact width as the home bottom bar: 76dp icon+text slots, 8dp shell inset.
 * The old wrap dock used 84dp slots and read a full circle larger.
 */
internal fun resolveHomeTopTabFloatingDockWidth(
    containerWidth: Dp,
    itemCount: Int,
    labelMode: Int,
): Dp = resolveBiliPaiFloatingBottomBarWidth(
    containerWidth = containerWidth,
    itemCount = itemCount,
    minEdgePadding = AppSpacingTokens.None,
    labelMode = labelMode,
    cornerRadius = FloatingBottomBarDefaultShellHeight / 2,
)

/**
 * Top category dock built from the same [FloatingBottomBar] as the home bottom bar:
 * 56dp shell, 52dp indicator, 4dp inset. 仅文字用更大字号填满胶囊，图标+文字仍用 caption。
 */
@Composable
internal fun HomeTopTabFloatingDock(
    categories: List<String>,
    categoryKeys: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onReselected: () -> Unit,
    showIcon: Boolean,
    showText: Boolean,
    iconFamily: AppSemanticIconFamily,
    itemWidth: Dp?,
    labelFontSize: TextUnit,
    liquidGlassEffectsEnabled: Boolean,
    miuixBackdrop: Backdrop?,
    liquidGlassTuning: LiquidGlassTuning,
    indicatorPositionProvider: (() -> Float)?,
    isScrollInProgressProvider: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    BottomBarFloatingSegmentedControl(
        items = categories,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        modifier = modifier,
        enabled = true,
        itemWidth = itemWidth,
        height = FloatingBottomBarDefaultShellHeight,
        indicatorHeight = FloatingBottomBarIndicatorHeight,
        labelFontSize = labelFontSize,
        containerHorizontalPadding = AppSpacingTokens.ExtraSmall,
        containerVerticalPadding = AppSpacingTokens.ExtraSmall,
        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled,
        dragSelectionEnabled = true,
        longPressDragSelectionEnabled = false,
        miuixBackdrop = miuixBackdrop,
        selectedTextColorOverride = MaterialTheme.colorScheme.primary,
        unselectedTextColorOverride = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorPositionProvider = indicatorPositionProvider,
        isScrollInProgressProvider = isScrollInProgressProvider,
        onIndicatorPositionChanged = null,
        liquidGlassTuningOverride = liquidGlassTuning,
        onItemReselected = onReselected,
        itemContent = { index, label, selected ->
            val contentColor = LocalFloatingBottomBarContentColor.current
            val selectionScale = LocalFloatingBottomBarItemSelectionScale.current
            val categoryKey = categoryKeys.getOrNull(index) ?: label
            if (showIcon) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        val scale = selectionScale()
                        scaleX = scale
                        scaleY = scale
                        clip = false
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = resolveTopTabCategoryIcon(
                            categoryKey = categoryKey,
                            iconFamily = iconFamily,
                            selected = selected,
                        ),
                        contentDescription = label,
                        tint = contentColor,
                    )
                }
            }
            if (showText) {
                AppText(
                    text = label,
                    color = contentColor,
                    fontSize = labelFontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    tapToCopyEnabled = false,
                )
            }
        },
    )
}
