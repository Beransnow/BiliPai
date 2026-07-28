package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.resolveSharedLiquidGlassChromeEnabled
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.animation.MotionReader
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.BottomBarMotionSpec
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.android.purebilibili.feature.home.components.liquid.lens as miuixLens
import com.android.purebilibili.feature.home.components.liquid.vibrancy as miuixVibrancy
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop
import top.yukonga.miuix.kmp.blur.blur as miuixBlur
import top.yukonga.miuix.kmp.blur.drawBackdrop as miuixDrawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

@androidx.compose.runtime.Stable
private class SegmentedControlMotionReader(
    private val source: MotionReader,
    private val externalPositionProvider: State<(() -> Float)?>,
    private val itemCount: Int
) : MotionReader by source {
    override fun readPosition(): Float = resolveSegmentedControlIndicatorPosition(
        internalPosition = source.readPosition(),
        externalPosition = if (source.readDragging()) {
            null
        } else {
            externalPositionProvider.value?.invoke()
        },
        itemCount = itemCount
    )
}

private enum class SegmentedLabelLayer {
    VISIBLE,
    EXPORT,
    INPUT
}

internal fun resolveSegmentedControlLiquidGlassEnabled(
    storedLiquidGlassEnabled: Boolean,
    liquidGlassEffectsEnabled: Boolean,
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean {
    if (!liquidGlassEffectsEnabled) return false
    // Same shared contract as top dock / search / bottom bar: global master ORs
    // with the per-surface toggle and always reuses bottom-bar liquid material.
    return resolveSharedLiquidGlassChromeEnabled(
        individualEnabled = storedLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    )
}

internal enum class SegmentedControlChromeStyle {
    LIQUID_PILL,
    ANDROID_NATIVE_UNDERLINE
}

internal const val BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_HEIGHT_DP = 58
internal const val BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_INDICATOR_HEIGHT_DP = 56
private const val SEGMENTED_CONTROL_MIN_INDICATOR_ASPECT_RATIO = 1.6f

internal fun resolveSegmentedControlChromeStyle(
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean,
    preferInlineContentStyle: Boolean = false
): SegmentedControlChromeStyle {
    return if (uiPreset == UiPreset.MD3 && !androidNativeLiquidGlassEnabled) {
        SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE
    } else {
        SegmentedControlChromeStyle.LIQUID_PILL
    }
}

internal fun resolveLiquidSegmentedControlUnselectedTextColor(
    onSurface: Color,
    enabled: Boolean
): Color = if (enabled) onSurface else onSurface.copy(alpha = 0.42f)

internal fun resolveSegmentedControlIndicatorWidthDp(
    slotWidthDp: Float,
    indicatorHeightDp: Float,
    itemCount: Int
): Float {
    if (slotWidthDp <= 0f || indicatorHeightDp <= 0f || itemCount <= 0) return 0f
    return slotWidthDp
}

internal fun resolveSegmentedControlIndicatorHeightDp(
    slotWidthDp: Float,
    indicatorHeightDp: Float
): Float {
    if (slotWidthDp <= 0f || indicatorHeightDp <= 0f) return 0f
    return min(
        indicatorHeightDp,
        slotWidthDp / SEGMENTED_CONTROL_MIN_INDICATOR_ASPECT_RATIO
    )
}

internal fun resolveSegmentedControlIndicatorOffsetDp(
    position: Float,
    slotWidthDp: Float,
    contentPaddingDp: Float
): Float {
    return contentPaddingDp + (slotWidthDp * position)
}

internal fun shouldFollowSegmentedControlIndicatorDrag(
    pointerX: Float,
    indicatorPosition: Float,
    itemWidthPx: Float
): Boolean {
    if (itemWidthPx <= 0f) return false
    val startX = indicatorPosition * itemWidthPx
    val endX = startX + itemWidthPx
    return pointerX in startX..endX
}

internal fun resolveSegmentedControlSweepSelectionIndex(
    pointerX: Float,
    itemWidthPx: Float,
    itemCount: Int
): Int {
    if (itemWidthPx <= 0f || itemCount <= 0) return 0
    return (pointerX.coerceAtLeast(0f) / itemWidthPx)
        .toInt()
        .coerceIn(0, itemCount - 1)
}

internal fun resolveSegmentedControlIndicatorPosition(
    internalPosition: Float,
    externalPosition: Float?,
    itemCount: Int
): Float {
    if (itemCount <= 0) return 0f
    return (externalPosition ?: internalPosition)
        .coerceIn(0f, (itemCount - 1).toFloat())
}

internal fun shouldDrawSegmentedControlIndicatorBackdrop(
    liquidGlassEnabled: Boolean,
    motionProgress: Float,
    hasExternalBackdrop: Boolean
): Boolean {
    if (!liquidGlassEnabled) return false
    return hasExternalBackdrop || motionProgress > 0.001f
}

/**
 * Export capture may drawBackdrop only from an external page LayerBackdrop.
 * Sampling the same tabs LayerBackdrop being recorded on that node creates a
 * cyclic RenderNode graph and overflows HyperOS MiBackgroundBlurBlend.
 */
internal fun shouldDrawSegmentedControlExportCaptureBackdrop(
    liquidGlassEnabled: Boolean,
    hasExternalBackdrop: Boolean
): Boolean {
    return liquidGlassEnabled && hasExternalBackdrop
}

@Composable
internal fun BottomBarLiquidIndicatorSurface(
    modifier: Modifier = Modifier,
    shape: Shape = resolveSharedBottomBarCapsuleShape(),
    liquidGlassEnabled: Boolean,
    backdrop: Backdrop? = null,
    hasExternalBackdrop: Boolean = backdrop != null,
    indicatorLensSpec: BottomBarBackdropPresetLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
        progress = if (liquidGlassEnabled) 1f else 0f
    ),
    indicatorHighlightAlpha: Float = resolveBottomBarLiquidGlassHighlightAlpha(
        motionProgress = if (liquidGlassEnabled) 1f else 0f
    ),
    indicatorGlowAlpha: Float = resolveBottomBarIndicatorGlowAlpha(
        glassEnabled = liquidGlassEnabled,
        pressProgress = 0f
    ),
    motionProgress: Float = 0f,
    idleSurfaceColor: Color = Color.Unspecified,
    layerBlock: GraphicsLayerScope.() -> Unit = {}
) {
    val resolvedIdleSurfaceColor = if (idleSurfaceColor == Color.Unspecified) {
        resolveAndroidNativeIdleIndicatorSurfaceColor(darkTheme = isSystemInDarkTheme())
    } else {
        idleSurfaceColor
    }
    Box(
        modifier = modifier.run {
            if (backdrop != null && shouldDrawSegmentedControlIndicatorBackdrop(
                    liquidGlassEnabled = liquidGlassEnabled,
                    motionProgress = motionProgress,
                    hasExternalBackdrop = hasExternalBackdrop
                )
            ) {
                drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        lens(
                            refractionHeight = indicatorLensSpec.refractionHeightDp.dp.toPx(),
                            refractionAmount = indicatorLensSpec.refractionAmountDp.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = maxOf(indicatorHighlightAlpha, indicatorGlowAlpha))
                    },
                    shadow = {
                        Shadow(alpha = indicatorGlowAlpha)
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 8.dp * indicatorGlowAlpha,
                            alpha = indicatorGlowAlpha
                        )
                    },
                    layerBlock = layerBlock,
                    onDrawSurface = {
                        drawRect(
                            color = resolvedIdleSurfaceColor,
                            alpha = 1f - motionProgress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * motionProgress))
                    }
                )
            } else {
                background(resolvedIdleSurfaceColor, shape)
            }
        }
    )
}

