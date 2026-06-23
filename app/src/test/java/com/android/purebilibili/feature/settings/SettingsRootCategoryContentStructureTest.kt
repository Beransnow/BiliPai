package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRootCategoryContentStructureTest {

    @Test
    fun rootCategoryContent_usesStateAndActionHolders() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("internal data class SettingsRootCategoryActions("))
        assertTrue(source.contains("internal data class SettingsRootCategoryState("))
        assertTrue(
            source.contains(
                """
                internal fun SettingsRootCategoryContent(
                    category: SettingsRootCategory,
                    actions: SettingsRootCategoryActions,
                    state: SettingsRootCategoryState
                )
                """.trimIndent()
            )
        )
    }

    @Test
    fun detailEntrySection_submitsDetailFocusBeforeOpeningEntry() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val sectionBlock = source
            .substringAfter("internal fun SettingsDetailEntrySection(")
            .substringBefore("internal fun SettingsRootCategoryContent(")

        assertTrue(sectionBlock.contains("resolveSettingsSceneDetailFocus(entry.target)?.let"))
        assertTrue(sectionBlock.contains("SettingsSearchFocusController.submit(detailFocus.target, detailFocus.focusId)"))
        assertTrue(sectionBlock.contains("entry.onClick()"))
        assertTrue(sectionBlock.contains("subtitle = entry.value"))
    }

    @Test
    fun feedSwitchDescription_allowsWrappingInIosSettings() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val feedSwitchBlock = source
            .substringAfter("private fun FeedSwitchItem(")
            .substringBefore("@Composable\nprivate fun FeedRefreshCountItem(")

        assertTrue(feedSwitchBlock.contains("text = subtitle"))
        assertTrue(!feedSwitchBlock.contains("maxLines = 1"))
    }

    @Test
    fun mobileSettingsRoot_doesNotRenderDuplicateCategoryHeaders() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
        ).first { it.exists() }.readText()

        val rootLoopBlock = source
            .substringAfter("sectionOrder.forEachIndexed { index, section ->")
            .substringBefore("item { Spacer(modifier = Modifier.height(16.dp)) }")

        assertFalse(rootLoopBlock.contains("SettingsCategoryHeader("))
        assertTrue(rootLoopBlock.contains("SettingsRootCategoryNavigationSection("))
        assertFalse(rootLoopBlock.contains("SettingsRootCategoryContent("))
    }

    @Test
    fun rootCategoryContent_usesStableDetailGroupsWithoutSceneShortcutRows() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val contentBlock = source
            .substringAfter("internal fun SettingsRootCategoryContent(")
            .substringBefore("@Composable\nfun SupportToolsSection(")

        assertTrue(contentBlock.contains("Column {\n        when (category)"))
        assertTrue(contentBlock.contains("SettingsDetailGroup("))
        assertTrue(contentBlock.contains("SettingsDetailEntrySection("))
        assertFalse(contentBlock.contains("SettingsSceneShortcutSection("))
        assertTrue(contentBlock.contains("SettingsRootCategory.DYNAMIC_RECOMMEND -> {"))
        assertTrue(contentBlock.contains("SettingsRootCategory.DATA_PRIVACY -> {"))
        assertTrue(contentBlock.contains("SettingsRootCategory.EXTENSION_ABOUT -> {"))
    }

    @Test
    fun mobileSettingsRootPinsSearchAboveSupportAndCategories() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
        ).first { it.exists() }.readText()

        val rootListBlock = source
            .substringAfter("LazyColumn(")
            .substringBefore("sectionOrder.forEachIndexed")

        assertTrue(rootListBlock.contains("SettingsSearchBarSection("))
        assertTrue(rootListBlock.contains("SupportAuthorCompactSection("))
        assertTrue(rootListBlock.contains("activeRootCategory == null"))
        assertTrue(rootListBlock.indexOf("SettingsSearchBarSection(") < rootListBlock.indexOf("SupportAuthorCompactSection("))
        assertFalse(rootListBlock.contains("FollowAuthorSection("))
    }

    @Test
    fun mobileSettingsRootUsesNavigationCategorySections() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val sectionBlock = source
            .substringAfter("internal fun SettingsRootCategoryNavigationSection(")
            .substringBefore("@Composable\ninternal fun SettingsDetailGroup(")

        assertTrue(sectionBlock.contains("text = category.title"))
        assertTrue(sectionBlock.contains("text = category.subtitle"))
        assertTrue(sectionBlock.contains("CupertinoIcons.Default.ChevronForward"))
        assertFalse(sectionBlock.contains("AnimatedVisibility("))
        assertFalse(sectionBlock.contains("SettingsRootCategoryContent("))
    }

    @Test
    fun mobileSettingsRootUsesCategoryDetailScreenInsteadOfInlineExpansion() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("activeRootCategoryName"))
        assertTrue(source.contains("SettingsRootCategoryDetailLayout("))
        assertFalse(source.contains("expandedRootCategoryNames"))
    }

    @Test
    fun tabletSettingsRootUsesCompactSupportInMasterAndKeepsDetailFocused() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/TabletSettingsLayout.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/TabletSettingsLayout.kt")
        ).first { it.exists() }.readText()

        val masterBlock = source
            .substringAfter("// Master List")
            .substringBefore("secondaryContent =")
        val rootDetailBlock = source
            .substringAfter("// Category Root")
            .substringBefore("Spacer(modifier = Modifier\n                                .windowInsetsBottomHeight")

        assertTrue(masterBlock.contains("SettingsSearchBarSection("))
        assertTrue(masterBlock.contains("SupportAuthorCompactSection("))
        assertTrue(masterBlock.indexOf("SettingsSearchBarSection(") < masterBlock.indexOf("SupportAuthorCompactSection("))
        assertFalse(rootDetailBlock.contains("FollowAuthorSection("))
        assertTrue(rootDetailBlock.contains("SettingsRootCategoryContent("))
    }

    @Test
    fun aboutSupport_keepsReleaseChannelBelowAboutDetailsWithoutDuplicateAuthorCard() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val aboutBlock = source
            .substringAfter("SettingsRootCategory.EXTENSION_ABOUT -> {")
            .substringBefore("SupportToolsSection(")

        assertTrue(aboutBlock.indexOf("AboutSection(") < aboutBlock.indexOf("ReleaseChannelPinnedCard("))
        assertFalse(aboutBlock.contains("FollowAuthorSection("))
    }

    @Test
    fun aboutSectionShowsProjectOverviewAndStaticContributorsBeforeRows() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val aboutSectionBlock = source
            .substringAfter("fun AboutSection(")
            .substringBefore("@Composable\nprivate fun AboutProjectOverviewCard(")

        assertTrue(aboutSectionBlock.contains("AboutProjectOverviewCard(versionName = versionName)"))
        assertTrue(aboutSectionBlock.indexOf("AboutProjectOverviewCard(") < aboutSectionBlock.indexOf("SettingsCardGroup {"))
        assertTrue(source.contains("internal val AboutContributors = listOf("))
        assertTrue(source.contains("AboutContributor(\"Chenx Dust\""))
        assertTrue(source.contains("AboutContributor(\"usontong\""))
    }

    @Test
    fun aboutSection_doesNotRenderDuplicateReleaseChannelDisclaimerEntry() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val aboutSectionBlock = source
            .substringAfter("fun AboutSection(")
            .substringBefore("@Composable\nfun CheckUpdateSection(")

        assertFalse(aboutSectionBlock.contains("title = \"发布渠道声明\""))
        assertFalse(aboutSectionBlock.contains("SettingsSearchTarget.DISCLAIMER"))
    }

    @Test
    fun releaseChannelPinnedCard_keepsActionsInOneLine() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val pinnedCardBlock = source
            .substringAfter("fun ReleaseChannelPinnedCard(")
            .substringBefore("@Composable\nfun SettingsSubpageEntrySection(")

        assertTrue(pinnedCardBlock.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(pinnedCardBlock.contains("modifier = Modifier.weight(1f)"))
        assertTrue(pinnedCardBlock.contains("softWrap = false"))
        assertTrue(pinnedCardBlock.contains("maxLines = 1"))
    }
}
