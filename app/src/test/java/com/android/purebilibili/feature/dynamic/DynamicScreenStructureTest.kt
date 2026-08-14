package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicScreenStructureTest {

    @Test
    fun `dynamic staggered grid provides stable content types`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val gridSource = source
            .substringAfter("LazyVerticalStaggeredGrid(")
            .substringBefore("@Composable\nprivate fun OldContentDivider")

        assertTrue(gridSource.contains("contentType = \"dynamic_empty_state\""))
        assertTrue(gridSource.contains("contentType = { \"dynamic_card\" }"))
        assertTrue(gridSource.contains("contentType = \"dynamic_old_content_divider\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_loading_footer\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_no_more_footer\""))
    }

    @Test
    fun `dynamic screen supports tab reselect and up panel shortcuts`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()

        assertTrue(source.contains("resolveDynamicTabReselectAction("))
        assertTrue(source.contains("DynamicTabReselectAction.SCROLL_TO_TOP"))
        assertTrue(source.contains("resolveDynamicUpPanelUsers("))
        assertTrue(source.contains("isDynamicUpPanelAllShortcut(clickedUserId)"))
        assertTrue(source.contains("onTabSelected = onDynamicTabSelected"))
        assertTrue(source.contains("val dynamicFeedBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("miuixBackdrop = dynamicFeedBackdrop"))
        assertTrue(source.contains(".background(AppSurfaceTokens.background())"))
        assertTrue(source.contains(".layerBackdrop(dynamicFeedBackdrop)"))
        assertTrue(source.indexOf(".background(AppSurfaceTokens.background())") < source.indexOf(".layerBackdrop(dynamicFeedBackdrop)"))
        assertTrue(source.contains("animateScale = false"))
        assertTrue(source.contains("shouldUseDynamicTopBarHeaderBlur("))
        assertTrue(source.contains("liquidGlassReuseEnabled = homeSettings.androidNativeLiquidGlassEnabled"))
        assertTrue(!source.contains("text = \"全\""))
        val sidebarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt"
        ).readText()
        assertTrue(!sidebarSource.contains("text = \"全\""))
        assertTrue(!sidebarSource.contains("isAllShortcut"))
    }

    @Test
    fun `report dialog and additional cards use shared native components`() {
        val screenSource = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        ).readText()
        val sidebarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt"
        ).readText()

        assertTrue(screenSource.contains("AppListItem("))
        assertTrue(screenSource.contains("AppRadioButton("))
        assertTrue(cardSource.contains("AppContentCard("))
        assertTrue(sidebarSource.contains("AppTextButton("))
    }
}
