package com.android.purebilibili.core.theme

import androidx.compose.runtime.compositionLocalOf

enum class AppIconStyle(val value: Int, val label: String, val description: String) {
    MATERIAL_SYMBOLS(
        value = 0,
        label = "Material Symbols",
        description = "安卓默认风格，优先稳定和熟悉感"
    ),
    LUCIDE(
        value = 1,
        label = "Lucide",
        description = "线条简洁，默认推荐"
    ),
    PHOSPHOR(
        value = 2,
        label = "Phosphor",
        description = "线条更圆润，画面更轻"
    ),
    TABLER(
        value = 3,
        label = "Tabler",
        description = "线条更硬朗，识别更直接"
    );

    companion object {
        fun fromValue(value: Int): AppIconStyle =
            entries.find { it.value == value } ?: LUCIDE
    }
}

val LocalAppIconStyle = compositionLocalOf { AppIconStyle.LUCIDE }
