package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoCardImageDecodeStrategyStructureTest {

    @Test
    fun `tiered home covers forward HomeCoverRequestSpec while unconstrained styles stay layout sized`() {
        val sourceRoot = File("src/main/java/com/android/purebilibili/feature/home/components/cards")
        val videoCard = sourceRoot.resolve("VideoCard.kt").readText()
        val storyCard = sourceRoot.resolve("StoryVideoCard.kt").readText()
        val glassCard = sourceRoot.resolve("GlassVideoCard.kt").readText()
        val cinematicCard = sourceRoot.resolve("CinematicVideoCard.kt").readText()

        listOf(videoCard, storyCard).forEach { source ->
            assertTrue(source.contains("widthPx = coverRequestSpec?.widthPx"))
            assertTrue(source.contains("heightPx = coverRequestSpec?.heightPx"))
        }
        listOf(glassCard, cinematicCard).forEach { source ->
            val coverRequest = source.substringAfter("model = rememberImageRequest(\n")
                .substringBefore("),\n")
            assertTrue(!coverRequest.contains("widthPx ="))
            assertTrue(!coverRequest.contains("heightPx ="))
        }
    }

    @Test
    fun `home image models survive unrelated recomposition`() {
        val sourceRoot = File("src/main/java/com/android/purebilibili/feature/home/components/cards")
        val cardSources = listOf(
            "VideoCard.kt",
            "StoryVideoCard.kt",
            "GlassVideoCard.kt",
            "CinematicVideoCard.kt"
        ).map { sourceRoot.resolve(it).readText() }
        val requestHelper = File(
            "src/main/java/com/android/purebilibili/core/ui/image/RememberedImageRequest.kt"
        ).readText()

        cardSources.forEach { source ->
            assertTrue(source.contains("rememberImageRequest("))
            assertTrue(!source.contains("model = ImageRequest.Builder("))
        }
        listOf(
            "data",
            "widthPx",
            "heightPx",
            "referer",
            "crossfadeEnabled",
            "crossfadeMillis",
            "placeholderMemoryCacheKey",
            "memoryCacheKey",
            "diskCacheKey",
            "scale",
        ).forEach { requestKey ->
            assertTrue(requestHelper.contains("        $requestKey,"), "$requestKey must be a remember key")
        }
    }
}
