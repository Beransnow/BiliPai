package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiStyle

data class AppearanceUiPresetDescription(
    val title: String,
    val summary: String
)

internal fun resolveAppearanceUiPresetDescription(
    uiStyle: UiStyle,
    iosTitle: String,
    iosSummary: String,
    materialTitle: String,
    materialSummary: String,
    miuixTitle: String,
    miuixSummary: String
): AppearanceUiPresetDescription {
    return when (uiStyle) {
        UiStyle.IOS -> AppearanceUiPresetDescription(
            title = iosTitle,
            summary = iosSummary
        )

        UiStyle.MATERIAL3 -> AppearanceUiPresetDescription(
            title = materialTitle,
            summary = materialSummary
        )

        UiStyle.MIUIX -> AppearanceUiPresetDescription(
            title = miuixTitle,
            summary = miuixSummary
        )
    }
}
