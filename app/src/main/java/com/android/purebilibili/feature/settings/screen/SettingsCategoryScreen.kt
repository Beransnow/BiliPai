package com.android.purebilibili.feature.settings.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.feature.settings.SettingsNavDestination
import com.android.purebilibili.feature.settings.SettingsRootCategory
import com.android.purebilibili.feature.settings.SettingsScreen
import com.android.purebilibili.feature.settings.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun SettingsCategoryScreen(
    category: SettingsRootCategory,
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onAnimationClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onMessageNotificationClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onSettingsShareClick: () -> Unit = {},
    onWebDavBackupClick: () -> Unit = {},
    onNavigateToBottomBarSettings: () -> Unit = {},
    onTipsClick: () -> Unit = {},
    onReplayOnboardingClick: () -> Unit = {},
    onCategoryClick: (SettingsRootCategory) -> Unit = {},
    onSearchOpen: () -> Unit = {},
    mainHazeState: HazeState? = null,
    forceSinglePaneContent: Boolean = false,
) {
    if (com.android.purebilibili.feature.settings.canonicalSettingsRootCategory(category) == SettingsRootCategory.HOME_RECOMMENDATION) {
        com.android.purebilibili.feature.settings.HomeSettingsScreen(viewModel = viewModel, onBack = onBack)
        return
    }
    if (category == SettingsRootCategory.GLASS_ADVANCED) {
        com.android.purebilibili.feature.settings.AnimationSettingsScreen(
            viewModel = viewModel,
            onBack = onBack,
            advancedOnly = true,
        )
        return
    }
    val playbackPage = when (com.android.purebilibili.feature.settings.canonicalSettingsRootCategory(category)) {
        SettingsRootCategory.PLAYBACK_QUALITY -> com.android.purebilibili.feature.settings.PlaybackSettingsPage.PLAYBACK
        SettingsRootCategory.FULLSCREEN_GESTURE -> com.android.purebilibili.feature.settings.PlaybackSettingsPage.FULLSCREEN
        SettingsRootCategory.COMMENTS_CONTENT -> com.android.purebilibili.feature.settings.PlaybackSettingsPage.COMMENTS
        SettingsRootCategory.VIDEO_DECODER -> com.android.purebilibili.feature.settings.PlaybackSettingsPage.DECODER
        else -> null
    }
    if (playbackPage != null) {
        com.android.purebilibili.feature.settings.PlaybackSettingsScreen(
            viewModel = viewModel,
            onBack = onBack,
            page = playbackPage,
            onOpenPage = { page ->
                onCategoryClick(when (page) {
                    com.android.purebilibili.feature.settings.PlaybackSettingsPage.DECODER -> SettingsRootCategory.VIDEO_DECODER
                    com.android.purebilibili.feature.settings.PlaybackSettingsPage.DIAGNOSTICS -> SettingsRootCategory.PLAYER_DIAGNOSTICS
                    com.android.purebilibili.feature.settings.PlaybackSettingsPage.FULLSCREEN -> SettingsRootCategory.FULLSCREEN_GESTURE
                    com.android.purebilibili.feature.settings.PlaybackSettingsPage.COMMENTS -> SettingsRootCategory.COMMENTS_CONTENT
                    else -> SettingsRootCategory.PLAYBACK_QUALITY
                })
            },
        )
        return
    }
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack,
        onOpenSourceLicensesClick = onOpenSourceLicensesClick,
        onAppearanceClick = onAppearanceClick,
        onHomeClick = onHomeClick,
        onAnimationClick = onAnimationClick,
        onPlaybackClick = onPlaybackClick,
        onPermissionClick = onPermissionClick,
        onMessageNotificationClick = onMessageNotificationClick,
        onPluginsClick = onPluginsClick,
        onSettingsShareClick = onSettingsShareClick,
        onWebDavBackupClick = onWebDavBackupClick,
        onNavigateToBottomBarSettings = onNavigateToBottomBarSettings,
        onTipsClick = onTipsClick,
        onReplayOnboardingClick = onReplayOnboardingClick,
        onCategoryClick = onCategoryClick,
        onSearchOpen = onSearchOpen,
        destination = SettingsNavDestination.Category(category),
        mainHazeState = mainHazeState,
        forceSinglePaneContent = forceSinglePaneContent,
    )
}
