package com.android.purebilibili.feature.anime4k

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoEnhancementRememberWarningStructureTest {

    @Test
    fun `跨视频记忆提醒区分记忆功能与实际增强开销`() {
        val source = pluginSource()

        assertTrue(source.contains("开启记忆本身不会占用性能"))
        assertTrue(source.contains("避免之后忘记关闭"))
        assertTrue(source.contains("较高分辨率视频"))
        assertTrue(source.contains("HDR 等自动旁路场景不会执行增强"))
        assertFalse(source.contains("持续开启可能影响性能"))
    }

    private fun pluginSource(): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/plugin/Anime4KPlugin.kt"),
        File("src/main/java/com/android/purebilibili/feature/plugin/Anime4KPlugin.kt")
    ).first { it.exists() }.readText()
}
