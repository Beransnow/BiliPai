package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppChromeNavigationApiStructureTest {

    @Test
    fun neutralChromeApisDelegateToExistingAdaptiveRenderers() {
        val chrome = loadSource("app/src/main/java/com/android/purebilibili/core/ui/AdaptiveChrome.kt")
        val navigation = loadSource("app/src/main/java/com/android/purebilibili/core/ui/AdaptiveNavigation.kt")

        assertTrue(chrome.contains("fun AppScaffold("))
        assertTrue(chrome.contains(") = AdaptiveScaffold("))
        assertTrue(chrome.contains("fun AppTopBar("))
        assertTrue(chrome.contains(") = AdaptiveTopAppBar("))
        assertTrue(navigation.contains("fun AppNavigation("))
        assertTrue(navigation.contains(") = AdaptiveNavigationContainer("))
        assertTrue(navigation.contains("fun AppSideNavigationRail("))
        assertTrue(navigation.contains(") = AdaptiveSideNavigationRail("))
        assertTrue(navigation.contains("fun AppSplitLayout("))
        assertTrue(navigation.contains(") = AdaptiveSplitLayout("))
    }

    @Test
    fun featureAndNavigationCallersUseNeutralChromeApis() {
        val sourceRoots = listOf(
            File("app/src/main/java/com/android/purebilibili/feature"),
            File("app/src/main/java/com/android/purebilibili/navigation"),
        )
        val legacyCalls = Regex(
            """\b(AdaptiveScaffold|AdaptiveTopAppBar|AdaptiveNavigationContainer|AdaptiveSideNavigationRail|AdaptiveSplitLayout)\s*\("""
        )
        val offenders = sourceRoots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .filter { legacyCalls.containsMatchIn(it.readText()) }

        assertFalse(offenders.isNotEmpty(), "Legacy chrome callers: ${offenders.joinToString { it.path }}")
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .firstOrNull(File::exists)
            ?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
