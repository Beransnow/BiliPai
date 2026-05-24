package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconVisualMotifPolicyTest {

    @Test
    fun commonSettingsEntries_doNotCollapseToSameVisualMotif() {
        val entries = listOf(
            "settings_donate",
            "settings_open_source_license",
            "settings_open_source_home",
            "settings_source_consistency",
            "settings_build_source",
            "settings_sha_256",
            "settings_check_update",
            "settings_release_notes"
        )
        val motifs = entries.map(::resolveIconVisualMotifForAudit)

        assertEquals(
            motifs.size,
            motifs.toSet().size,
            "常见设置入口不能只靠不同 ImageVector.name 去重，主图形也必须不同"
        )
    }
}
