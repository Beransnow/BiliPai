package com.android.purebilibili.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.UiStyle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.ChartBar
import io.github.alexzhirkevich.cupertino.icons.outlined.Cpu
import io.github.alexzhirkevich.cupertino.icons.outlined.Grid
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircleBadgePlus
import io.github.alexzhirkevich.cupertino.icons.outlined.RectangleStack
import io.github.alexzhirkevich.cupertino.icons.outlined.Star
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarSettingsScreenIconPolicyTest {

    @Test
    fun bottomBarIconPolicy_usesSemanticIconsForSecondaryTabs() {
        assertSameVectorAsset(CupertinoIcons.Outlined.RectangleStack, resolveBottomBarTabIcon("DYNAMIC", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Star, resolveBottomBarTabIcon("FAVORITE", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Clock, resolveBottomBarTabIcon("WATCHLATER", UiStyle.IOS))
    }

    @Test
    fun bottomBarIconPolicy_usesMaterialIconsForMd3Preset() {
        assertSameVectorAsset(Icons.Outlined.Home, resolveBottomBarTabIcon("HOME", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.DynamicFeed, resolveBottomBarTabIcon("DYNAMIC", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.StarBorder, resolveBottomBarTabIcon("FAVORITE", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.WatchLater, resolveBottomBarTabIcon("WATCHLATER", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.LiveTv, resolveBottomBarTabIcon("LIVE", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.Settings, resolveBottomBarTabIcon("SETTINGS", UiStyle.MATERIAL3))
    }

    @Test
    fun topTabIconPolicy_usesSemanticIconsForContentCategories() {
        assertSameVectorAsset(CupertinoIcons.Outlined.PersonCropCircleBadgePlus, resolveTopTabIcon("FOLLOW", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.ChartBar, resolveTopTabIcon("POPULAR", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Grid, resolveTopTabIcon("PARTITION", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Lightbulb, resolveTopTabIcon("KNOWLEDGE", UiStyle.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Cpu, resolveTopTabIcon("TECH", UiStyle.IOS))
    }

    @Test
    fun topTabIconPolicy_usesMaterialIconsForMd3Preset() {
        assertSameVectorAsset(Icons.Outlined.Person, resolveTopTabIcon("FOLLOW", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.AutoMirrored.Outlined.TrendingUp, resolveTopTabIcon("POPULAR", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.GridView, resolveTopTabIcon("PARTITION", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.Lightbulb, resolveTopTabIcon("KNOWLEDGE", UiStyle.MATERIAL3))
        assertSameVectorAsset(Icons.Outlined.SmartToy, resolveTopTabIcon("TECH", UiStyle.MATERIAL3))
    }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }
}
