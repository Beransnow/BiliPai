package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTopBarLayoutPolicyTest {

    @Test
    fun topBarRowMinHeight_accommodatesInputHeightAndVerticalPadding() {
        assertEquals(72, resolveSearchTopBarRowMinHeightDp(inputHeightDp = 56))
        assertEquals(64, resolveSearchTopBarRowMinHeightDp(inputHeightDp = 44))
    }
}
