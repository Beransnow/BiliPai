package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppIconStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSemanticIconNoDuplicatePolicyTest {

    @Test
    fun settingsSemanticRoles_doNotReuseAssetsWithinAnyStyle() {
        AppIconStyle.entries.forEach { style ->
            val keys = SettingsIconRole.entries.map { role ->
                resolveSettingsSemanticIconAssetKey(role, style)
            }

            assertEquals(
                keys.size,
                keys.toSet().size,
                "${style.name} 不允许设置语义图标复用同一个图标资产"
            )

            val vectorNames = SettingsIconRole.entries.map { role ->
                resolveSettingsSemanticIcon(role, style).name
            }

            assertEquals(
                vectorNames.size,
                vectorNames.toSet().size,
                "${style.name} 不允许设置语义图标复用同一个 ImageVector.name"
            )
        }
    }
}
