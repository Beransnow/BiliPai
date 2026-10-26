package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.components.PlayerNativeMenuTheme
import com.android.purebilibili.feature.video.ui.components.PlayerSettingsRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerSettingsRowUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun switchAndLabelTouchesEachToggleTheRowExactlyOnce() {
        val labels = listOf("左右翻转", "上下翻转", "听视频", "后台播放")
        val checked = mutableStateListOf(false, true, false, true)
        val calls = IntArray(labels.size)
        rule.setContent {
            MaterialTheme {
                PlayerNativeMenuTheme {
                    Column {
                        labels.forEachIndexed { index, label ->
                            PlayerSettingsRow(label, icon = Icons.Outlined.Flip, checked = checked[index]) {
                                calls[index]++
                                checked[index] = !checked[index]
                            }
                        }
                    }
                }
            }
        }
        labels.forEachIndexed { index, label ->
            val initial = index % 2 != 0
            val row = rule.onNodeWithText(label).assertIsToggleable()
            row.assertIsOnOrOff(initial)
            // The trailing switch is a visual child, not a second interactive toggle.
            row.performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
            row.assertIsOnOrOff(!initial)
            rule.runOnIdle { assertEquals(1, calls[index]) }
            row.performTouchInput { click(Offset(width * 0.3f, height / 2f)) }
            row.assertIsOnOrOff(initial)
            rule.runOnIdle { assertEquals(2, calls[index]) }
        }
    }

    private fun SemanticsNodeInteraction.assertIsOnOrOff(checked: Boolean) {
        if (checked) assertIsOn() else assertIsOff()
    }
}
