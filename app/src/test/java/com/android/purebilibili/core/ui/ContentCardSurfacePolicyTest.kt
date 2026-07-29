package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.UiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentCardSurfacePolicyTest {

    @Test
    fun miuixContentCardsUseTokenSurfaceAndFlatElevation() {
        val spec = resolveContentCardSurfaceSpec(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        assertTrue(spec.usesTonalContainerTreatment)
        assertEquals(ContainerLevel.Card, spec.cornerLevel)
        assertEquals(0.8f, spec.borderWidthDp)
        assertEquals(0.22f, spec.borderAlpha)
        assertEquals(0f, spec.tonalElevationDp)
        assertEquals(0f, spec.shadowElevationDp)
    }

    @Test
    fun materialContentCardsKeepLegacyGlassShellDefaults() {
        val spec = resolveContentCardSurfaceSpec(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        assertFalse(spec.usesTonalContainerTreatment)
        assertEquals(0f, spec.borderWidthDp)
    }

    @Test
    fun appCardRoutesMiuixToNativeCardAndOtherStylesToMaterialSurface() {
        UiStyle.entries.forEach { uiStyle ->
            val spec = resolveAppCardVisualSpec(uiStyle, AppCardTone.STANDARD)
            val expected = if (uiStyle == UiStyle.MIUIX) {
                AppCardRenderer.MIUIX_CARD
            } else {
                AppCardRenderer.MATERIAL_SURFACE
            }
            assertEquals(expected, spec.renderer)
        }
    }

    @Test
    fun appCardTonesKeepSemanticSurfaceAndElevationContracts() {
        val standard = resolveAppCardVisualSpec(UiStyle.MATERIAL3, AppCardTone.STANDARD)
        val muted = resolveAppCardVisualSpec(UiStyle.MATERIAL3, AppCardTone.MUTED)
        val glass = resolveAppCardVisualSpec(UiStyle.IOS, AppCardTone.GLASS)

        assertEquals(AppCardContainerRole.CARD, standard.containerRole)
        assertEquals(1f, standard.borderWidthDp)
        assertEquals(AppCardContainerRole.SURFACE_VARIANT, muted.containerRole)
        assertEquals(0.42f, muted.containerAlpha)
        assertEquals(AppCardContainerRole.SURFACE, glass.containerRole)
        assertEquals(0.6f, glass.containerAlpha)
        assertEquals(0f, glass.tonalElevationDp)
        assertEquals(0f, glass.shadowElevationDp)
    }

    @Test
    fun migratedFeatureCardsUseNeutralAppCardWithoutStyleLocals() {
        val messageSource = load("app/src/main/java/com/android/purebilibili/feature/message/feed/MessageFeedCommon.kt")
        val dynamicSource = load(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicComponents.kt"
        )
        val liveSource = load("app/src/main/java/com/android/purebilibili/feature/live/LiveRoomCard.kt")

        assertTrue(messageSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(messageSource.contains("AppShapes.borderedContainer("))
        assertTrue(searchSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(searchSource.contains("AppShapes.borderedContainer("))
        assertTrue(dynamicSource.contains("rememberContentCardSurfaceSpec("))
        assertTrue(dynamicSource.contains("AppShapes.borderedContainer("))
    }

    private fun load(path: String): String {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized))
            .first { it.exists() }
            .readText()
    }
}
