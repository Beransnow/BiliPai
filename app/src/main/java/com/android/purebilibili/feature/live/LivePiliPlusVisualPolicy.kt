package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.resolveCompactCapsuleChromeSpec

internal data class LivePiliPlusHomeMetrics(
    val safeSpaceDp: Int,
    val cardSpaceDp: Int,
    val cardRadiusDp: Int,
    val coverAspectRatio: Float,
    val followAvatarSizeDp: Int,
    val followItemExtentDp: Int
)

internal data class LiveVisualSpec(
    val homeMetrics: LivePiliPlusHomeMetrics,
    val maxContentWidthDp: Int,
    val playerButtonTouchTargetDp: Int,
    val playerButtonVisualSizeDp: Int,
    val playerQualityDialogWidthDp: Int,
    val emptyStateContainerSizeDp: Int,
    val emptyStateIconSizeDp: Int,
)

internal data class LiveChatInputVisualSpec(
    val controlSizeDp: Int,
    val inputFieldHeightDp: Int,
    val iconSizeDp: Int,
    val sendButtonSizeDp: Int,
    val sendIconOffsetXDp: Int,
    val sendIconOffsetYDp: Int,
    val overlayMessageSpaceDp: Int,
    val likeBadgeOffsetXDp: Int,
    val likeBadgeOffsetYDp: Int,
)

internal data class LiveSheetVisualSpec(
    val emoticonListMaxHeightDp: Int,
    val emoticonImageHeightDp: Int,
    val contributionListMaxHeightDp: Int,
)

internal data class LivePlayerControlVisualSpec(
    val rowHeightDp: Int,
    val iconSizeDp: Int,
)

internal data class LivePiliPlusChipColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color
)

internal data class LivePiliPlusChatBubbleTokens(
    val cornerRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val fontSizeSp: Int,
    val backgroundAlpha: Float,
    val nameAlpha: Float
)

internal data class LivePiliPlusRoomColorTokens(
    val baseBackgroundColor: Color,
    val backdropImageAlpha: Float,
    val inputContainerAlpha: Float,
    val inputOverlayColor: Color,
    val inputContentColor: Color
)

internal data class LiveInteractionSegmentedControlSpec(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val heightDp: Int,
    val indicatorHeightDp: Int,
    val labelFontSizeSp: Int
)

internal data class LandscapeLiveChatVisualSpec(
    val headerFontSizeSp: Int,
    val subtitleFontSizeSp: Int,
    val medalFontSizeSp: Int,
    val messageFontSizeSp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val medalHorizontalPaddingDp: Int,
    val medalVerticalPaddingDp: Float,
)

internal data class LiveMedalBadgeVisualSpec(
    val verticalPaddingDp: Float,
    val dividerWidthDp: Float,
)

internal fun resolveLiveVisualSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): LiveVisualSpec {
    val isMiuix = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX
    val safeSpaceDp = when {
        uiPreset == UiPreset.IOS -> 12
        isMiuix -> 16
        else -> 18
    }
    val cardSpaceDp = when {
        uiPreset == UiPreset.IOS -> 8
        isMiuix -> 10
        else -> 12
    }
    val playerButtonVisualSizeDp = when {
        uiPreset == UiPreset.IOS -> 38
        isMiuix -> 38
        else -> 40
    }
    return LiveVisualSpec(
        homeMetrics = LivePiliPlusHomeMetrics(
            safeSpaceDp = safeSpaceDp,
            cardSpaceDp = cardSpaceDp,
            cardRadiusDp = 10,
            coverAspectRatio = 16f / 10f,
            followAvatarSizeDp = 45,
            followItemExtentDp = 70,
        ),
        maxContentWidthDp = 1200,
        playerButtonTouchTargetDp = 48,
        playerButtonVisualSizeDp = playerButtonVisualSizeDp,
        playerQualityDialogWidthDp = 280,
        emptyStateContainerSizeDp = 64,
        emptyStateIconSizeDp = 28,
    )
}

internal fun resolveLiveChatInputVisualSpec(): LiveChatInputVisualSpec = LiveChatInputVisualSpec(
    controlSizeDp = 48,
    inputFieldHeightDp = 40,
    iconSizeDp = 20,
    sendButtonSizeDp = 48,
    sendIconOffsetXDp = -2,
    sendIconOffsetYDp = 2,
    overlayMessageSpaceDp = 10,
    likeBadgeOffsetXDp = 8,
    likeBadgeOffsetYDp = -6,
)

internal fun resolveLiveSheetVisualSpec(): LiveSheetVisualSpec = LiveSheetVisualSpec(
    emoticonListMaxHeightDp = 420,
    emoticonImageHeightDp = 34,
    contributionListMaxHeightDp = 360,
)

internal fun resolveLivePlayerControlVisualSpec(): LivePlayerControlVisualSpec =
    LivePlayerControlVisualSpec(
        rowHeightDp = 48,
        iconSizeDp = 14,
    )

