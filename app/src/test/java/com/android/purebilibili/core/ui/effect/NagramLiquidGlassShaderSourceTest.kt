package com.android.purebilibili.core.ui.effect

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NagramLiquidGlassShaderSourceTest {

    @Test
    fun `shader source is copied from nagramx without bilipai-only uniforms`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/effect/LiquidGlassShader.kt"
        )
        val thirdPartyNotice = loadSource(
            "docs/third_party/NagramX-LiquidGlass.md"
        )

        assertTrue(source.contains("risin42/NagramX"))
        assertTrue(source.contains("TMessagesProj/src/main/res/raw/liquid_glass_shader.agsl"))
        assertTrue(source.contains("GPL-3.0"))
        assertTrue(thirdPartyNotice.contains("risin42/NagramX"))
        assertTrue(thirdPartyNotice.contains("GPL-3.0"))
        assertTrue(thirdPartyNotice.contains("liquid_glass_shader.agsl"))
        assertTrue(source.contains("half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));"))
        assertTrue(source.contains("return srcOver(half4(foreground_color_premultiplied), img.eval(uv));"))
        assertFalse(source.contains("aberration_strength"))
        assertFalse(source.contains("specular_alpha"))
        assertFalse(source.contains("light_dir"))
        assertFalse(source.contains("background_color"))
    }

    @Test
    fun `transparent bottom bar branch applies copied nagramx shader modifier`() {
        val bottomBarSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        )
        val transparentPresetSource = bottomBarSource
            .substringAfter("BottomBarLiquidGlassPreset.BACKDROP_NATIVE -> drawBackdrop(")
            .substringBefore("BottomBarLiquidGlassPreset.BILIPAI_TUNED -> drawBackdrop(")
        val modifierSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/effect/LiquidGlassModifier.kt"
        )

        assertTrue(transparentPresetSource.contains(".nagramLiquidGlass("))
        assertTrue(modifierSource.contains("fun Modifier.nagramLiquidGlass("))
        assertFalse(modifierSource.contains("setFloatUniform(\"aberration_strength\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"specular_alpha\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"light_dir\""))
        assertFalse(modifierSource.contains("setFloatUniform(\"background_color\""))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
            File("../$path")
        ).firstOrNull { it.exists() } ?: error("Source file not found: $path")
        return sourceFile.readText()
    }
}
