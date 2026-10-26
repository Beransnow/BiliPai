package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.store.PlayerProgressPlacement

data class BottomControlBarLayoutPolicy(
    val bottomPaddingDp: Int,
    val progressSpacingDp: Int,
    val horizontalPaddingDp: Int,
    val playButtonSizeDp: Int,
    val playIconSizeDp: Int,
    val afterPlaySpacingDp: Int,
    val timeFontSp: Int,
    val afterTimeSpacingDp: Int,
    val danmakuIconSizeDp: Int,
    val danmakuSwitchToInputSpacingDp: Int,
    val danmakuSwitchHorizontalPaddingDp: Int,
    val danmakuSwitchVerticalPaddingDp: Int,
    val danmakuInputHeightDp: Int,
    val danmakuInputStartPaddingDp: Int,
    val danmakuInputFontSp: Int,
    val danmakuSettingButtonSizeDp: Int,
    val danmakuSettingEndPaddingDp: Int,
    val danmakuSettingIconSizeDp: Int,
    val afterInputSpacingDp: Int,
    val rightActionSpacingDp: Int,
    val actionChipHorizontalPaddingDp: Int,
    val actionChipVerticalPaddingDp: Int,
    val actionTextFontSp: Int,
    val fullscreenIconSizeDp: Int
)

/** Keep the visible time-to-icon gap independent of the expanded touch target. */
internal fun resolveTimeToIconSpacingDp(timeFontSp: Int, iconSizeDp: Int, slotSizeDp: Int): Int {
    val iconInsetDp = ((slotSizeDp - iconSizeDp) / 2).coerceAtLeast(0)
    return (timeFontSp - iconInsetDp).coerceAtLeast(0)
}

internal fun resolveBottomControlBarBottomPaddingDp(
    defaultBottomPaddingDp: Int,
    progressPlacement: PlayerProgressPlacement
): Int = if (progressPlacement == PlayerProgressPlacement.BOTTOM_EDGE) 0 else defaultBottomPaddingDp

/** Inline playback keeps the scrubber above its compact action row; fullscreen honors settings. */
internal fun resolveVideoDetailProgressPlacement(
    requestedPlacement: PlayerProgressPlacement,
    isFullscreen: Boolean
): PlayerProgressPlacement = if (isFullscreen) requestedPlacement else PlayerProgressPlacement.ABOVE_CONTROLS

/** Width changes horizontal density, never the shared inline/fullscreen action-row height. */
internal fun resolvePlayerBottomControlBarLayoutPolicy(
    widthDp: Int,
    compact: Boolean = false,
    isFullscreen: Boolean = false,
): BottomControlBarLayoutPolicy {
    val base = resolveBottomControlBarLayoutPolicy(widthDp, compact)
    return base.copy(
        progressSpacingDp = if (isFullscreen) 4 else base.progressSpacingDp,
        horizontalPaddingDp = if (isFullscreen) PlayerChromeLayout.EdgePaddingDp else base.horizontalPaddingDp,
        playButtonSizeDp = PlayerChromeLayout.CircularButtonSizeDp,
        playIconSizeDp = 22,
        afterPlaySpacingDp = 0,
        timeFontSp = 12,
        afterTimeSpacingDp = 4,
        danmakuIconSizeDp = 22,
        danmakuSwitchToInputSpacingDp = 8,
        danmakuInputHeightDp = PlayerChromeLayout.CircularButtonSizeDp,
        danmakuInputStartPaddingDp = 12,
        danmakuInputFontSp = 12,
        danmakuSettingButtonSizeDp = PlayerChromeLayout.CircularButtonSizeDp,
        danmakuSettingIconSizeDp = 22,
        afterInputSpacingDp = 8,
        rightActionSpacingDp = if (isFullscreen) 0 else base.rightActionSpacingDp,
        actionTextFontSp = 12,
        fullscreenIconSizeDp = 22,
    )
}

/** Keep the right capsule bounded so both edge groups remain visually distinct on narrow screens. */
internal fun resolveBottomRightGroupMaxWidthPx(
    availableWidthPx: Int,
    groupGapPx: Int,
    targetSizePx: Int,
): Int {
    val available = availableWidthPx.coerceAtLeast(0)
    val gap = groupGapPx.coerceIn(0, available)
    val leftReserve = targetSizePx.coerceAtLeast(0).coerceAtMost((available - gap) / 2)
    return minOf(available - gap - leftReserve, (available - gap) / 3 * 2)
}

