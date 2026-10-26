package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntRect
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayOptions
import com.android.purebilibili.feature.video.ui.components.AudioQualitySelectionMenuDialog
import com.android.purebilibili.feature.video.ui.components.PlayerListPopupPlacement
import com.android.purebilibili.feature.video.ui.components.PlayerSelectionItem
import com.android.purebilibili.feature.video.ui.components.PlayerSelectionPopup

/** Native selection presentation, including the shortcuts formerly exclusive to glass chrome. */
@Composable
internal fun PlayerQuickMenu(
    menu: PlayerQuickMenu,
    state: VideoPlayerOverlayState,
    actions: VideoPlayerOverlayActions,
    subtitles: SubtitleControlUiState,
    subtitleActions: SubtitleControlCallbacks,
    anchorBounds: IntRect?,
    onDismiss: () -> Unit,
) {
    val placement = if (anchorBounds != null)
        PlayerListPopupPlacement.ANCHOR_END else PlayerListPopupPlacement.END
    if (menu == PlayerQuickMenu.AUDIO) {
        AudioQualitySelectionMenuDialog(
            options = state.availableAudioQualities,
            requestedAudioQuality = state.currentAudioQuality,
            selectedAudioQuality = state.selectedAudioQuality,
            onAudioQualitySelected = { actions.onAudioQualityChange(it); onDismiss() },
            onDismiss = onDismiss,
            placement = placement,
            anchorBounds = anchorBounds,
        )
        return
    }
    PlayerSelectionPopup(
        title = when (menu) {
            PlayerQuickMenu.SUBTITLE -> "字幕"
            PlayerQuickMenu.AUDIO -> "音质"
        },
        placement = placement,
        anchorBounds = anchorBounds,
        onDismissRequest = onDismiss,
    ) {
        when (menu) {
            PlayerQuickMenu.SUBTITLE -> {
                val modes = resolveSubtitleDisplayOptions(
                    subtitles.primaryLabel, subtitles.secondaryLabel,
                    subtitles.primaryAvailable, subtitles.secondaryAvailable,
                )
                val tracks = resolveSubtitlePanelTrackOptions(subtitles.trackOptions)
                val count = modes.size + tracks.size + 2
                modes.forEachIndexed { index, mode ->
                    PlayerSelectionItem(
                        text = mode.label, selected = subtitles.displayMode == mode.mode,
                        enabled = mode.enabled, index = index, optionCount = count,
                        onClick = { subtitleActions.onDisplayModeChange(mode.mode); onDismiss() },
                    )
                }
                tracks.forEachIndexed { index, track ->
                    PlayerSelectionItem(
                        text = track.label, selected = subtitles.enabled && track.selected,
                        index = modes.size + index, optionCount = count,
                        onClick = {
                            subtitleActions.onTrackSelected(track.trackKey)
                            subtitleActions.onDisplayModeChange(SubtitleDisplayMode.PRIMARY_ONLY)
                            onDismiss()
                        },
                    )
                }
                PlayerSelectionItem(
                    text = "大字号", selected = subtitles.largeTextEnabled,
                    index = count - 2, optionCount = count,
                    onClick = { subtitleActions.onLargeTextChange(!subtitles.largeTextEnabled) },
                )
                PlayerSelectionItem(
                    text = "锁定位置", selected = subtitles.positionLocked,
                    index = count - 1, optionCount = count,
                    onClick = { subtitleActions.onPositionLockedChange(!subtitles.positionLocked) },
                )
            }
            PlayerQuickMenu.AUDIO -> Unit
        }
    }
}
