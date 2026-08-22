package com.android.purebilibili.feature.home.components.liquid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LensRefractionPolicyTest {

    @Test
    fun `ordinary edge refraction remains unchanged`() {
        assertEquals(
            24f,
            resolveSafeLensRefractionHeightPx(
                requestedHeightPx = 24f,
                lensMinDimensionPx = 64f,
            ),
            0.001f,
        )
    }

    @Test
    fun `strong refraction leaves an undistorted center band`() {
        val resolved = resolveSafeLensRefractionHeightPx(
            requestedHeightPx = 43.2f,
            lensMinDimensionPx = 64f,
        )

        assertEquals(29.44f, resolved, 0.001f)
        assertTrue(resolved * 2f < 64f)
    }

    @Test
    fun `invalid lens geometry disables refraction`() {
        assertEquals(0f, resolveSafeLensRefractionHeightPx(24f, 0f), 0.001f)
        assertEquals(0f, resolveSafeLensRefractionHeightPx(-1f, 64f), 0.001f)
    }
}
