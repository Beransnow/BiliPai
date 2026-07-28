package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferenceApiStructureTest {

    @Test
    fun neutralPreferenceApi_delegatesToExistingAdaptiveRenderers() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppPreferenceComponents.kt"
        )

        assertTrue(source.contains("fun AppPreference("))
        assertTrue(source.contains("fun AppSwitchPreference("))
        assertTrue(source.contains("fun AppSliderPreference("))
        assertTrue(source.contains("fun AppPreferenceGroup("))
        assertTrue(source.contains("fun AppPreferenceSectionTitle("))
        assertTrue(source.contains("fun AppPreferenceDivider("))
        assertTrue(source.contains("fun AppTextField("))
        assertTrue(source.contains(") = IOSClickableItem("))
        assertTrue(source.contains(") = IOSSwitchItem("))
        assertTrue(source.contains(") = IOSSliderPreference("))
        assertTrue(source.contains(") = IOSGroup("))

        val dialogSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/AppDialogComponents.kt"
        )
        assertTrue(dialogSource.contains("fun AppAlertDialog("))
        assertTrue(dialogSource.contains(") = IOSAlertDialog("))
        assertTrue(dialogSource.contains("fun AppDialogAction("))
        assertTrue(dialogSource.contains(") = IOSDialogAction("))
    }

    @Test
    fun phaseOneSettingsPilot_usesNeutralPreferenceNames() {
        val pilotPaths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PluginsScreen.kt",
        )
        val legacyCall = Regex(
            """\b(IOSSectionTitle|IOSGroup|IOSSwitchItem|IOSSliderPreference|IOSClickableItem|IOSDivider|IOSAdaptiveTextField|IOSSlidingSegmentedControl|IOSSlidingSegmentedSetting|IOSAlertDialog|IOSDialogAction)\b"""
        )

        val requiredNeutralCalls = mapOf(
            pilotPaths[0] to listOf("AppPreferenceGroup", "AppSegmentedControl", "AppAlertDialog"),
            pilotPaths[1] to listOf("AppPreferenceGroup", "AppSegmentedPreference", "AppTextField"),
            pilotPaths[2] to listOf("AppPreferenceGroup", "AppSwitchPreference", "AppAlertDialog"),
            pilotPaths[3] to listOf("AppTextField"),
        )

        pilotPaths.forEach { path ->
            val source = loadSource(path)
            assertFalse(legacyCall.containsMatchIn(source), "Legacy preference call remains in $path")
            requiredNeutralCalls.getValue(path).forEach { neutralCall ->
                assertTrue(source.contains(neutralCall), "$neutralCall is missing from $path")
            }
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .firstOrNull(File::exists)
            ?.readText()
            ?.replace("\r\n", "\n")
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
