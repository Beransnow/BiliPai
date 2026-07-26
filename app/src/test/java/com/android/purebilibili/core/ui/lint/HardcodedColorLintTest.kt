package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedColorLintTest {
    @Test
    fun migrated_features_use_theme_roles_or_named_palette() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex(
                """Color\(\s*(?:0x[0-9A-Fa-f]+|\d+)\s*\)|""" +
                    """Color\.(?:Black|White|Red|Blue|Green|Yellow|Gray|LightGray|DarkGray)""",
            ),
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains raw colors. Use theme roles or a named Palette/Policy.\n" +
                offenders.joinToString("\n"),
        )
    }
}