internal fun resolveSegmentedControlMotionProgress(
    pressProgress: Float,
    refractionProgress: Float,
    tapPressRefractionEnabled: Boolean
): Float {
    val resolvedPressProgress = if (tapPressRefractionEnabled) pressProgress else 0f
    return maxOf(resolvedPressProgress, refractionProgress)
}

/**
 * Shared liquid segmented/top-tab indicator motion must match the home floating bottom bar.
 * Do not soften springs/offsets here — any divergence makes swipe stretch/settle feel wrong.
 */
internal fun resolveSegmentedControlMotionSpec(): BottomBarMotionSpec {
    return resolveBottomBarMotionSpec(profile = BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
}

private fun resolveSegmentedControlRefractionMotionProfile(
    motionReader: MotionReader,
    motionSpec: BottomBarMotionSpec,
    preset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
): BottomBarRefractionMotionProfile = resolveBottomBarEffectiveRefractionMotionProfile(
    preset = preset,
    profile = resolveBottomBarRefractionMotionProfile(
        position = motionReader.readPosition(),
        velocity = motionReader.readVelocityPxPerSecond(),
        isDragging = motionReader.readDragging(),
        motionSpec = motionSpec
    )
)

private fun resolveSegmentedControlMotionProgress(
    motionReader: MotionReader,
    motionSpec: BottomBarMotionSpec,
    preset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
): Float = resolveSegmentedControlMotionProgress(
    pressProgress = motionReader.readPressProgress(),
    refractionProgress = resolveSegmentedControlRefractionMotionProfile(
        motionReader = motionReader,
        motionSpec = motionSpec,
        preset = preset
    ).progress,
    tapPressRefractionEnabled = true
)

private fun resolveSegmentedControlEffectivePressProgress(
    motionReader: MotionReader,
    tapPressRefractionEnabled: Boolean
): Float {
    val pressProgress = motionReader.readPressProgress()
    return if (tapPressRefractionEnabled || motionReader.readDragging()) pressProgress else 0f
}

private fun resolveSegmentedControlLensProgress(
    motionReader: MotionReader,
    motionSpec: BottomBarMotionSpec,
    preset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset,
    tapPressRefractionEnabled: Boolean
): Float = resolveSharedLiquidIndicatorLensProgress(
    pressProgress = resolveSegmentedControlEffectivePressProgress(
        motionReader = motionReader,
        tapPressRefractionEnabled = tapPressRefractionEnabled
    ),
    motionProgress = resolveSegmentedControlMotionProgress(
        motionReader = motionReader,
        motionSpec = motionSpec,
        preset = preset
    ),
    isDragging = motionReader.readDragging()
)

/**
 * Same panel-offset formula as [KernelSuAlignedBottomBar]: fraction of full dock width,
 * capped at 4.dp, EaseOut mapped.
 */
internal fun resolveSharedLiquidIndicatorPanelOffsetPx(
    dragOffsetPx: Float,
    dockWidthPx: Float,
    maxOffsetPx: Float
): Float {
    if (dockWidthPx <= 0f) return 0f
    val fraction = (dragOffsetPx / dockWidthPx).coerceIn(-1f, 1f)
    return maxOffsetPx * fraction.sign * EaseOut.transform(abs(fraction))
}

/**
 * Lens/refraction progress for shared liquid indicators.
 * Bottom bar keeps a drag floor so slow swipes still show glass stretch instead of fading out.
 */
internal fun resolveSharedLiquidIndicatorLensProgress(
    pressProgress: Float,
    motionProgress: Float,
    isDragging: Boolean
): Float {
    val dragFloor = if (isDragging) 0.6f else 0f
    return maxOf(pressProgress, motionProgress, dragFloor).coerceIn(0f, 1f)
}

/**
 * When glass is active and the capsule is moving, visible labels stay neutral and the
 * selected color is carried by the export layer + tint (same as home bottom bar).
 */
internal fun resolveSharedLiquidIndicatorUseGlassColorPath(
    liquidGlassEnabled: Boolean,
    lensProgress: Float
): Boolean = liquidGlassEnabled && lensProgress > 0.001f

/** Capture lens strength: full 24dp while interacting, like KernelSu bottom bar capture. */
internal fun resolveSharedLiquidIndicatorCaptureLensProgress(
    lensProgress: Float,
    isDragging: Boolean
): Float {
    if (isDragging) return 1f
    return lensProgress.coerceIn(0f, 1f)
}

/**
 * Export-layer glyph color before [ColorFilter.tint].
 * Must stay near-white so SrcIn tint resolves to pure theme/primary color.
 */
internal fun resolveSharedLiquidExportMonochromeColor(
    darkTheme: Boolean
): Color = if (darkTheme) {
    Color.White.copy(alpha = 0.96f)
} else {
    Color.White
}

@Composable
fun BottomBarLiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemWidth: Dp? = null,
    height: Dp = BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_HEIGHT_DP.dp,
    indicatorHeight: Dp = BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_INDICATOR_HEIGHT_DP.dp,
    labelFontSize: TextUnit = 14.sp,
    containerHorizontalPadding: Dp = 3.dp,
    containerVerticalPadding: Dp = 3.dp,
    liquidGlassEffectsEnabled: Boolean = true,
    dragSelectionEnabled: Boolean = true,
    preferInlineContentStyle: Boolean = false,
    forceLiquidChrome: Boolean = false,
    backdrop: Backdrop? = null,
    miuixBackdrop: MiuixBackdrop? = null,
    tapPressRefractionEnabled: Boolean = true,
    containerColorOverride: Color? = null,
    selectedTextColorOverride: Color? = null,
    unselectedTextColorOverride: Color? = null,
    indicatorIdleSurfaceColorOverride: Color? = null,
    indicatorPositionProvider: (() -> Float)? = null,
    onIndicatorPositionChanged: ((Float) -> Unit)? = null
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val uiPreset = LocalUiPreset.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings(),
            context = kotlin.coroutines.EmptyCoroutineContext
        )
    val effectiveAndroidNativeLiquidGlassEnabled =
        forceLiquidChrome || homeSettings.androidNativeLiquidGlassEnabled
    val chromeStyle = resolveSegmentedControlChromeStyle(
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = effectiveAndroidNativeLiquidGlassEnabled,
        preferInlineContentStyle = preferInlineContentStyle
    )
    if (chromeStyle == SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE) {
        AndroidNativeUnderlinedSegmentedControl(
            items = items,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            modifier = modifier,
            enabled = enabled,
            itemWidth = itemWidth,
            height = height,
            labelFontSize = labelFontSize,
            selectedTextColorOverride = selectedTextColorOverride,
            unselectedTextColorOverride = unselectedTextColorOverride,
            indicatorPositionProvider = indicatorPositionProvider,
            onIndicatorPositionChanged = onIndicatorPositionChanged
        )
        return
    }

    val liquidGlassEnabled = resolveSegmentedControlLiquidGlassEnabled(
        storedLiquidGlassEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = effectiveAndroidNativeLiquidGlassEnabled
    )
    val blurIntensity = currentUnifiedBlurIntensity()
    val density = LocalDensity.current
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val motionSpec = remember { resolveSegmentedControlMotionSpec() }
    val clickPulseKey = remember { mutableIntStateOf(0) }
    val clickPulseTransform = rememberBottomBarClickPulseTransform(clickPulseKey.intValue)
    val dragState = rememberDampedDragAnimationState(
        initialIndex = safeSelectedIndex,
        itemCount = itemCount,
        motionSpec = motionSpec,
        notifyIndexChangedOnReleaseStart = indicatorPositionProvider != null,
        // Match home bottom bar: hold press glass until settle finishes.
        holdPressUntilReleaseTargetSettles = true,
        onIndexChanged = { index ->
            if (enabled && index in items.indices) {
                onSelected(index)
            }
        }
    )
    val latestExternalPositionProvider = rememberUpdatedState(indicatorPositionProvider)
    val motionReader = remember(dragState, itemCount, latestExternalPositionProvider) {
        SegmentedControlMotionReader(
            source = dragState,
            externalPositionProvider = latestExternalPositionProvider,
            itemCount = itemCount
        )
    }
    val latestIndicatorPositionChanged = rememberUpdatedState(onIndicatorPositionChanged)
    LaunchedEffect(motionReader, onIndicatorPositionChanged) {
        if (onIndicatorPositionChanged == null) return@LaunchedEffect
        snapshotFlow { motionReader.readPosition() }
            .collect { position -> latestIndicatorPositionChanged.value?.invoke(position) }
    }
    val indicatorShape = resolveSharedBottomBarCapsuleShape()
    val containerShape = indicatorShape
    val indicatorCorner = indicatorHeight / 2
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = AppSurfaceTokens.cardContainer()
    val androidNativeTuning = resolveAndroidNativeBottomBarTuning(
        blurEnabled = liquidGlassEnabled,
        darkTheme = isDarkTheme
    )
    val containerColor = containerColorOverride ?: resolveAndroidNativeFloatingBottomBarContainerColor(
        surfaceColor = surfaceColor,
        tuning = androidNativeTuning,
        glassEnabled = liquidGlassEnabled,
        blurEnabled = liquidGlassEnabled,
        blurIntensity = blurIntensity,
        liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
    )
    val themeColor = MaterialTheme.colorScheme.primary
    val selectedTextColor = selectedTextColorOverride ?: themeColor
    val unselectedTextColor = unselectedTextColorOverride
        ?: resolveLiquidSegmentedControlUnselectedTextColor(
            onSurface = MaterialTheme.colorScheme.onSurface,
            enabled = enabled
        )
    // Bottom-bar path: export is monochrome so SrcIn tint becomes pure theme color under glass.
    val exportTintColor = resolveAndroidNativeExportTintColor(
        themeColor = themeColor,
        darkTheme = isDarkTheme
    )
    val exportMonochromeColor = resolveSharedLiquidExportMonochromeColor(darkTheme = isDarkTheme)
    fun selectFromTap(index: Int) {
        if (!enabled || index !in items.indices) return
        clickPulseKey.intValue += 1
        // Animate indicator with the same spring path as home bottom bar taps.
        dragState.updateIndex(index)
        onSelected(index)
    }
    LaunchedEffect(safeSelectedIndex) {
        dragState.updateIndex(safeSelectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (itemWidth != null) {
                    Modifier.width((itemWidth.value * itemCount).dp + containerHorizontalPadding * 2)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(height)
    ) {
        val contentPadding = containerHorizontalPadding
        val contentVerticalInset = containerVerticalPadding
        val slotWidth = (maxWidth - (contentPadding * 2)) / itemCount
        val indicatorWidth = resolveSegmentedControlIndicatorWidthDp(
            slotWidthDp = slotWidth.value,
            indicatorHeightDp = indicatorHeight.value,
            itemCount = itemCount
        ).dp
        val resolvedIndicatorHeight = resolveSegmentedControlIndicatorHeightDp(
            slotWidthDp = slotWidth.value,
            indicatorHeightDp = indicatorHeight.value
        ).dp
        val itemWidthPx = with(density) { slotWidth.toPx() }.coerceAtLeast(1f)
        val dockWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val maxPanelOffsetPx = with(density) { 4.dp.toPx() }
        val preset = homeSettings.bottomBarLiquidGlassPreset
        val panelOffsetsProvider = remember(
            motionReader,
            dockWidthPx,
            maxPanelOffsetPx,
            preset
        ) {
            {
                resolveBottomBarPresetPanelOffsets(
                    preset = preset,
                    rawPanelOffsetPx = resolveSharedLiquidIndicatorPanelOffsetPx(
                        dragOffsetPx = motionReader.readDragOffsetPx(),
                        dockWidthPx = dockWidthPx,
                        maxOffsetPx = maxPanelOffsetPx
                    )
                )
            }
        }
        val motionProgressProvider = remember(motionReader, motionSpec, preset) {
            {
                resolveSegmentedControlMotionProgress(
                    motionReader = motionReader,
                    motionSpec = motionSpec,
                    preset = preset
                )
            }
        }
        val effectivePressProgressProvider = remember(
            motionReader,
            tapPressRefractionEnabled
        ) {
            {
                resolveSegmentedControlEffectivePressProgress(
                    motionReader = motionReader,
                    tapPressRefractionEnabled = tapPressRefractionEnabled
                )
            }
        }
        val lensProgressProvider = remember(
            motionReader,
            motionSpec,
            preset,
            tapPressRefractionEnabled
        ) {
            {
                resolveSegmentedControlLensProgress(
                    motionReader = motionReader,
                    motionSpec = motionSpec,
                    preset = preset,
                    tapPressRefractionEnabled = tapPressRefractionEnabled
                )
            }
        }
        // Match home bottom bar: drag anywhere on the dock, not only from the capsule.
        val dragModifier = if (enabled && itemCount > 1 && dragSelectionEnabled) {
            Modifier.horizontalDragGesture(
                dragState = dragState,
                itemWidthPx = itemWidthPx
            )
        } else {
            Modifier
        }
        val tabsBackdrop = rememberLayerBackdrop()
        val tabsMiuixBackdrop = rememberMiuixLayerBackdrop()
        // Never fall back export/shell sampling to tabsBackdrop: that LayerBackdrop is
        // recorded on the export node, and self-drawBackdrop overflows HyperOS
        // MiBackgroundBlurBlend (RenderThread stack overflow). Also never CombinedBackdrop
        // the page + tabs layers — same nested RenderNode failure mode as the dock bar.
        val hasExternalBackdrop = backdrop != null
        val hasMiuixExternalBackdrop = miuixBackdrop != null
        val containerBackdrop = backdrop
        val indicatorIdleSurfaceColor = indicatorIdleSurfaceColorOverride
            ?: resolveBottomBarIdleIndicatorSurfaceColor(
                preset = homeSettings.bottomBarLiquidGlassPreset,
                darkTheme = isDarkTheme
            )
        val foregroundAboveIndicator = shouldRenderBottomBarForegroundAboveIndicator(
            homeSettings.bottomBarLiquidGlassPreset
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .run {
                    if (miuixBackdrop != null) {
                        this.kernelSuMiuixFloatingDockSurface(
                            shape = containerShape,
                            backdrop = miuixBackdrop,
                            containerColor = containerColor,
                            blurEnabled = liquidGlassEnabled,
                            glassEnabled = liquidGlassEnabled,
                            blurRadius = androidNativeTuning.shellBlurRadiusDp.dp,
                            hazeState = null,
                            motionTier = MotionTier.Normal,
                            isTransitionRunning = false,
                            forceLowBlurBudget = false,
                            liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
                        )
                    } else {
                        this.kernelSuFloatingDockSurface(
                            shape = containerShape,
                            backdrop = backdrop,
                            containerColor = containerColor,
                            blurEnabled = liquidGlassEnabled,
                            glassEnabled = liquidGlassEnabled,
                            blurRadius = androidNativeTuning.shellBlurRadiusDp.dp,
                            hazeState = null,
                            motionTier = MotionTier.Normal,
                            isTransitionRunning = false,
                            forceLowBlurBudget = false,
                            liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
                        )
                    }
                }
        )

        // 1) Visible labels BEHIND the capsule (bottom-bar z-order).
        //    While sliding they stay neutral; theme color is revealed only through glass.
        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            motionReader = motionReader,
            motionSpec = motionSpec,
            liquidGlassPreset = preset,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = onSelected,
            interactive = false,
            applyItemScale = true,
            liquidGlassEnabled = liquidGlassEnabled,
            tapPressRefractionEnabled = tapPressRefractionEnabled,
            layer = SegmentedLabelLayer.VISIBLE,
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = contentPadding, vertical = contentVerticalInset)
                .zIndex(if (foregroundAboveIndicator) 1f else 0f)
                .graphicsLayer {
                    translationX = panelOffsetsProvider().indicatorPanelOffsetPx
                }
        )

        // 2) Hidden export capture: monochrome glyphs, theme tint on content only (not backdrop).
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .alpha(0f)
                .run {
                    if (hasMiuixExternalBackdrop) {
                        this.miuixLayerBackdrop(tabsMiuixBackdrop)
                            .graphicsLayer {
                                translationX = panelOffsetsProvider().exportPanelOffsetPx
                            }
                            .run {
                                if (
                                    shouldDrawSegmentedControlExportCaptureBackdrop(
                                        liquidGlassEnabled = liquidGlassEnabled,
                                        hasExternalBackdrop = true
                                    )
                                ) {
                                    miuixDrawBackdrop(
                                        backdrop = miuixBackdrop,
                                        shape = { containerShape },
                                        effects = {
                                            miuixVibrancy()
                                            miuixBlur(4.dp.toPx(), 4.dp.toPx())
                                            val captureLensProgress =
                                                resolveSharedLiquidIndicatorCaptureLensProgress(
                                                    lensProgress = lensProgressProvider(),
                                                    isDragging = motionReader.readDragging()
                                                )
                                            if (captureLensProgress > 0.001f) {
                                                val captureLensSpec =
                                                    resolveBottomBarBackdropPresetCaptureLens(
                                                        progress = captureLensProgress
                                                    )
                                                miuixLens(
                                                    refractionHeight = captureLensSpec.refractionHeightDp.dp.toPx(),
                                                    refractionAmount = captureLensSpec.refractionAmountDp.dp.toPx(),
                                                    depthEffect = true,
                                                    chromaticAberration = 0.5f
                                                )
                                            }
                                        },
                                        onDrawSurface = { drawRect(containerColor) }
                                    )
                                } else {
                                    this
                                }
                            }
                    } else {
                        this.layerBackdrop(tabsBackdrop)
                            .graphicsLayer {
                                translationX = panelOffsetsProvider().exportPanelOffsetPx
                            }
                            .run {
                                if (
                                    shouldDrawSegmentedControlExportCaptureBackdrop(
                                        liquidGlassEnabled = liquidGlassEnabled,
                                        hasExternalBackdrop = hasExternalBackdrop
                                    ) && containerBackdrop != null
                                ) {
                                    drawBackdrop(
                                        backdrop = containerBackdrop,
                                        shape = { containerShape },
                                        effects = {
                                            vibrancy()
                                            blur(androidNativeTuning.shellBlurRadiusDp.dp.toPx())
                                            val captureLensProgress =
                                                resolveSharedLiquidIndicatorCaptureLensProgress(
                                                    lensProgress = lensProgressProvider(),
                                                    isDragging = motionReader.readDragging()
                                                )
                                            if (captureLensProgress > 0.001f) {
                                                val captureLensSpec =
                                                    resolveBottomBarBackdropPresetCaptureLens(
                                                        progress = captureLensProgress
                                                    )
                                                lens(
                                                    refractionHeight = captureLensSpec.refractionHeightDp.dp.toPx(),
                                                    refractionAmount = captureLensSpec.refractionAmountDp.dp.toPx(),
                                                    depthEffect = true,
                                                    chromaticAberration = true
                                                )
                                            }
                                        },
                                        highlight = {
                                            val captureLensProgress =
                                                resolveSharedLiquidIndicatorCaptureLensProgress(
                                                    lensProgress = lensProgressProvider(),
                                                    isDragging = motionReader.readDragging()
                                                )
                                            Highlight.Default.copy(
                                                alpha = resolveBottomBarLiquidGlassHighlightAlpha(
                                                    captureLensProgress
                                                )
                                            )
                                        },
                                        onDrawSurface = { drawRect(containerColor) }
                                    )
                                } else {
                                    this
                                }
                            }
                    }
                }
        ) {
            BottomBarLiquidSegmentedLabels(
                items = items,
                selectedIndex = safeSelectedIndex,
                motionReader = motionReader,
                motionSpec = motionSpec,
                liquidGlassPreset = preset,
                // Match bottom bar export: neutral glyphs then SrcIn-tint to primary.
                selectedTextColor = exportMonochromeColor,
                unselectedTextColor = exportMonochromeColor,
                enabled = enabled,
                labelFontSize = labelFontSize,
                indicatorCorner = indicatorCorner,
                onSelected = onSelected,
                interactive = false,
                applyItemScale = true,
                liquidGlassEnabled = liquidGlassEnabled,
                tapPressRefractionEnabled = tapPressRefractionEnabled,
                layer = SegmentedLabelLayer.EXPORT,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPadding, vertical = contentVerticalInset)
                    .graphicsLayer(colorFilter = ColorFilter.tint(exportTintColor))
            )
        }

        // 3) Capsule on top — only this small motion scope observes values that the backdrop
        // API still requires as composable parameters. Translation and label color/scale remain
        // deferred to their layer/draw blocks.
        SegmentedControlIndicatorMotionLayer(
            motionReader = motionReader,
            motionSpec = motionSpec,
            liquidGlassPreset = preset,
            lensProgressProvider = lensProgressProvider,
            motionProgressProvider = motionProgressProvider,
            effectivePressProgressProvider = effectivePressProgressProvider,
            panelOffsetsProvider = panelOffsetsProvider,
            slotWidth = slotWidth,
            contentPadding = contentPadding,
            indicatorWidth = indicatorWidth,
            indicatorHeight = resolvedIndicatorHeight,
            indicatorShape = indicatorShape,
            clickPulseTransform = clickPulseTransform,
            tabsBackdrop = tabsBackdrop,
            tabsMiuixBackdrop = tabsMiuixBackdrop,
            backdrop = backdrop,
            miuixBackdrop = miuixBackdrop,
            indicatorIdleSurfaceColor = indicatorIdleSurfaceColor,
            liquidGlassEnabled = liquidGlassEnabled,
            isDarkTheme = isDarkTheme
        )

        // 4) Invisible hit / drag layer above everything.
        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            motionReader = motionReader,
            motionSpec = motionSpec,
            liquidGlassPreset = preset,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = ::selectFromTap,
            interactive = true,
            onPressChanged = dragState::setPressed,
            applyItemScale = false,
            liquidGlassEnabled = liquidGlassEnabled,
            tapPressRefractionEnabled = tapPressRefractionEnabled,
            layer = SegmentedLabelLayer.INPUT,
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = contentPadding, vertical = contentVerticalInset)
                .alpha(0f)
                .graphicsLayer {
                    translationX = panelOffsetsProvider().indicatorPanelOffsetPx
                }
                .then(dragModifier)
        )
    }
}

