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
        assertTrue(source.contains(") = IOSClickableItem("))
        assertTrue(source.contains(") = IOSSwitchItem("))
        assertTrue(source.contains(") = IOSSliderPreference("))
        assertTrue(source.contains(") = IOSGroup("))
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
            """\b(IOSSectionTitle|IOSGroup|IOSSwitchItem|IOSSliderPreference|IOSClickableItem|IOSDivider|IOSAdaptiveTextField|IOSSlidingSegmentedControl|IOSSlidingSegmentedSetting)\b"""
        )

        pilotPaths.forEach { path ->
            val source = loadSource(path)
            assertFalse(legacyCall.containsMatchIn(source), "Legacy preference call remains in $path")
        }
    }

    @Test
    fun appTextField_forwardsSharedInputSemanticsToNativeRenderers() {
        val apiSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppPreferenceComponents.kt"
        )
        val rendererSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt"
        )

        listOf(
            "enabled = enabled",
            "readOnly = readOnly",
            "keyboardOptions = keyboardOptions",
            "keyboardActions = keyboardActions",
            "leadingIcon = leadingIcon",
            "trailingIcon = trailingIcon",
            "visualTransformation = visualTransformation",
        ).forEach { forwarding ->
            assertTrue(apiSource.contains(forwarding), "AppTextField does not forward $forwarding")
            assertTrue(rendererSource.contains(forwarding), "Renderer does not forward $forwarding")
        }
        assertTrue(rendererSource.contains("MiuixTextField("))
        assertTrue(rendererSource.contains("OutlinedTextField("))
        val textFieldRenderer = rendererSource
            .substringAfter("fun IOSAdaptiveTextField(")
            .substringBefore("private fun MiuixAdaptiveSearchBar(")
        assertTrue(textFieldRenderer.contains("LocalUiStyle.current == UiStyle.MIUIX"))
        assertFalse(textFieldRenderer.contains("LocalUiPreset"))
        assertFalse(textFieldRenderer.contains("LocalAndroidNativeVariant"))
    }

    @Test
    fun phaseThreeInputPilot_usesNeutralTextField() {
        val pilotPaths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/RepostDialog.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveSearchScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/components/LiveInteractionSheets.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/components/LiveSendDanmakuSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
        )
        val directRendererCall = Regex("""\b(OutlinedTextField|InputField)\s*\(""")

        pilotPaths.forEach { path ->
            val source = loadSource(path)
            assertTrue(source.contains("AppTextField("), "Neutral input is missing in $path")
            assertFalse(directRendererCall.containsMatchIn(source), "Direct input renderer remains in $path")
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .firstOrNull(File::exists)
            ?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
