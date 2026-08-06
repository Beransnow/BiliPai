package com.android.purebilibili.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalDynamicColorActive
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveUiStyle

enum class AppSemanticIconFamily {
    MATERIAL,
}

enum class AppSemanticAccentRole {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

data class AppSemanticAccentPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
)

data class AppSemanticVisualPolicy(
    val iconFamily: AppSemanticIconFamily,
    val accentPalette: AppSemanticAccentPalette?,
    val prefersNativeChrome: Boolean,
    val supportsIndependentLiquidGlass: Boolean,
    val prefersGroupedListCards: Boolean = false,
) {
    fun resolveAccent(role: AppSemanticAccentRole, fallback: Color): Color {
        val palette = accentPalette ?: return fallback
        return when (role) {
            AppSemanticAccentRole.PRIMARY -> palette.primary
            AppSemanticAccentRole.SECONDARY -> palette.secondary
            AppSemanticAccentRole.TERTIARY -> palette.tertiary
            AppSemanticAccentRole.ERROR -> palette.error
        }
    }

    companion object {
        fun material(palette: AppSemanticAccentPalette) = AppSemanticVisualPolicy(
            iconFamily = AppSemanticIconFamily.MATERIAL,
            accentPalette = palette,
            prefersNativeChrome = true,
            supportsIndependentLiquidGlass = false,
        )
    }
}

fun resolveAppSemanticAccentPalette(
    colorScheme: ColorScheme,
    useSemanticAccentRoles: Boolean,
): AppSemanticAccentPalette = AppSemanticAccentPalette(
    primary = colorScheme.primary,
    secondary = if (useSemanticAccentRoles) colorScheme.secondary else colorScheme.primary,
    tertiary = if (useSemanticAccentRoles) colorScheme.tertiary else colorScheme.primary,
    error = colorScheme.error,
)

fun resolveAppSemanticVisualPolicy(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    materialPalette: AppSemanticAccentPalette,
): AppSemanticVisualPolicy = when (resolveUiStyle(uiPreset, androidNativeVariant)) {
    // 两值模型：历史 iOS 值在运行时解析为 MIUIX，不再产生 Cupertino 视觉策略。
    AppUiStyle.MATERIAL3 -> AppSemanticVisualPolicy.material(materialPalette)
    AppUiStyle.MIUIX -> AppSemanticVisualPolicy.material(materialPalette).copy(
        prefersGroupedListCards = true,
    )
}

@Composable
fun rememberAppSemanticVisualPolicy(): AppSemanticVisualPolicy {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val dynamicColorActive = LocalDynamicColorActive.current
    val colorScheme = MaterialTheme.colorScheme
    return remember(uiPreset, androidNativeVariant, dynamicColorActive, colorScheme) {
        resolveAppSemanticVisualPolicy(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant,
            materialPalette = resolveAppSemanticAccentPalette(
                colorScheme = colorScheme,
                useSemanticAccentRoles = dynamicColorActive,
            ),
        )
    }
}

fun resolveAppChromeLiquidGlassEnabled(
    supportsIndependentLiquidGlass: Boolean,
    individualEnabled: Boolean,
    androidNativeEnabled: Boolean,
): Boolean = androidNativeEnabled || (supportsIndependentLiquidGlass && individualEnabled)

@Composable
fun rememberAppChromeLiquidGlassEnabled(
    individualEnabled: Boolean,
    androidNativeEnabled: Boolean,
): Boolean {
    val policy = rememberAppSemanticVisualPolicy()
    return remember(policy.supportsIndependentLiquidGlass, individualEnabled, androidNativeEnabled) {
        resolveAppChromeLiquidGlassEnabled(
            supportsIndependentLiquidGlass = policy.supportsIndependentLiquidGlass,
            individualEnabled = individualEnabled,
            androidNativeEnabled = androidNativeEnabled,
        )
    }
}
