package com.android.purebilibili.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalDynamicColorActive
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.UiStyle
import com.android.purebilibili.core.theme.resolveUiStyle

enum class AppSemanticIconFamily {
    CUPERTINO,
    MATERIAL,
}

/**
 * 全局图标呈现样式(用户可切换的两套方案)。
 * - [AUTO]:跟随 UI 预设(IOS→[THEME_CONTAINER],MD3/MIUIX→[MD3_STANDARD])。
 * - [THEME_CONTAINER]:主题色容器 —— 图标置于主题色(secondaryContainer)
 *   圆角容器内,图标用 onSecondaryContainer,对齐官方 Settings 容器图标规范。
 * - [MD3_STANDARD]:MD3 官方推荐 —— onSurfaceVariant 单色图标、无容器。
 */
enum class AppIconStyle {
    AUTO,
    THEME_CONTAINER,
    MD3_STANDARD,
}

/**
 * AUTO 表示"保持预设现状":iOS/MIUIX 预设不引入容器化与单色化(设置图标保持
 * 多彩色等既有外观),仅 MD3 预设解析为官方推荐样式。
 */
fun resolveAppIconStyle(
    iconStyle: AppIconStyle,
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): AppIconStyle = when (iconStyle) {
    AppIconStyle.AUTO -> when (resolveUiStyle(uiPreset, androidNativeVariant)) {
        UiStyle.IOS, UiStyle.MIUIX -> AppIconStyle.AUTO
        UiStyle.MATERIAL3 -> AppIconStyle.MD3_STANDARD
    }
    else -> iconStyle
}

/** 从持久化字符串解析 [AppIconStyle],非法或缺失值回退 [AppIconStyle.AUTO]。 */
fun resolveAppIconStylePreference(rawValue: String?): AppIconStyle {
    return runCatching {
        rawValue?.let(AppIconStyle::valueOf)
    }.getOrNull() ?: AppIconStyle.AUTO
}

/**
 * 全局图标呈现样式 CompositionLocal。
 * 默认 [AppIconStyle.AUTO] 由 UI 预设推导;主题层提供用户显式选择后全局生效。
 */
val LocalAppIconStyle = staticCompositionLocalOf {
    AppIconStyle.AUTO
}

/** 解析当前生效的图标呈现样式(处理 AUTO 跟随预设)。 */
@Composable
fun rememberResolvedAppIconStyle(): AppIconStyle {
    val iconStyle = LocalAppIconStyle.current
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    return remember(iconStyle, uiPreset, androidNativeVariant) {
        resolveAppIconStyle(iconStyle, uiPreset, androidNativeVariant)
    }
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
    val iconStyle: AppIconStyle = AppIconStyle.AUTO,
    val accentPalette: AppSemanticAccentPalette?,
    val prefersNativeChrome: Boolean,
    val supportsIndependentLiquidGlass: Boolean,
    val prefersGroupedListCards: Boolean = false,
) {
    /**
     * MD3 官方推荐样式强制使用 Material 官方字形;
     * 主题色容器样式沿用当前 UI 预设的字形(Cupertino 或 Material)。
     */
    val effectiveIconFamily: AppSemanticIconFamily
        get() = if (iconStyle == AppIconStyle.MD3_STANDARD) {
            AppSemanticIconFamily.MATERIAL
        } else {
            iconFamily
        }

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
        val Cupertino = AppSemanticVisualPolicy(
            iconFamily = AppSemanticIconFamily.CUPERTINO,
            accentPalette = null,
            prefersNativeChrome = false,
            supportsIndependentLiquidGlass = true,
        )

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
    iconStyle: AppIconStyle = AppIconStyle.AUTO,
): AppSemanticVisualPolicy = when (resolveUiStyle(uiPreset, androidNativeVariant)) {
    UiStyle.IOS -> AppSemanticVisualPolicy.Cupertino.copy(iconStyle = iconStyle)
    UiStyle.MATERIAL3 -> AppSemanticVisualPolicy.material(materialPalette).copy(iconStyle = iconStyle)
    UiStyle.MIUIX -> AppSemanticVisualPolicy.material(materialPalette).copy(
        prefersGroupedListCards = true,
        iconStyle = iconStyle,
    )
}

@Composable
fun rememberAppSemanticVisualPolicy(): AppSemanticVisualPolicy {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val dynamicColorActive = LocalDynamicColorActive.current
    val colorScheme = MaterialTheme.colorScheme
    val iconStyle = rememberResolvedAppIconStyle()
    return remember(uiPreset, androidNativeVariant, dynamicColorActive, colorScheme, iconStyle) {
        resolveAppSemanticVisualPolicy(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant,
            materialPalette = resolveAppSemanticAccentPalette(
                colorScheme = colorScheme,
                useSemanticAccentRoles = dynamicColorActive,
            ),
            iconStyle = iconStyle,
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
