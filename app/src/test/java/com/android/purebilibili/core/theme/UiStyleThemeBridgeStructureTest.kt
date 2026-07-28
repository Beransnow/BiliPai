package com.android.purebilibili.core.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class UiStyleThemeBridgeStructureTest {

    @Test
    fun appTheme_providesNewStyleAlongsideLegacyCompatibilityLocals() {
        val source = themeSource()

        assertTrue(source.contains("uiStyle: UiStyle = resolveUiStyle(uiPreset, androidNativeVariant)"))
        assertTrue(source.contains("LocalUiStyle provides uiStyle"))
        assertTrue(source.contains("LocalUiPreset provides uiPreset"))
        assertTrue(source.contains("LocalAndroidNativeVariant provides androidNativeVariant"))
    }

    private fun themeSource(): String {
        val relative = "core/theme/Theme.kt"
        return listOf(
            File("src/main/java/com/android/purebilibili/$relative"),
            File("app/src/main/java/com/android/purebilibili/$relative"),
        ).firstOrNull(File::exists)?.readText()
            ?: error("Theme.kt not found from ${File(".").absolutePath}")
    }
}