@Composable
private fun BoxScope.SegmentedControlIndicatorMotionLayer(
    motionReader: MotionReader,
    motionSpec: BottomBarMotionSpec,
    liquidGlassPreset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset,
    lensProgressProvider: () -> Float,
    motionProgressProvider: () -> Float,
    effectivePressProgressProvider: () -> Float,
    panelOffsetsProvider: () -> BottomBarPresetPanelOffsets,
    slotWidth: Dp,
    contentPadding: Dp,
    indicatorWidth: Dp,
    indicatorHeight: Dp,
    indicatorShape: Shape,
    clickPulseTransform: BottomBarClickPulseTransform,
    tabsBackdrop: Backdrop,
    tabsMiuixBackdrop: MiuixBackdrop,
    backdrop: Backdrop?,
    miuixBackdrop: MiuixBackdrop?,
    indicatorIdleSurfaceColor: Color,
    liquidGlassEnabled: Boolean,
    isDarkTheme: Boolean
) {
    val density = LocalDensity.current
    val lensProgress = lensProgressProvider()
    val motionProgress = motionProgressProvider()
    val effectivePressProgress = effectivePressProgressProvider()
    val indicatorDragScaleProgress = rememberBottomBarIndicatorDragScaleProgress(
        isDragging = motionReader.readDragging()
    )
    val indicatorLayerScaleProgress = maxOf(
        indicatorDragScaleProgress,
        effectivePressProgress
    )
    val indicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
        progress = lensProgress
    )
    val indicatorOffsetPx = with(density) {
        resolveSegmentedControlIndicatorOffsetDp(
            position = motionReader.readPosition(),
            slotWidthDp = slotWidth.value,
            contentPaddingDp = contentPadding.value
        ).dp.toPx()
    }
    val indicatorPanelOffsetPx = panelOffsetsProvider().indicatorPanelOffsetPx

    if (miuixBackdrop != null) {
        KernelSuMiuixBottomBarIndicatorLayer(
            visible = true,
            dockContentAlpha = 1f,
            indicatorTranslationXPx = indicatorOffsetPx,
            indicatorPanelOffsetPx = indicatorPanelOffsetPx,
            indicatorWidth = indicatorWidth,
            indicatorHeight = indicatorHeight,
            shellShape = indicatorShape,
            liquidGlassPreset = liquidGlassPreset,
            contentBackdrop = tabsMiuixBackdrop,
            backdrop = miuixBackdrop,
            indicatorLensSpec = indicatorLensSpec,
            effectivePressProgress = lensProgress,
            indicatorIdleSurfaceColor = indicatorIdleSurfaceColor,
            glassEnabled = liquidGlassEnabled,
            motionProgress = motionProgress,
            velocityItemsPerSecond = motionReader.readDeformationVelocityItemsPerSecond(),
            isDragging = motionReader.readDragging(),
            indicatorLayerScaleProgress = indicatorLayerScaleProgress,
            indicatorLayerScaleTransform = null,
            bottomBarMotionSpec = motionSpec,
            isDarkTheme = isDarkTheme
        )
    } else {
        KernelSuBottomBarIndicatorLayer(
            visible = true,
            dockContentAlpha = 1f,
            indicatorTranslationXPx = indicatorOffsetPx,
            indicatorPanelOffsetPx = indicatorPanelOffsetPx,
            indicatorSettleReboundTransform = clickPulseTransform,
            indicatorWidth = indicatorWidth,
            indicatorHeight = indicatorHeight,
            shellShape = indicatorShape,
            liquidGlassPreset = liquidGlassPreset,
            contentBackdrop = tabsBackdrop,
            backdrop = backdrop,
            indicatorLensSpec = indicatorLensSpec,
            effectivePressProgress = lensProgress,
            indicatorIdleSurfaceColor = indicatorIdleSurfaceColor,
            glassEnabled = liquidGlassEnabled,
            motionProgress = motionProgress,
            velocityItemsPerSecond = motionReader.readDeformationVelocityItemsPerSecond(),
            isDragging = motionReader.readDragging(),
            indicatorLayerScaleProgress = indicatorLayerScaleProgress,
            indicatorLayerScaleTransform = null,
            bottomBarMotionSpec = motionSpec,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
internal fun AndroidNativeUnderlinedSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemWidth: Dp? = null,
    height: Dp,
    labelFontSize: TextUnit,
    selectedTextColorOverride: Color? = null,
    unselectedTextColorOverride: Color? = null,
    indicatorPositionProvider: (() -> Float)? = null,
    onIndicatorPositionChanged: ((Float) -> Unit)? = null
) {
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val selectedTextColor = selectedTextColorOverride ?: MaterialTheme.colorScheme.primary
    val unselectedTextColor = unselectedTextColorOverride
        ?: MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.78f else 0.42f)
    val underlineShape = CircleShape
    val latestIndicatorPositionProvider = rememberUpdatedState(indicatorPositionProvider)
    val indicatorPosition = remember(safeSelectedIndex, itemCount, latestIndicatorPositionProvider) {
        {
            resolveSegmentedControlIndicatorPosition(
                internalPosition = safeSelectedIndex.toFloat(),
                externalPosition = latestIndicatorPositionProvider.value?.invoke(),
                itemCount = itemCount
            )
        }
    }
    val latestIndicatorPositionChanged = rememberUpdatedState(onIndicatorPositionChanged)
    LaunchedEffect(indicatorPosition, onIndicatorPositionChanged) {
        if (onIndicatorPositionChanged == null) return@LaunchedEffect
        snapshotFlow { indicatorPosition() }
            .collect { position -> latestIndicatorPositionChanged.value?.invoke(position) }
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (itemWidth != null) {
                    Modifier.width(itemWidth * itemCount)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(height)
    ) {
        val segmentWidth = maxWidth / itemCount
        val underlineWidth = (segmentWidth * 0.42f)
            .coerceAtLeast(28.dp)
            .coerceAtMost(56.dp)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val selected = index == safeSelectedIndex
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clickable(enabled = enabled) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) selectedTextColor else unselectedTextColor,
                        fontSize = labelFontSize,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset {
                    val underlineOffsetX =
                        (segmentWidth * indicatorPosition()) +
                            ((segmentWidth - underlineWidth) / 2)
                    IntOffset(underlineOffsetX.roundToPx(), 0)
                }
                .width(underlineWidth)
                .height(3.dp)
                .clip(underlineShape)
                .background(selectedTextColor)
        )
    }
}

