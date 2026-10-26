package com.android.purebilibili.feature.video.ui.overlay

data class TopControlBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val timeFontSp: Int,
    val timeBottomSpacingDp: Int,
    val buttonSizeDp: Int,
    val iconSizeDp: Int,
    val backToTitleSpacingDp: Int,
    val sectionGapDp: Int,
    val actionSpacingDp: Int,
    val titleFontSp: Int,
    val onlineCountStartPaddingDp: Int,
    val onlineCountFontSp: Int,
    val onlineCountTopPaddingDp: Int
)

fun resolveTopControlBarLayoutPolicy(
    widthDp: Int
): TopControlBarLayoutPolicy {
    if (widthDp >= 1600) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 32,
            verticalPaddingDp = 12,
            timeFontSp = 15,
            timeBottomSpacingDp = 8,
            buttonSizeDp = 44,
            iconSizeDp = 30,
            backToTitleSpacingDp = 20,
            sectionGapDp = 28,
            actionSpacingDp = 30,
            titleFontSp = 17,
            onlineCountStartPaddingDp = 60,
            onlineCountFontSp = 13,
            onlineCountTopPaddingDp = 3
        )
    }

    if (widthDp >= 1200) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 28,
            verticalPaddingDp = 11,
            timeFontSp = 14,
            timeBottomSpacingDp = 7,
            buttonSizeDp = 40,
            iconSizeDp = 28,
            backToTitleSpacingDp = 18,
            sectionGapDp = 26,
            actionSpacingDp = 28,
            titleFontSp = 17,
            onlineCountStartPaddingDp = 56,
            onlineCountFontSp = 12,
            onlineCountTopPaddingDp = 2
        )
    }

    if (widthDp >= 600) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 26,
            verticalPaddingDp = 10,
            timeFontSp = 13,
            timeBottomSpacingDp = 6,
            buttonSizeDp = 36,
            iconSizeDp = 26,
            backToTitleSpacingDp = 16,
            sectionGapDp = 24,
            actionSpacingDp = 26,
            titleFontSp = 16,
            onlineCountStartPaddingDp = 52,
            onlineCountFontSp = 12,
            onlineCountTopPaddingDp = 2
        )
    }

    return TopControlBarLayoutPolicy(
        horizontalPaddingDp = 24,
        verticalPaddingDp = 10,
        timeFontSp = 12,
        timeBottomSpacingDp = 5,
        buttonSizeDp = 32,
        iconSizeDp = 24,
        backToTitleSpacingDp = 14,
        sectionGapDp = 20,
        actionSpacingDp = 24,
        titleFontSp = 16,
        onlineCountStartPaddingDp = 48,
        onlineCountFontSp = 11,
        onlineCountTopPaddingDp = 2
    )
}

internal fun shouldShowDislikeInTopControlBar(widthDp: Int): Boolean = widthDp >= 980
internal fun shouldShowInteractiveActionsInTopControlBar(
    showFullscreenActionItems: Boolean
): Boolean = showFullscreenActionItems
/**
 * 顶栏是否加状态栏 padding。
 *
 * 以 [statusBarVisible] 为准；全屏时系统栏通常已隐藏，应传 false。
 * [isFullscreen] 仅作兼容旧调用：未显式传 statusBarVisible 时，全屏仍 pad
 *（隐藏栏时 inset 为 0，无视觉影响）。
 */
internal fun shouldApplyStatusBarPaddingToTopControlBar(
    isFullscreen: Boolean = false,
    statusBarVisible: Boolean = isFullscreen,
): Boolean = com.android.purebilibili.feature.video.screen
    .shouldApplyStatusBarPaddingToVideoPlayerChrome(statusBarVisible = statusBarVisible)
internal fun shouldPollTopControlBarClock(
    showCurrentTime: Boolean,
    hostLifecycleStarted: Boolean
): Boolean = showCurrentTime && hostLifecycleStarted
internal fun shouldPollTopControlBarBattery(
    showBatteryLevel: Boolean,
    hostLifecycleStarted: Boolean
): Boolean = showBatteryLevel && hostLifecycleStarted

internal enum class BatteryChargeTone {
    CRITICAL,
    LOW,
    NORMAL
}

internal data class LandscapeBatteryVisualState(
    val percent: Int,
    val fillFraction: Float,
    val chargeTone: BatteryChargeTone
) {
    val displayText: String
        get() = "$percent%"
}

internal fun resolveLandscapeBatteryVisualState(percent: Int?): LandscapeBatteryVisualState? {
    val normalizedPercent = percent?.coerceIn(0, 100) ?: return null
    val chargeTone = when {
        normalizedPercent <= 15 -> BatteryChargeTone.CRITICAL
        normalizedPercent <= 35 -> BatteryChargeTone.LOW
        else -> BatteryChargeTone.NORMAL
    }
    return LandscapeBatteryVisualState(
        percent = normalizedPercent,
        fillFraction = normalizedPercent / 100f,
        chargeTone = chargeTone
    )
}

internal data class LandscapeTopStatusInfo(
    val battery: LandscapeBatteryVisualState? = null,
    val currentTimeText: String? = null
) {
    val isVisible: Boolean
        get() = battery != null || !currentTimeText.isNullOrBlank()
}

internal fun resolveLandscapeTopStatusInfo(
    showBatteryLevel: Boolean,
    batteryLevelPercent: Int?,
    showCurrentTime: Boolean,
    currentTimeText: String
): LandscapeTopStatusInfo {
    return LandscapeTopStatusInfo(
        battery = if (showBatteryLevel) resolveLandscapeBatteryVisualState(batteryLevelPercent) else null,
        currentTimeText = currentTimeText.takeIf { showCurrentTime }
    )
}

internal fun shouldShowLandscapeMetaRow(
    hasStatusInfo: Boolean,
    onlineCount: String
): Boolean = hasStatusInfo || onlineCount.isNotBlank()

