package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceUiPresetSegmentPolicyTest {

    @Test
    fun uiStyleSegmentOptions_exposeStableOrder_andUseProvidedLabels() {
        val options = resolveUiStyleSegmentOptions(
            iosLabel = "iOS",
            material3Label = "Material 3",
            miuixLabel = "Miuix",
        )

        assertEquals(
            listOf(UiStyle.IOS, UiStyle.MATERIAL3, UiStyle.MIUIX),
            options.map { it.value }
        )
        assertEquals(
            listOf("iOS", "Material 3", "Miuix"),
            options.map { it.label }
        )
    }
}
