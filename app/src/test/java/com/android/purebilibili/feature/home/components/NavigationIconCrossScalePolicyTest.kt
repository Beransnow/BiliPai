package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.HomeSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationIconCrossScalePolicyTest {

    @Test
    fun `coverage only enlarges navigation icons during transition`() {
        assertEquals(1.085f, resolveNavigationIconCrossScale(true, 0.75f), 0.001f)
        assertEquals(1.12f, resolveNavigationIconCrossScale(true, 0.5f), 0.001f)
        assertEquals(1.085f, resolveNavigationIconCrossScale(true, 0.25f), 0.001f)
    }

    @Test
    fun `cross scale is disabled by default and both endpoints keep authored size`() {
        assertFalse(HomeSettings().navigationIconCrossScaleEnabled)
        assertEquals(1f, resolveNavigationIconCrossScale(false, 1f), 0.001f)
        assertEquals(1f, resolveNavigationIconCrossScale(true, 0f), 0.001f)
        assertEquals(1f, resolveNavigationIconCrossScale(true, 1f), 0.001f)
    }
}
