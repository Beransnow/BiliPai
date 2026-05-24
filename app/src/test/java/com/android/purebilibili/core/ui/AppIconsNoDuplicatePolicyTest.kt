package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppIconStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconsNoDuplicatePolicyTest {

    @Test
    fun appIconRoles_doNotReuseAssetsWithinAnyStyle() {
        AppIconStyle.entries.forEach { style ->
            val keys = appIconRolesForDuplicateAudit().map { role ->
                resolveAppIconAssetKey(role, style)
            }

            assertEquals(
                keys.size,
                keys.toSet().size,
                "${style.name} 不允许多个全局图标角色复用同一个图标资产"
            )

            val vectorNames = appIconRolesForDuplicateAudit().map { role ->
                resolveAppIconVectorForDuplicateAudit(role, style).name
            }

            assertEquals(
                vectorNames.size,
                vectorNames.toSet().size,
                "${style.name} 不允许多个全局图标角色复用同一个 ImageVector.name"
            )
        }
    }
}
