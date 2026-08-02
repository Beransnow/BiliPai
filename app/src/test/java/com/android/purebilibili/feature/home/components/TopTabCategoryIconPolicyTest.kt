package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class TopTabCategoryIconPolicyTest {

    @Test
    fun `top category tabs use local vectors only for the four supplied symbols`() {
        assertEquals(HomeNavigationIconSource.LOCAL_LIVE, resolveMiuixPreferredHomeNavigationIconSource("LIVE"))
        assertEquals(HomeNavigationIconSource.LOCAL_GAME, resolveMiuixPreferredHomeNavigationIconSource("GAME"))
    }

    @Test
    fun `top category tabs use Miuix for every remaining category`() {
        listOf("RECOMMEND", "FOLLOW", "POPULAR", "ANIME", "KNOWLEDGE", "TECH", "PARTITION").forEach {
            assertEquals(HomeNavigationIconSource.MIUIX, resolveMiuixPreferredHomeNavigationIconSource(it))
        }
    }
}
