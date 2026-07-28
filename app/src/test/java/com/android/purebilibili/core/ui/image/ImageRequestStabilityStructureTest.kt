package com.android.purebilibili.core.ui.image

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageRequestStabilityStructureTest {

    @Test
    fun `non live image dense surfaces use remembered request models`() {
        val sources = listOf(
            "feature/home/components/cards/StoryVideoCard.kt",
            "feature/home/components/cards/GlassVideoCard.kt",
            "feature/home/components/cards/CinematicVideoCard.kt",
            "feature/dynamic/DynamicScreen.kt",
            "feature/dynamic/components/DrawGrid.kt",
            "feature/dynamic/components/DynamicSidebar.kt",
            "feature/dynamic/components/VideoCards.kt",
            "feature/space/SpaceScreen.kt",
            "feature/settings/screen/AppearanceSettingsScreen.kt",
            "feature/video/ui/components/EmotePanelSheet.kt",
            "feature/video/ui/components/SubReplyDetailComponents.kt",
        ).associateWith(::mainSource)

        sources.forEach { (path, source) ->
            assertTrue(source.contains("rememberImageRequest("), "$path must remember image requests")
            assertTrue(!source.contains("model = ImageRequest.Builder("), "$path rebuilds a request in composition")
            assertTrue(!source.contains("model = coil.request.ImageRequest.Builder("), "$path rebuilds a request in composition")
        }
    }

    @Test
    fun `search remembers non live models without migrating live cards`() {
        val source = mainSource("feature/search/SearchScreen.kt")
        val beforeLiveResults = source.substringAfter("fun SearchResultCard(")
            .substringBefore("internal fun LiveSearchResultCard(")
        val liveResults = source.substringAfter("internal fun LiveSearchResultCard(")
            .substringBefore("internal fun TopicSearchResultCard(")
        val afterLiveResults = source.substringAfter("internal fun TopicSearchResultCard(")

        listOf(beforeLiveResults, afterLiveResults).forEach { nonLiveSource ->
            assertTrue(nonLiveSource.contains("rememberImageRequest("))
            assertTrue(!nonLiveSource.contains("model = ImageRequest.Builder("))
        }
        assertTrue(liveResults.contains("model = ImageRequest.Builder("))
    }

    @Test
    fun `dynamic and comment inline content cache their internal requests`() {
        val dynamicSource = mainSource("feature/dynamic/components/DynamicCard.kt")
        assertTrue(dynamicSource.contains("val emojiImageRequests = remember(context, emojiNodes)"))
        assertTrue(dynamicSource.contains("val inlineContent = remember(emojiImageRequests)"))
        assertTrue(dynamicSource.contains("model = imageRequest"))

        val replySource = mainSource("feature/video/ui/components/ReplyComponents.kt")
        assertTrue(replySource.contains("val emoteImageRequests = remember(context, renderableEmoteKeys, emoteMap)"))
        assertTrue(replySource.contains("val prefixIconImageRequests = remember(context, content?.urls)"))
        assertTrue(replySource.contains("emoteImageRequests.forEach { (key, imageRequest) ->"))
        assertTrue(replySource.contains("prefixIconImageRequests.forEach { (inlineId, imageRequest) ->"))
    }

    @Test
    fun `comment decoration fallback participates in request identity`() {
        val source = mainSource("feature/video/ui/components/ReplyComponents.kt")
        val requestBlock = source.substringAfter("val decorationImageRequest = remember(")
            .substringBefore(".build()")

        assertTrue(requestBlock.contains("imageUrl"))
        assertTrue(requestBlock.contains("fallbackImageUrl"))
        assertTrue(requestBlock.contains("crossfadeEnabled"))
        assertTrue(source.contains("model = decorationImageRequest"))
    }

    private fun mainSource(relativePath: String): String =
        File("src/main/java/com/android/purebilibili/$relativePath").readText()
}
