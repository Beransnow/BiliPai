package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressiveTopChromePolicyTest {
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
        assertTrue(dynamicTopBar.contains("modifier.biliPaiProgressiveTopBlur("))
        assertTrue(commonList.contains(".biliPaiProgressiveTopBlur("))
    }

    private fun loadSource(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }.readText()
    }
}
