package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppThemeAdaptiveTabRowStructureTest {
    @Test
    fun `global tab row dispatches md3 underline and miuix liquid capsule`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidAwareTabRow.kt"
        ).readText()

        assertTrue(source.contains("LocalAppUiStyle.current == AppUiStyle.MATERIAL3"))
        assertTrue(source.contains("AppNativeTabRow("))
        assertTrue(source.contains("AppLiquidAwareTabRow("))
        assertTrue(source.contains("miuixBackdrop = miuixBackdrop"))
    }
}
