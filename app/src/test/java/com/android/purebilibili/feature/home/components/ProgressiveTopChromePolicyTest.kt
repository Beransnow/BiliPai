package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressiveTopChromePolicyTest {
    @Test
    fun sharedProgressiveBlurUsesTheSoftTopEdgePreset() {
        assertEquals(10f, BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP)
        val source = loadSource("feature/home/components/ProgressiveTopChrome.kt")
        assertTrue(source.contains("gradient = ProgressiveBlur.Top"))
    }

    @Test
    fun progressiveBlurRequiresEnabledBackdropAndAndroid13() {
        assertTrue(shouldUseBiliPaiProgressiveTopBlur(true, true, sdkInt = 33))
        assertFalse(shouldUseBiliPaiProgressiveTopBlur(false, true, sdkInt = 33))
        assertFalse(shouldUseBiliPaiProgressiveTopBlur(true, false, sdkInt = 33))
        assertFalse(shouldUseBiliPaiProgressiveTopBlur(true, true, sdkInt = 32))
    }

    @Test
    fun homeDynamicAndCommonListReuseTheSharedProgressiveTopBlur() {
        val homeHeader = loadSource("feature/home/components/HomeHeader.kt")
        val dynamicTopBar = loadSource("feature/dynamic/components/DynamicTopBar.kt")
        val commonList = loadSource("feature/list/CommonListScreen.kt")

        assertTrue(homeHeader.contains("Modifier.biliPaiProgressiveTopBlur("))
        assertTrue(homeHeader.contains("homeSettings?.androidNativeLiquidGlassEnabled == true"))
        assertTrue(dynamicTopBar.contains("modifier.biliPaiProgressiveTopBlur("))
        assertTrue(dynamicTopBar.contains("enabled = liquidGlassEnabled"))
        assertTrue(commonList.contains(".biliPaiProgressiveTopBlur("))
        assertTrue(commonList.contains("enabled = homeSettings.androidNativeLiquidGlassEnabled"))
    }

    @Test
    fun progressiveBlurContainersIncludeTheStatusBarBand() {
        val homeHeader = loadSource("feature/home/components/HomeHeader.kt")
        val dynamicTopBar = loadSource("feature/dynamic/components/DynamicTopBar.kt")
        val commonList = loadSource("feature/list/CommonListScreen.kt")

        assertTrue(homeHeader.contains("val continuousSlabHeight = pinnedChromeLayout.blurHeight"))
        assertTrue(dynamicTopBar.contains("Spacer(modifier = Modifier.height(statusBarHeight))"))
        assertTrue(commonList.contains(".then(topBarBackgroundModifier)"))
    }

    private fun loadSource(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }.readText()
    }
}
