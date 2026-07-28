package com.android.purebilibili.core.store.theme

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.android.purebilibili.core.store.resolveAndroidNativeVariantPreferenceValue
import com.android.purebilibili.core.store.resolveUiPresetPreferenceValue
import com.android.purebilibili.core.store.settingsDataStore
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.UiStyle
import com.android.purebilibili.core.theme.resolveLegacyUiStylePreferences
import com.android.purebilibili.core.theme.resolveUiStyle
import com.android.purebilibili.core.theme.resolveUiStylePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal val UI_STYLE_PREFERENCE_KEY = intPreferencesKey("ui_style_v1")
internal val UI_PRESET_PREFERENCE_KEY = intPreferencesKey("ui_preset")
internal val ANDROID_NATIVE_VARIANT_PREFERENCE_KEY =
    intPreferencesKey("android_native_variant_v1")

internal data class StoredUiStyleSettings(
    val uiStyle: UiStyle,
    val uiPreset: UiPreset,
    val androidNativeVariant: AndroidNativeVariant,
)

internal fun resolveStoredUiStyleSettings(preferences: Preferences): StoredUiStyleSettings {
    val storedPreset = resolveUiPresetPreferenceValue(preferences[UI_PRESET_PREFERENCE_KEY])
    val storedVariant = resolveAndroidNativeVariantPreferenceValue(
        preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY]
    )
    val uiStyle = resolveUiStylePreference(
        rawUiStyleValue = preferences[UI_STYLE_PREFERENCE_KEY],
        legacyUiPreset = storedPreset,
        legacyAndroidNativeVariant = storedVariant,
    )
    val effectiveLegacyValues = resolveLegacyUiStylePreferences(uiStyle, storedVariant)
    return StoredUiStyleSettings(
        uiStyle = uiStyle,
        uiPreset = effectiveLegacyValues.uiPreset,
        androidNativeVariant = effectiveLegacyValues.androidNativeVariant,
    )
}

internal fun synchronizeImportedUiStylePreferences(
    preferences: MutablePreferences,
    importedKeys: Set<String>,
): Boolean {
    val uiStyleImported = UI_STYLE_PREFERENCE_KEY.name in importedKeys
    val legacyStyleImported = UI_PRESET_PREFERENCE_KEY.name in importedKeys ||
        ANDROID_NATIVE_VARIANT_PREFERENCE_KEY.name in importedKeys
    if (!uiStyleImported && !legacyStyleImported) return false

    val storedPreset = resolveUiPresetPreferenceValue(preferences[UI_PRESET_PREFERENCE_KEY])
    val storedVariant = resolveAndroidNativeVariantPreferenceValue(
        preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY]
    )
    val importedUiStyle = if (uiStyleImported) {
        UiStyle.fromValueOrNull(preferences[UI_STYLE_PREFERENCE_KEY])
    } else {
        null
    }
    val hasNewLegacyConflict = importedUiStyle != null &&
        importedUiStyle != resolveUiStyle(storedPreset, storedVariant)
    val effectiveStyle = importedUiStyle ?: resolveUiStyle(storedPreset, storedVariant)
    writeUiStyleMirrors(preferences, effectiveStyle, storedVariant)
    return hasNewLegacyConflict
}

private fun writeUiStyleMirrors(
    preferences: MutablePreferences,
    uiStyle: UiStyle,
    currentAndroidNativeVariant: AndroidNativeVariant,
) {
    val legacyValues = resolveLegacyUiStylePreferences(uiStyle, currentAndroidNativeVariant)
    preferences[UI_STYLE_PREFERENCE_KEY] = uiStyle.value
    preferences[UI_PRESET_PREFERENCE_KEY] = legacyValues.uiPreset.value
    preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY] =
        legacyValues.androidNativeVariant.value
}

object UiStyleSettingsStore {
    fun getUiStyle(context: Context): Flow<UiStyle> = context.settingsDataStore.data
        .map { resolveStoredUiStyleSettings(it).uiStyle }
        .distinctUntilChanged()

    fun getUiPreset(context: Context): Flow<UiPreset> = context.settingsDataStore.data
        .map { resolveStoredUiStyleSettings(it).uiPreset }
        .distinctUntilChanged()

    fun getAndroidNativeVariant(context: Context): Flow<AndroidNativeVariant> =
        context.settingsDataStore.data
            .map { resolveStoredUiStyleSettings(it).androidNativeVariant }
            .distinctUntilChanged()

    suspend fun setUiStyle(context: Context, uiStyle: UiStyle) {
        context.settingsDataStore.edit { preferences ->
            val currentVariant = resolveAndroidNativeVariantPreferenceValue(
                preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY]
            )
            writeUiStyleMirrors(preferences, uiStyle, currentVariant)
        }
    }

    suspend fun setUiPreset(context: Context, uiPreset: UiPreset) {
        context.settingsDataStore.edit { preferences ->
            val currentVariant = resolveAndroidNativeVariantPreferenceValue(
                preferences[ANDROID_NATIVE_VARIANT_PREFERENCE_KEY]
            )
            writeUiStyleMirrors(
                preferences = preferences,
                uiStyle = resolveUiStyle(uiPreset, currentVariant),
                currentAndroidNativeVariant = currentVariant,
            )
        }
    }

    suspend fun setAndroidNativeVariant(
        context: Context,
        androidNativeVariant: AndroidNativeVariant,
    ) {
        context.settingsDataStore.edit { preferences ->
            val currentPreset = resolveUiPresetPreferenceValue(
                preferences[UI_PRESET_PREFERENCE_KEY]
            )
            writeUiStyleMirrors(
                preferences = preferences,
                uiStyle = resolveUiStyle(currentPreset, androidNativeVariant),
                currentAndroidNativeVariant = androidNativeVariant,
            )
        }
    }
}
