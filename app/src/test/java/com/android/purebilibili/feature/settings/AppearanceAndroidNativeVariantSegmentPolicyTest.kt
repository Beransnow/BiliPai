package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceAndroidNativeVariantSegmentPolicyTest {

    @Test
    fun uiStyleSegmentOptions_keepAndroidStylesInStableOrder() {
        val options = resolveUiStyleSegmentOptions(
            iosLabel = "iOS",
            material3Label = "Material 3",
            miuixLabel = "Miuix",
        )

        assertEquals(
            listOf(UiStyle.MATERIAL3, UiStyle.MIUIX),
            options.drop(1).map { it.value }
        )
        assertEquals(
            listOf("Material 3", "Miuix"),
            options.drop(1).map { it.label }
        )
    }
}
