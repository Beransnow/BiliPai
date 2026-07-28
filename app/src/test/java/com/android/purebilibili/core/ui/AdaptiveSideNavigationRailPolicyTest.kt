package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AdaptiveSideNavigationRailIntegrationTest {

    @Test
    fun adaptiveNavigationSourceMountsMiuixRailOnMiuixBranch() {
        val source = File("src/main/java/com/android/purebilibili/core/ui/AdaptiveNavigation.kt")
            .takeIf { it.exists() }
            ?.readText()
            ?: File("app/src/main/java/com/android/purebilibili/core/ui/AdaptiveNavigation.kt").readText()

        assertTrue(source.contains("resolveAdaptiveSideNavigationRailRenderer("))
        assertTrue(source.contains("MiuixNavigationRail("))
        assertTrue(source.contains("MiuixNavigationRailItem("))
        assertTrue(source.contains("MiuixBadge"))
        assertTrue(source.contains("rememberMiuixNavigationRailState("))
    }
}
