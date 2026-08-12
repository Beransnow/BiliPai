package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeNavigationMiuixStructureTest {

    @Test
    fun `home navigation runtime does not select Cupertino or Material icons`() {
        // 顶栏类别图标是刻意例外：Miuix 图标集（156 个）缺少推荐/关注/直播/番剧/游戏/
        // 知识/科技等类别图标，TopBar 类别解析器沿用 Material（与 BottomBar 同样的文档化
        // 例外）；因此严格检查只覆盖 HomeHeader 与导航图标策略，TopBar 仅禁止 Cupertino
        // 与 fallbackIconFamily 回退机制。
        val strictSources = listOf(
            "HomeHeader.kt",
            "HomeNavigationIconPolicy.kt",
        ).map(::sourceText)

        strictSources.forEach { source ->
            assertFalse(source.contains("CupertinoIcons"))
            assertFalse(source.contains("androidx.compose.material.icons"))
            assertFalse(source.contains("fallbackIconFamily"))
        }

        val topBar = sourceText("TopBar.kt")
        assertFalse(topBar.contains("CupertinoIcons"))
        assertFalse(topBar.contains("fallbackIconFamily"))
    }

    @Test
    fun `bottom bar keeps legacy Cupertino definitions and Material icon pairs`() {
        // BottomNavItem 仍保留旧版 Cupertino 定义供兼容路径使用；AUTO 浮动栏由下方测试
        // 约束为 Miuix 原生图标，MD3_STANDARD 则继续使用 Material filled/outlined 图标对。
        val source = sourceText("BottomBar.kt")

        assertTrue(source.contains("enum class BottomNavItem"))
        assertTrue(source.contains("{ AppIcon(CupertinoIcons.Filled.House, contentDescription = null) }"))
        assertTrue(source.contains("{ AppIcon(CupertinoIcons.Outlined.House, contentDescription = null) }"))
        assertTrue(source.contains("internal fun resolveMaterialBottomBarIcon("))
        assertTrue(source.contains("if (selected) Icons.Filled.Home else Icons.Outlined.Home"))
    }

    @Test
    fun `miuix auto floating bottom bar uses native Miuix icon pairs`() {
        val source = sourceText("BottomBar.kt")

        assertTrue(source.contains("SharedFloatingBottomBarIconStyle.MIUIX"))
        assertTrue(source.contains("BottomBarBlendedMiuixIcon("))
        assertTrue(source.contains("resolveHomeNavigationBarIcon(item, selected = false)"))
        assertTrue(source.contains("resolveHomeNavigationBarIcon(item, selected = true)"))
        assertTrue(source.contains("resolveMiuixPreferredHomeNavigationIcon(tabId = \"PARTITION\")"))
        assertFalse(source.contains("SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertFalse(source.contains("BottomBarBlendedCupertinoIcon("))
    }

    @Test
    fun `home header actions use Miuix search settings and messages icons`() {
        val source = sourceText("HomeHeader.kt")

        assertTrue(source.contains("val searchIcon = MiuixIcons.Search"))
        assertTrue(source.contains("val settingsIcon = MiuixIcons.Settings"))
        assertTrue(source.contains("val inboxIcon = MiuixIcons.Messages"))
    }

    private fun sourceText(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/home/components/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/home/components/$fileName"),
    ).first { it.exists() }.readText()
}
