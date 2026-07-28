package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiStyle

internal fun resolveUiStyleSegmentOptions(
    iosLabel: String,
    material3Label: String,
    miuixLabel: String,
): List<PlaybackSegmentOption<UiStyle>> {
    return listOf(
        PlaybackSegmentOption(UiStyle.IOS, iosLabel),
        PlaybackSegmentOption(UiStyle.MATERIAL3, material3Label),
        PlaybackSegmentOption(UiStyle.MIUIX, miuixLabel),
    )
}
