package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedTypographyLintTest {
    @Test
    fun migrated_features_use_theme_typography_or_named_visual_policy() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex("""(?:fontSize|lineHeight)\s*=\s*\d+(?:\.\d+)?\.sp"""),
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains literal typography. Use MaterialTheme.typography " +
                "or a named VisualPolicy for media-density exceptions.\n" + offenders.joinToString("\n"),
        )
    }
}