internal fun resolveLivePiliPlusHomeMetrics(): LivePiliPlusHomeMetrics {
    return resolveLiveVisualSpec(
        uiPreset = UiPreset.IOS,
        androidNativeVariant = AndroidNativeVariant.MATERIAL3,
    ).homeMetrics
}

internal fun resolveLivePiliPlusHomeMetrics(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): LivePiliPlusHomeMetrics {
    return resolveLiveVisualSpec(uiPreset, androidNativeVariant).homeMetrics
}

internal fun resolveLivePiliPlusGridColumns(
    widthDp: Int,
    isTabletLayout: Boolean,
): Int {
    if (!isTabletLayout) return 2
    return (widthDp / 240).coerceIn(3, 5)
}

internal fun resolveLivePiliPlusChipColors(
    selectedContainer: Color,
    selectedContent: Color,
    unselectedContent: Color
): LivePiliPlusChipColors {
    return LivePiliPlusChipColors(
        selectedContainerColor = selectedContainer,
        selectedContentColor = selectedContent,
        unselectedContainerColor = Color.Transparent,
        unselectedContentColor = unselectedContent
    )
}

internal fun resolveLivePiliPlusChatBubbleTokens(
    isOverlay: Boolean,
    isDark: Boolean
): LivePiliPlusChatBubbleTokens {
    val backgroundAlpha = when {
        isOverlay -> 0f
        else -> 0.08f
    }
    val nameAlpha = if (isOverlay) 0.90f else 0.60f
    return LivePiliPlusChatBubbleTokens(
        cornerRadiusDp = 14,
        horizontalPaddingDp = 10,
        verticalPaddingDp = 4,
        fontSizeSp = 14,
        backgroundAlpha = backgroundAlpha,
        nameAlpha = nameAlpha
    )
}

internal fun shouldRenderLiveDanmaku(
    text: String,
    emoticonUrl: String?
): Boolean {
    return text.isNotBlank() || !emoticonUrl.isNullOrBlank()
}

internal fun shouldRenderLiveDanmakuImageEmoticon(emoticonUrl: String?): Boolean {
    return !emoticonUrl.isNullOrBlank()
}

internal fun shouldStopLivePlaybackOnRouteDispose(isChangingConfigurations: Boolean): Boolean {
    return !isChangingConfigurations
}

internal fun resolveLiveInteractionSegmentedControlSpec(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): LiveInteractionSegmentedControlSpec {
    val compactChrome = resolveCompactCapsuleChromeSpec(uiPreset, androidNativeVariant)
    return LiveInteractionSegmentedControlSpec(
        horizontalPaddingDp = compactChrome.chipHorizontalPaddingDp,
        verticalPaddingDp = compactChrome.standardGapDp,
        heightDp = compactChrome.primaryHeightDp,
        indicatorHeightDp = compactChrome.compactChipHeightDp,
        labelFontSizeSp = 14
    )
}

internal fun resolveLandscapeLiveChatVisualSpec(): LandscapeLiveChatVisualSpec {
    return LandscapeLiveChatVisualSpec(
        headerFontSizeSp = 13,
        subtitleFontSizeSp = 11,
        medalFontSizeSp = 9,
        messageFontSizeSp = 14,
        horizontalPaddingDp = 12,
        verticalPaddingDp = 10,
        medalHorizontalPaddingDp = 3,
        medalVerticalPaddingDp = 0.5f,
    )
}

internal fun resolveLiveMedalColor(colorInt: Int): Color {
    return if (colorInt != 0) Color(colorInt) else LiveStatusPalette.MedalFallback
}

internal fun resolveLiveSuperChatColor(colorInt: Int): Color {
    return if (colorInt != 0) Color(colorInt) else LiveStatusPalette.SuperChatFallback
}

internal fun resolveLiveMedalBadgeVisualSpec(): LiveMedalBadgeVisualSpec {
    return LiveMedalBadgeVisualSpec(
        verticalPaddingDp = 0.5f,
        dividerWidthDp = 0.5f,
    )
}

internal fun resolveLiveLevelColor(level: Int): Color = when {
    level >= 40 -> LiveStatusPalette.LevelHigh
    level >= 20 -> LiveStatusPalette.LevelMedium
    else -> LiveStatusPalette.LevelLow
}

internal fun resolveLivePiliPlusRoomColorTokens(
    inputOverlayColor: Color = LiveStatusPalette.MediaContent,
    inputContentColor: Color = LiveStatusPalette.InputContent,
): LivePiliPlusRoomColorTokens {
    return LivePiliPlusRoomColorTokens(
        baseBackgroundColor = LiveStatusPalette.MediaScrim,
        backdropImageAlpha = 0.60f,
        inputContainerAlpha = 0.10f,
        inputOverlayColor = inputOverlayColor,
        inputContentColor = inputContentColor
    )
}
