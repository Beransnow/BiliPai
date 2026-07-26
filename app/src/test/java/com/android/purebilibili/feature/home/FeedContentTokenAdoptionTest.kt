package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FeedContentTokenAdoptionTest {
    private val cardFiles = listOf(
        "CinematicVideoCard.kt",
        "GlassVideoCard.kt",
        "HomeStyleSingleColumnVideoCard.kt",
        "StoryVideoCard.kt",
        "VideoCard.kt",
    )

    @Test
    fun feedCardCompositions_readSharedSemanticTypography() {
        val sources = cardFiles.associateWith { name ->
            locate("src/main/java/com/android/purebilibili/feature/home/components/cards/$name")
                .readText()
        }

        sources.forEach { (name, source) ->
            assertTrue(source.contains("feedContentTypography()"), "$name 未读取共享信息流排版")
            listOf("title", "author", "statistic", "coverBadge").forEach { role ->
                assertTrue(
                    source.contains("contentTypography.$role"),
                    "$name 没有使用信息流排版角色 $role",
                )
            }
        }

        val sharedRowSource = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt",
        ).readText()
        assertTrue(!sharedRowSource.contains("topSpacingDp"), "卡片间距不能通过裸整数绕过 token 扫描")
        assertTrue(sharedRowSource.contains("topSpacing: Dp"), "共享元数据行应接收已解析的 Dp")
    }

    private fun locate(path: String): File = listOf(File(path), File("app/$path"))
        .firstOrNull(File::exists) ?: error("Cannot locate $path")
}