@Composable
private fun BottomBarLiquidSegmentedLabels(
    items: List<String>,
    selectedIndex: Int,
    motionReader: MotionReader,
    motionSpec: BottomBarMotionSpec,
    liquidGlassPreset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    enabled: Boolean,
    labelFontSize: TextUnit,
    indicatorCorner: Dp,
    onSelected: (Int) -> Unit,
    interactive: Boolean,
    onPressChanged: ((Boolean) -> Unit)? = null,
    applyItemScale: Boolean = true,
    liquidGlassEnabled: Boolean,
    tapPressRefractionEnabled: Boolean = true,
    layer: SegmentedLabelLayer,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val interactionSource = remember { MutableInteractionSource() }
            val latestOnPressChanged = rememberUpdatedState(onPressChanged)
            if (interactive && onPressChanged != null) {
                LaunchedEffect(interactionSource) {
                    var pressed = false
                    try {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    pressed = true
                                    latestOnPressChanged.value?.invoke(true)
                                }
                                is PressInteraction.Release,
                                is PressInteraction.Cancel -> {
                                    pressed = false
                                    latestOnPressChanged.value?.invoke(false)
                                }
                            }
                        }
                    } finally {
                        if (pressed) latestOnPressChanged.value?.invoke(false)
                    }
                }
            }
            val textColor = remember(
                index,
                selectedIndex,
                motionReader,
                motionSpec,
                liquidGlassPreset,
                selectedTextColor,
                unselectedTextColor,
                enabled,
                liquidGlassEnabled,
                tapPressRefractionEnabled,
                layer
            ) {
                ColorProducer {
                    if (!enabled) {
                        unselectedTextColor.copy(alpha = 0.44f)
                    } else {
                        val motionProgress = resolveSegmentedControlMotionProgress(
                            motionReader = motionReader,
                            motionSpec = motionSpec,
                            preset = liquidGlassPreset
                        )
                        val refractionProfile = resolveSegmentedControlRefractionMotionProfile(
                            motionReader = motionReader,
                            motionSpec = motionSpec,
                            preset = liquidGlassPreset
                        )
                        val selectionEmphasis = when (layer) {
                            SegmentedLabelLayer.EXPORT -> refractionProfile.exportSelectionEmphasis
                            SegmentedLabelLayer.VISIBLE,
                            SegmentedLabelLayer.INPUT -> refractionProfile.visibleSelectionEmphasis
                        }
                        val visual = resolveBottomBarItemMotionVisual(
                            itemIndex = index,
                            indicatorPosition = motionReader.readPosition(),
                            currentSelectedIndex = selectedIndex,
                            motionProgress = motionProgress,
                            selectionEmphasis = selectionEmphasis
                        )
                        val forceUnselectedColor = layer == SegmentedLabelLayer.VISIBLE &&
                            resolveSharedLiquidIndicatorUseGlassColorPath(
                                liquidGlassEnabled = liquidGlassEnabled,
                                lensProgress = resolveSegmentedControlLensProgress(
                                    motionReader = motionReader,
                                    motionSpec = motionSpec,
                                    preset = liquidGlassPreset,
                                    tapPressRefractionEnabled = tapPressRefractionEnabled
                                )
                            )
                        resolveLiquidGlassSelectionContentColors(
                            unselectedColor = unselectedTextColor,
                            selectedColor = selectedTextColor,
                            themeWeight = visual.themeWeight,
                            glassEnabled = forceUnselectedColor,
                            indicatorProgress = motionProgress,
                            indicatorBackdropEnabled = true
                        ).visibleColor
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(indicatorCorner))
                    .then(
                        if (interactive) {
                            Modifier.clickable(
                                enabled = enabled,
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelected(index)
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = labelFontSize,
                    fontWeight = if (index == selectedIndex) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        val motionProgress = resolveSegmentedControlMotionProgress(
                            motionReader = motionReader,
                            motionSpec = motionSpec,
                            preset = liquidGlassPreset
                        )
                        val refractionProfile = resolveSegmentedControlRefractionMotionProfile(
                            motionReader = motionReader,
                            motionSpec = motionSpec,
                            preset = liquidGlassPreset
                        )
                        val selectionEmphasis = when (layer) {
                            SegmentedLabelLayer.EXPORT -> refractionProfile.exportSelectionEmphasis
                            SegmentedLabelLayer.VISIBLE,
                            SegmentedLabelLayer.INPUT -> refractionProfile.visibleSelectionEmphasis
                        }
                        val labelScale = if (applyItemScale) {
                            resolveBottomBarItemMotionVisual(
                                itemIndex = index,
                                indicatorPosition = motionReader.readPosition(),
                                currentSelectedIndex = selectedIndex,
                                motionProgress = motionProgress,
                                selectionEmphasis = selectionEmphasis
                            ).scale
                        } else {
                            1f
                        }
                        scaleX = labelScale
                        scaleY = labelScale
                    }
                )
            }
        }
    }
}
