package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTextTapCopyPolicyTest {

    @Test
    fun plainShortTapCopiesText() {
        assertTrue(
            shouldCopyGlobalTextTap(
                text = "可复制文字",
                gestureCanceled = false,
                pressDurationMillis = 120L,
                longPressTimeoutMillis = 500L,
            )
        )
    }

    @Test
    fun blankConsumedMovedOrLongPressDoesNotCopy() {
        assertFalse(shouldCopyGlobalTextTap(" ", false, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("按钮文字", true, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("滚动文字", true, 120L, 500L))
        assertFalse(shouldCopyGlobalTextTap("长按文字", false, 500L, 500L))
    }
}
