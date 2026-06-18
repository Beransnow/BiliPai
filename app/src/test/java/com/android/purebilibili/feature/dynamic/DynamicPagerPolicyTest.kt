package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicPagerPolicyTest {

    @Test
    fun `稳定页索引解析为对应逻辑 Tab`() {
        val visibleTabs = resolveDynamicVisibleTabs(setOf("all", "pgc", "up"))

        assertEquals(2, resolveDynamicSettledLogicalTab(1, visibleTabs))
        assertEquals(4, resolveDynamicSettledLogicalTab(2, visibleTabs))
        assertEquals(null, resolveDynamicSettledLogicalTab(3, visibleTabs))
    }

    @Test
    fun `Pager 不在组合阶段创建列表状态或切页回顶`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()

        assertFalse(source.contains("derivedStateOf {\n            val tab = visibleTabs.getOrNull(pagerState.currentPage)"))
        assertFalse(source.contains("listStates.getOrPut"))
        assertFalse(source.contains("beyondViewportPageCount = 1"))
        assertFalse(source.contains("activeListState?.scrollToItem(0)"))
        assertTrue(source.contains("snapshotFlow { pagerState.settledPage }"))
    }
}
