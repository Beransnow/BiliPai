package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLanguageStateTest {

    @Test
    fun settingsUiState_defaultsToIosStyle() {
        assertEquals(UiStyle.IOS, SettingsUiState().uiStyle)
    }

    @Test
    fun settingsUiState_preservesExplicitUiStyle() {
        assertEquals(
            UiStyle.MIUIX,
            SettingsUiState(uiStyle = UiStyle.MIUIX).uiStyle,
        )
    }

    @Test
    fun settingsUiState_defaultsToFollowSystemLanguage() {
        assertEquals(
            AppLanguage.FOLLOW_SYSTEM,
            SettingsUiState().appLanguage
        )
    }

    @Test
    fun settingsUiState_preservesExplicitLanguageSelection() {
        val state = SettingsUiState(appLanguage = AppLanguage.ENGLISH)

        assertEquals(AppLanguage.ENGLISH, state.appLanguage)
    }
}
