package com.android.purebilibili.navigation3

import androidx.compose.animation.DeferredAnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.ProvideAnimatedVisibilityScope

internal enum class MiuixSharedTransitionEntryCommand {
    HoldVisible,
    DeferVisible,
    DeferHidden,
    AnimateVisible,
    AnimateHidden,
}

internal fun resolveMiuixSharedTransitionEntryCommand(
    participatesInSharedTransition: Boolean,
    isVideoDetailEntry: Boolean,
    isTopEntry: Boolean,
    isPredictiveBackTarget: Boolean,
    predictiveBackPreviewInProgress: Boolean,
): MiuixSharedTransitionEntryCommand = when {
    !participatesInSharedTransition -> MiuixSharedTransitionEntryCommand.HoldVisible
    predictiveBackPreviewInProgress && isTopEntry && isVideoDetailEntry ->
        MiuixSharedTransitionEntryCommand.DeferHidden
    predictiveBackPreviewInProgress && isPredictiveBackTarget ->
        MiuixSharedTransitionEntryCommand.DeferVisible
    isTopEntry -> MiuixSharedTransitionEntryCommand.AnimateVisible
    else -> MiuixSharedTransitionEntryCommand.AnimateHidden
}

/**
 * Adapts a Miuix navigation entry to Compose's standard shared-transition lifecycle.
 *
 * Miuix remains responsible for the interactive page preview. During that deferred phase the
 * destination entry is only precomposed, so a cancelled gesture never starts a card morph. Once
 * navigation commits, [DeferredTransitionState.animateTo] starts the regular shared-bounds
 * transition and keeps the outgoing content composed until that transition has finished.
 */
@OptIn(ExperimentalDeferredTransitionApi::class)
@Composable
internal fun MiuixSharedTransitionEntryBridge(
    entryKey: BiliPaiNavKey,
    initiallyVisible: Boolean,
    participatesInSharedTransition: Boolean,
    isTopEntry: Boolean,
    isPredictiveBackTarget: Boolean,
    predictiveBackPreviewInProgress: Boolean,
    content: @Composable () -> Unit,
) {
    val visibilityState = remember(entryKey) {
        DeferredTransitionState(initiallyVisible)
    }
    val transition = rememberTransition(
        transitionState = visibilityState,
        label = "MiuixSharedEntry:${entryKey.routeBase}",
    )
    val command = resolveMiuixSharedTransitionEntryCommand(
        participatesInSharedTransition = participatesInSharedTransition,
        isVideoDetailEntry = isCardMorphDestinationNavKey(entryKey),
        isTopEntry = isTopEntry,
        isPredictiveBackTarget = isPredictiveBackTarget,
        predictiveBackPreviewInProgress = predictiveBackPreviewInProgress,
    )

    SideEffect {
        when (command) {
            MiuixSharedTransitionEntryCommand.HoldVisible -> visibilityState.animateTo(true)
            MiuixSharedTransitionEntryCommand.DeferVisible -> visibilityState.defer(true)
            MiuixSharedTransitionEntryCommand.DeferHidden -> visibilityState.defer(false)
            MiuixSharedTransitionEntryCommand.AnimateVisible -> visibilityState.animateTo(true)
            MiuixSharedTransitionEntryCommand.AnimateHidden -> visibilityState.animateTo(false)
        }
    }

    transition.DeferredAnimatedVisibility(
        visible = { it },
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        ProvideAnimatedVisibilityScope(this) {
            content()
        }
    }
}
