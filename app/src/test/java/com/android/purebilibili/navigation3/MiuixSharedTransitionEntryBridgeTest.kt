package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals

class MiuixSharedTransitionEntryBridgeTest {
    @Test
    fun stableTopAnimatesVisibleAndCoveredEntryAnimatesHidden() {
        assertEquals(
            MiuixSharedTransitionEntryCommand.AnimateVisible,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = true,
                isTopEntry = true,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = false,
            ),
        )
        assertEquals(
            MiuixSharedTransitionEntryCommand.AnimateHidden,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = false,
                isTopEntry = false,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = false,
            ),
        )
    }

    @Test
    fun predictivePreviewOnlyDefersOutgoingAndReturnTarget() {
        assertEquals(
            MiuixSharedTransitionEntryCommand.DeferHidden,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = true,
                isTopEntry = true,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = true,
            ),
        )
        assertEquals(
            MiuixSharedTransitionEntryCommand.DeferVisible,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = false,
                isTopEntry = false,
                isPredictiveBackTarget = true,
                predictiveBackPreviewInProgress = true,
            ),
        )
    }

    @Test
    fun predictiveCancelRestoresStableCommandsWithoutStartingTargetMorph() {
        assertEquals(
            MiuixSharedTransitionEntryCommand.AnimateVisible,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = true,
                isTopEntry = true,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = false,
            ),
        )
        assertEquals(
            MiuixSharedTransitionEntryCommand.AnimateHidden,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = false,
                isTopEntry = false,
                isPredictiveBackTarget = true,
                predictiveBackPreviewInProgress = false,
            ),
        )
    }

    @Test
    fun nonParticipantKeepsItsContentVisibleForMiuixNavigation() {
        assertEquals(
            MiuixSharedTransitionEntryCommand.HoldVisible,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = false,
                isVideoDetailEntry = false,
                isTopEntry = false,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = false,
            ),
        )
    }

    @Test
    fun committedPopMakesTheNewSourceTopAnimateVisibleEvenIfPreviewFlagLingers() {
        assertEquals(
            MiuixSharedTransitionEntryCommand.AnimateVisible,
            resolveMiuixSharedTransitionEntryCommand(
                participatesInSharedTransition = true,
                isVideoDetailEntry = false,
                isTopEntry = true,
                isPredictiveBackTarget = false,
                predictiveBackPreviewInProgress = true,
            ),
        )
    }
}
