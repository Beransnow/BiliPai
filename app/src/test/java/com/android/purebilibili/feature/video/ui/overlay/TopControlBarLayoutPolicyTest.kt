package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopControlBarLayoutPolicyTest {
    @Test
    fun topBar_padsOnlyWhenHostReportsVisibleStatusBar() {
        for (fullscreen in listOf(false, true)) {
            assertTrue(shouldApplyStatusBarPaddingToTopControlBar(fullscreen, statusBarVisible = true))
            assertFalse(shouldApplyStatusBarPaddingToTopControlBar(fullscreen, statusBarVisible = false))
        }
    }
}
