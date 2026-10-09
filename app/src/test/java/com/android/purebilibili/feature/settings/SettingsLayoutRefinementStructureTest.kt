package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsLayoutRefinementStructureTest {
    private fun source(path: String): String = listOf(File("app/$path"), File(path))
        .first { it.exists() }.readText()

    @Test
    fun appearanceDoesNotRepeatSelectedStyleAndOwnsTextCopy() {
        val appearance = source("src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")
        val animation = source("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt")
        assertFalse(appearance.contains("AppearanceUiPresetDescriptionCard"))
        assertFalse(appearance.contains("appearance_animation_entry"))
        assertTrue(appearance.contains("SettingsTextCopyPreference()"))
        assertFalse(animation.contains("animation.global_text_tap_copy_enabled"))
    }

    @Test
    fun helpGroupsVersionWithAppInformationAndKeepsActions() {
        val sections = source("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        val information = sections.substringAfter("SettingsSectionTitle(title = \"应用信息\")")
            .substringBefore("SettingsSectionTitle(title = \"来源与验证\")")
        assertTrue(information.contains("title = \"版本\""))
        assertTrue(information.contains("onClick = onVersionClick"))
        assertTrue(information.contains("用户协议与隐私政策"))
        assertFalse(sections.contains("SettingsSectionTitle(title = \"辅助\")"))
        assertFalse(sections.contains("SettingsDetailGroup(title = \"问题排查\")"))
        assertTrue(sections.contains("onClick = onDiagnosticsClick"))
    }
}
