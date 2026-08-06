package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppThemeSelection
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceUiPresetSegmentPolicyTest {

    @Test
    fun uiStyleSegmentOptions_exposeStableOrder_andUseProvidedLabels() {
        val options = resolveThemeSelectionOptions(
            material3Label = "Material 3",
            miuixLabel = "Miuix",
        )

        assertEquals(
            listOf(AppThemeSelection.MATERIAL3, AppThemeSelection.MIUIX),
            options.map { it.value }
        )
        assertEquals(
            listOf("Material 3", "Miuix"),
            options.map { it.label }
        )
    }
}
