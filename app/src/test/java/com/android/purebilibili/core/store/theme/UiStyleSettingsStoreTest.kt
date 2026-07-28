package com.android.purebilibili.core.store.theme

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.UiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStyleSettingsStoreTest {

    @Test
    fun settingsShare_keepsNewAndLegacyStyleKeysTogether() {
        val keys = SettingsManager.getShareableSettingsEntryDefinitions()
            .map { it.storageKey }
            .toSet()

        assertEquals(
            setOf("ui_style_v1", "ui_preset", "android_native_variant_v1"),
            keys.intersect(setOf("ui_style_v1", "ui_preset", "android_native_variant_v1")),
        )
    }

    @Test
    fun missingNewKey_readsLegacyPairWithoutChangingItsHiddenVariant() {
        val preferences = mutablePreferencesOf(
            UI_PRESET_PREFERENCE_KEY to UiPreset.IOS.value,
            ANDROID_NATIVE_VARIANT_PREFERENCE_KEY to AndroidNativeVariant.MIUIX.value,
        )

        assertEquals(
            StoredUiStyleSettings(UiStyle.IOS, UiPreset.IOS, AndroidNativeVariant.MIUIX),
            resolveStoredUiStyleSettings(preferences),
        )
    }

    @Test
    fun validNewKey_controlsEffectiveLegacyValues() {
        val preferences = mutablePreferencesOf(
            UI_STYLE_PREFERENCE_KEY to UiStyle.MIUIX.value,
            UI_PRESET_PREFERENCE_KEY to UiPreset.IOS.value,
            ANDROID_NATIVE_VARIANT_PREFERENCE_KEY to AndroidNativeVariant.MATERIAL3.value,
        )

        assertEquals(
            StoredUiStyleSettings(UiStyle.MIUIX, UiPreset.MD3, AndroidNativeVariant.MIUIX),
            resolveStoredUiStyleSettings(preferences),
        )
    }

    @Test
    fun importingNewKey_repairsConflictingLegacyMirrors() {
        val preferences = mutablePreferencesOf(
            UI_STYLE_PREFERENCE_KEY to UiStyle.MATERIAL3.value,
            UI_PRESET_PREFERENCE_KEY to UiPreset.IOS.value,
            ANDROID_NATIVE_VARIANT_PREFERENCE_KEY to AndroidNativeVariant.MIUIX.value,
        )

        val repairedConflict = synchronizeImportedUiStylePreferences(
            preferences = preferences,
            importedKeys = setOf(UI_STYLE_PREFERENCE_KEY.name),
        )

        assertEquals(true, repairedConflict)
        assertEquals(UiStyle.MATERIAL3.value, preferences[UI_STYLE_PREFERENCE_KEY])
        assertEquals(UiPreset.MD3.value, preferences[UI_PRESET_PREFERENCE_KEY])
        assertEquals(
            AndroidNativeVariant.MATERIAL3.value,
            preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY],
        )
    }

    @Test
    fun importingLegacyOnlySnapshot_createsNewMirror() {
        val preferences = mutablePreferencesOf(
            UI_PRESET_PREFERENCE_KEY to UiPreset.MD3.value,
            ANDROID_NATIVE_VARIANT_PREFERENCE_KEY to AndroidNativeVariant.MIUIX.value,
        )

        val repairedConflict = synchronizeImportedUiStylePreferences(
            preferences = preferences,
            importedKeys = setOf(
                UI_PRESET_PREFERENCE_KEY.name,
                ANDROID_NATIVE_VARIANT_PREFERENCE_KEY.name,
            ),
        )

        assertEquals(false, repairedConflict)
        assertEquals(UiStyle.MIUIX.value, preferences[UI_STYLE_PREFERENCE_KEY])
        assertEquals(UiPreset.MD3.value, preferences[UI_PRESET_PREFERENCE_KEY])
        assertEquals(
            AndroidNativeVariant.MIUIX.value,
            preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY],
        )
    }

    @Test
    fun importingIosStyle_preservesCurrentHiddenVariant() {
        val preferences = mutablePreferencesOf(
            UI_STYLE_PREFERENCE_KEY to UiStyle.IOS.value,
            UI_PRESET_PREFERENCE_KEY to UiPreset.MD3.value,
            ANDROID_NATIVE_VARIANT_PREFERENCE_KEY to AndroidNativeVariant.MIUIX.value,
        )

        val repairedConflict = synchronizeImportedUiStylePreferences(
            preferences = preferences,
            importedKeys = setOf(UI_STYLE_PREFERENCE_KEY.name),
        )

        assertEquals(true, repairedConflict)
        assertEquals(UiPreset.IOS.value, preferences[UI_PRESET_PREFERENCE_KEY])
        assertEquals(
            AndroidNativeVariant.MIUIX.value,
            preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY],
        )
    }
}
