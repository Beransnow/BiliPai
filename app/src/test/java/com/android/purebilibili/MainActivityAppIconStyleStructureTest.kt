package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MainActivityAppIconStyleStructureTest {

    @Test
    fun mainActivityProvidesAppIconStyleToThemeRoot() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/MainActivity.kt")
        val themeSource = loadSource("app/src/main/java/com/android/purebilibili/core/theme/Theme.kt")

        assertTrue(source.contains("SettingsManager.getAppIconStyle(context)"))
        assertTrue(source.contains("appIconStyle = appIconStyle"))
        assertTrue(themeSource.contains("LocalAppIconStyle provides appIconStyle"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
