package com.android.purebilibili.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.blur.LocalFloatingChromeBackdrop
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell

/** App-level back-to-top button with real backdrop glass and a safe opaque fallback. */
@Composable
fun AppLiquidGlassBackToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "回到顶部",
) {
    val backdrop = LocalFloatingChromeBackdrop.current
    val fallbackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val glassActive = LocalAppThemeConfig.current.liquidGlassEnabled &&
        backdrop != null &&
        !isLowBlurBudgetForced()

    AppBackToTopButton(
        visible = visible,
        onClick = onClick,
        modifier = modifier,
        buttonModifier = Modifier.biliPaiFloatingDockShell(
            backdrop = backdrop,
            containerColor = fallbackColor,
            pressProgress = 0f,
            enabled = glassActive,
        ),
        containerColor = if (glassActive) Color.Transparent else fallbackColor,
        contentDescription = contentDescription,
    )
}
