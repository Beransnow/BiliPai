package com.android.purebilibili.feature.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.feature.home.LocalHomeScrollOffset
import kotlinx.coroutines.flow.collect
import top.yukonga.miuix.kmp.blur.Backdrop

internal enum class LinkedDockPhase { Expanded, Playback, Search }

@Composable
internal fun LinkedBottomDock(
    currentItem: BottomNavItem,
    firstItem: BottomNavItem,
    firstLabel: String,
    searchEnabled: Boolean,
    isFeedScrollInProgress: Boolean,
    onSearchClick: () -> Unit,
    onSearchKeywordSubmit: (String) -> Unit,
    containerColor: Color,
    backdrop: Backdrop?,
    glassEnabled: Boolean,
    liquidGlassTuning: LiquidGlassTuning,
    iconStyle: SharedFloatingBottomBarIconStyle,
    nowPlayingContent: (@Composable (Modifier, Float, Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    navigationContent: @Composable () -> Unit,
) {
    var phase by remember(currentItem, searchEnabled, nowPlayingContent != null) {
        mutableStateOf(LinkedDockPhase.Expanded)
    }
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scroll = LocalHomeScrollOffset.current
    val scrolling by rememberUpdatedState(isFeedScrollInProgress)
    val hasAudio = nowPlayingContent != null
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(currentItem, hasAudio, scroll, threshold) {
        var previous = scroll.floatValue
        var accumulated = 0f
        snapshotFlow { scroll.floatValue to scrolling }.collect { (offset, active) ->
            val delta = offset - previous
            previous = offset
            if (!active || phase == LinkedDockPhase.Search || currentItem != BottomNavItem.HOME) {
                accumulated = 0f
            } else {
                accumulated = accumulateDockScroll(accumulated, delta)
                if (offset <= 0f || accumulated <= -threshold) {
                    phase = LinkedDockPhase.Expanded
                    accumulated = 0f
                } else if (hasAudio && accumulated >= threshold) {
                    phase = LinkedDockPhase.Playback
                    accumulated = 0f
                }
            }
        }
    }
    fun expand() {
        focusManager.clearFocus()
        phase = LinkedDockPhase.Expanded
    }
    BackHandler(phase != LinkedDockPhase.Expanded) { expand() }
    val reduceMotion = rememberSystemReduceMotion()
    val transition = updateTransition(phase, label = "linkedBottomDock")
    val merge = transition.animateFloat(
        transitionSpec = { if (reduceMotion) snap() else spring(dampingRatio = 0.86f, stiffness = 480f) },
        label = "dockMerge",
    ) { if (it == LinkedDockPhase.Expanded) 0f else 1f }
    val search = transition.animateFloat(
        transitionSpec = { if (reduceMotion) snap() else spring(dampingRatio = 0.86f, stiffness = 480f) },
        label = "dockSearch",
    ) { if (it == LinkedDockPhase.Search) 1f else 0f }
    val shape = resolveSharedBottomBarCapsuleShape()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    // A single audio child is measured and moved between rows. Playback and artwork stay mounted.
    Layout(
        modifier = modifier.fillMaxWidth().imePadding().navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = {
            Box(Modifier.graphicsLayer {
                alpha = merge.value.coerceIn(0f, 1f)
                val settle = (merge.value - 1f).coerceAtLeast(0f)
                scaleX = 1f + settle * 0.2f
                scaleY = 1f + settle * 0.1f
            }
                .biliPaiFloatingDockShell(backdrop, containerColor, 0f, shape = shape,
                    enabled = glassEnabled, liquidGlassTuning = liquidGlassTuning))
            Box(Modifier.graphicsLayer { alpha = (1f - merge.value * 3f).coerceIn(0f, 1f) }
                .pointerInput(phase) {
                    if (phase != LinkedDockPhase.Expanded) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .then(if (phase != LinkedDockPhase.Expanded) Modifier.clearAndSetSemantics {} else Modifier)) {
                if (merge.value < 0.999f) navigationContent()
            }
            Box(Modifier.graphicsLayer { alpha = (merge.value * 2f).coerceIn(0f, 1f) }
                .then(if (phase != LinkedDockPhase.Expanded) Modifier.clickable(role = Role.Button) { expand() }
                    else Modifier.clearAndSetSemantics {}), contentAlignment = Alignment.Center) {
                if (merge.value > 0.001f) {
                    AppIcon(
                        imageVector = if (iconStyle == SharedFloatingBottomBarIconStyle.MIUIX) {
                            resolveHomeNavigationBarIcon(firstItem, currentItem == firstItem)
                        } else resolveMaterialBottomBarIcon(firstItem, currentItem == firstItem),
                        contentDescription = "$firstLabel，展开底栏",
                        tint = accentColor,
                    )
                }
            }
            Box {
                nowPlayingContent?.invoke(Modifier.fillMaxSize(), merge.value.coerceIn(0f, 1f),
                    search.value > 0.5f)
            }
            Box(contentAlignment = Alignment.Center) {
                if (searchEnabled) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - merge.value }
                        .biliPaiFloatingDockShell(backdrop, containerColor, 0f, shape = shape,
                            enabled = glassEnabled, liquidGlassTuning = liquidGlassTuning))
                    Box(Modifier.fillMaxSize().then(
                        if (phase != LinkedDockPhase.Search) Modifier.clickable(role = Role.Button) {
                            phase = LinkedDockPhase.Search
                        } else Modifier
                    )) {
                        BiliPaiBottomBarSearchVisualContent(
                            expanded = phase == LinkedDockPhase.Search,
                            query = query,
                            onQueryChange = { query = it },
                            onSubmit = {
                                focusManager.clearFocus()
                                if (query.isBlank()) onSearchClick() else onSearchKeywordSubmit(query.trim())
                            },
                            contentColor = contentColor,
                            accentColor = accentColor,
                            iconScale = 1f,
                            fieldAlpha = search.value,
                            interactive = true,
                            iconStyle = iconStyle,
                        )
                    }
                }
            }
        },
    ) { children, constraints ->
        val width = constraints.maxWidth.coerceAtMost(600.dp.roundToPx())
        val button = 56.dp.roundToPx()
        val barHeight = 64.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val progress = merge.value.coerceIn(0f, 1f)
        val geometry = resolveLinkedDockGeometry(
            width, button, barHeight, gap, hasAudio, searchEnabled, progress, search.value,
        )
        val top = geometry.top
        val searchWidth = geometry.searchWidth
        val audioWidth = geometry.audioWidth
        val navWidth = (width - (if (searchEnabled) button + gap else 0)).coerceAtLeast(0)
        val shell = children[0].measure(Constraints.fixed(width, barHeight))
        val nav = children[1].measure(Constraints.fixed(navWidth, barHeight))
        val first = children[2].measure(Constraints.fixed(button, barHeight))
        val audio = children[3].measure(Constraints.fixed(if (hasAudio) audioWidth else 0, if (hasAudio) barHeight else 0))
        val searchBox = children[4].measure(Constraints.fixed(searchWidth, barHeight))
        layout(constraints.maxWidth, geometry.height) {
            val left = (constraints.maxWidth - width) / 2
            shell.placeRelative(left, top)
            if (progress < 0.999f) nav.placeRelative(left, top)
            if (progress > 0.001f) first.placeRelative(left, top)
            if (hasAudio) audio.placeRelative(left + geometry.audioX, geometry.audioY)
            searchBox.placeRelative(left + width - searchWidth, top)
        }
    }
}
