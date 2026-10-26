package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopControlBarPolicyTest {
    @Test
    fun interactiveActions_followPreference_withoutChangingShellLayout() {
        assertTrue(shouldShowInteractiveActionsInTopControlBar(showFullscreenActionItems = true))
        assertFalse(shouldShowInteractiveActionsInTopControlBar(showFullscreenActionItems = false))
    }

    @Test
    fun dislikeAction_requiresWideActionDock() {
        assertFalse(shouldShowDislikeInTopControlBar(widthDp = 979))
        assertTrue(shouldShowDislikeInTopControlBar(widthDp = 980))
    }

    @Test
    fun statusPolling_requiresBothVisibleStatusAndStartedHost() {
        for (visible in listOf(false, true)) {
            for (started in listOf(false, true)) {
                val expected = visible && started
                kotlin.test.assertEquals(expected, shouldPollTopControlBarClock(visible, started))
                kotlin.test.assertEquals(expected, shouldPollTopControlBarBattery(visible, started))
            }
        }
    }
}