fun resolveBottomControlBarLayoutPolicy(
    widthDp: Int,
    compact: Boolean = false
): BottomControlBarLayoutPolicy {
    if (widthDp >= 1600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 7,
            progressSpacingDp = if (compact) 5 else 8,
            horizontalPaddingDp = if (compact) 10 else 20,
            playButtonSizeDp = 48,
            playIconSizeDp = 28,
            afterPlaySpacingDp = resolveTimeToIconSpacingDp(14, 28, 48),
            timeFontSp = 14,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(14, 28, 48),
            danmakuIconSizeDp = 28,
            danmakuSwitchToInputSpacingDp = 12,
            danmakuSwitchHorizontalPaddingDp = 10,
            danmakuSwitchVerticalPaddingDp = 7,
            danmakuInputHeightDp = 44,
            danmakuInputStartPaddingDp = 18,
            danmakuInputFontSp = 15,
            danmakuSettingButtonSizeDp = 44,
            danmakuSettingEndPaddingDp = 6,
            danmakuSettingIconSizeDp = 22,
            afterInputSpacingDp = 16,
            rightActionSpacingDp = 16,
            actionChipHorizontalPaddingDp = 8,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 16,
            fullscreenIconSizeDp = 26
        )
    }
    if (widthDp >= 840) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 5,
            progressSpacingDp = if (compact) 4 else 6,
            horizontalPaddingDp = if (compact) 8 else 16,
            playButtonSizeDp = 40,
            playIconSizeDp = 26,
            afterPlaySpacingDp = resolveTimeToIconSpacingDp(12, 26, 40),
            timeFontSp = 12,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(12, 24, 40),
            danmakuIconSizeDp = 24,
            danmakuSwitchToInputSpacingDp = 10,
            danmakuSwitchHorizontalPaddingDp = 8,
            danmakuSwitchVerticalPaddingDp = 6,
            danmakuInputHeightDp = 36,
            danmakuInputStartPaddingDp = 16,
            danmakuInputFontSp = 13,
            danmakuSettingButtonSizeDp = 36,
            danmakuSettingEndPaddingDp = 5,
            danmakuSettingIconSizeDp = 18,
            afterInputSpacingDp = 12,
            rightActionSpacingDp = 14,
            actionChipHorizontalPaddingDp = 7,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 14,
            fullscreenIconSizeDp = 22
        )
    }
    if (widthDp >= 600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 5,
            progressSpacingDp = if (compact) 3 else 5,
            horizontalPaddingDp = if (compact) 6 else 12,
            playButtonSizeDp = 36,
            playIconSizeDp = 24,
            afterPlaySpacingDp = resolveTimeToIconSpacingDp(12, 24, 36),
            timeFontSp = 12,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(12, 22, 36),
            danmakuIconSizeDp = 22,
            danmakuSwitchToInputSpacingDp = 10,
            danmakuSwitchHorizontalPaddingDp = 8,
            danmakuSwitchVerticalPaddingDp = 6,
            danmakuInputHeightDp = 32,
            danmakuInputStartPaddingDp = 15,
            danmakuInputFontSp = 13,
            danmakuSettingButtonSizeDp = 34,
            danmakuSettingEndPaddingDp = 4,
            danmakuSettingIconSizeDp = 17,
            afterInputSpacingDp = 12,
            rightActionSpacingDp = 14,
            actionChipHorizontalPaddingDp = 7,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 13,
            fullscreenIconSizeDp = 20
        )
    }
    return BottomControlBarLayoutPolicy(
        bottomPaddingDp = 4,
        progressSpacingDp = if (compact) 3 else 0,
        horizontalPaddingDp = if (compact) 4 else 6,
        playButtonSizeDp = 32,
        playIconSizeDp = 22,
        afterPlaySpacingDp = resolveTimeToIconSpacingDp(11, 22, 32),
        timeFontSp = 11,
        afterTimeSpacingDp = resolveTimeToIconSpacingDp(11, 20, 32),
        danmakuIconSizeDp = 20,
        danmakuSwitchToInputSpacingDp = 8,
        danmakuSwitchHorizontalPaddingDp = 6,
        danmakuSwitchVerticalPaddingDp = 6,
        danmakuInputHeightDp = 28,
        danmakuInputStartPaddingDp = 14,
        danmakuInputFontSp = 12,
        danmakuSettingButtonSizeDp = 32,
        danmakuSettingEndPaddingDp = 4,
        danmakuSettingIconSizeDp = 16,
        afterInputSpacingDp = 10,
        rightActionSpacingDp = 8,
        actionChipHorizontalPaddingDp = 5,
        actionChipVerticalPaddingDp = 2,
        actionTextFontSp = 12,
        fullscreenIconSizeDp = 18
    )
}
