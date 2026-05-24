package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.AppIconStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconStyleSettingsPolicyTest {

    @Test
    fun nullPreferenceValue_defaultsToLucide() {
        assertEquals(AppIconStyle.LUCIDE, resolveAppIconStylePreferenceValue(null))
    }

    @Test
    fun invalidPreferenceValue_fallsBackToLucide() {
        assertEquals(AppIconStyle.LUCIDE, resolveAppIconStylePreferenceValue(99))
    }

    @Test
    fun persistedValues_restoreMatchingStyle() {
        AppIconStyle.entries.forEach { style ->
            assertEquals(style, resolveAppIconStylePreferenceValue(style.value))
        }
    }
}
