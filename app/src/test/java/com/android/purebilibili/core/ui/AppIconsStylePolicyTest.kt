package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppIconStyle
import kotlin.test.Test
import kotlin.test.assertTrue

class AppIconsStylePolicyTest {

    @Test
    fun coreSemanticIcons_resolveForEveryIconStyle() {
        AppIconStyle.entries.forEach { style ->
            appIconRolesForDuplicateAudit().forEach { role ->
                val icon = resolveAppIconVectorForDuplicateAudit(role, style)

                assertTrue(
                    icon.name.isNotBlank(),
                    "${style.name} 的 ${role.name} 必须解析到可命名图标"
                )
            }
        }
    }
}
