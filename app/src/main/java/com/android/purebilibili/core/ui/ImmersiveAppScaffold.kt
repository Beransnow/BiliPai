package com.android.purebilibili.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.feature.home.components.BiliPaiImmersiveTopBar
import com.android.purebilibili.feature.home.components.shouldUseBiliPaiProgressiveTopBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** List pages keep their viewport full height and apply scaffold insets as scroll content padding. */
@Composable
internal fun ImmersiveAppScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable (PaddingValues) -> Unit,
) {
    val config = LocalAppThemeConfig.current
    val progressive = shouldUseBiliPaiProgressiveTopBlur(
        enabled = config.progressiveTopBlurEnabled && !config.headerBlurEnabled && topBar != null,
        hasBackdrop = true,
    ) && !isLowBlurBudgetForced()
    val backdrop = if (progressive) rememberLayerBackdrop() else null
    AppScaffold(
        modifier = modifier,
        topBar = {
            if (topBar != null) {
                BiliPaiImmersiveTopBar(
                    backdrop = backdrop,
                    enabled = progressive,
                    modifier = Modifier.background(
                        if (progressive) Color.Transparent else globalWallpaperAwareChromeColor(containerColor)
                    ),
                    content = topBar,
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
            ),
        ) {
            content(padding)
        }
    }
}
