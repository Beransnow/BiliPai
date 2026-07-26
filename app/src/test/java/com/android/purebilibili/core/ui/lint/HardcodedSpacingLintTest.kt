package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedSpacingLintTest {
    @Test
    fun migrated_features_use_app_spacing_tokens() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex(
                """(?:(?:\.padding\([^\n]*|Arrangement\.spacedBy\(|Spacer\([^\n]*|""" +
                    """\.size\([^\n]*|\.height(?:In)?\([^\n]*|\.width(?:In)?\([^\n]*|""" +
                    """\.offset\([^\n]*|\bspace\s*=\s*)\b\d+(?:\.\d+)?\.dp|""" +
                    """\b\d+(?:\.\d+)?\.dp\.toPx\(\))""",
            ),
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains literal layout spacing. Use AppSpacingTokens or a named Spec.\n" +
                offenders.joinToString("\n"),
        )
    }
}
