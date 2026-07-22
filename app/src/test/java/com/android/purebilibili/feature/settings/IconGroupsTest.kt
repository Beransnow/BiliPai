package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class IconGroupsTest {

    @Test
    fun getIconGroups_onlyContainsCurrentBrandIcons() {
        val keys = getIconGroups().flatMap { group -> group.icons }.map { option -> option.key }.toSet()

        assertEquals(
            setOf(
                "icon_blue_snow_maid",
                "icon_3d",
                "icon_bilipai",
                "icon_bilipai_pink",
                "icon_bilipai_white",
                "icon_bilipai_monet"
            ),
            keys
        )
    }
}
