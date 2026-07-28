package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhaseThreeIconBoundaryStructureTest {

    @Test
    fun directGlyphScannerIgnoresCommentsAndLiteralsButHandlesLineBreaksAndAliases() {
        val source = """
            // Icons.Outlined.Search
            val example = "CupertinoIcons.Default.Xmark"
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Icon(CupertinoIcons.Default.Link, contentDescription = null)
            Icon(
                Icons
                    .Outlined
                    .Search,
                contentDescription = null,
            )
            import androidx.compose.material.icons.Icons as MaterialIcons
        """.trimIndent()

        assertEquals(
            listOf("Icons.Outlined.Delete", "CupertinoIcons.Default.Link", "Icons.Outlined.Search"),
            findDirectGlyphs(source).toList(),
        )
        assertEquals(listOf("MaterialIcons"), findAliasedIconReceivers(source).toList())
    }

    @Test
    fun phaseThreeOrdinaryFeaturesKeepOnlyTheReviewedDirectGlyphDebt() {
        val sourceRoot = resolveMainPackageRoot()
        val sourceFiles = phaseThreeSourceFiles(sourceRoot).toList()
        val aliases = sourceFiles.flatMap { file ->
            findAliasedIconReceivers(file.readText()).map { alias ->
                "${file.relativeTo(sourceRoot).invariantSeparatorsPath}: $alias"
            }.toList()
        }
        assertTrue(
            aliases.isEmpty(),
            "Aliasing an Icons/CupertinoIcons receiver bypasses the direct-glyph audit:\n" +
                aliases.joinToString(separator = "\n"),
        )

        val actual = sourceFiles.asSequence()
            .flatMap { file ->
                val relativePath = file.relativeTo(sourceRoot).invariantSeparatorsPath
                findDirectGlyphs(file.readText()).map { glyph -> DirectGlyphDebt(relativePath, glyph) }
            }
            .groupingBy { it }
            .eachCount()

        assertEquals(
            REVIEWED_DIRECT_GLYPH_DEBT,
            actual,
            "Direct platform icons in phase 3 must stay on the reviewed semantic-debt list; " +
                "migrate shared meanings through rememberApp*Icon and shrink this list deliberately.",
        )
        assertEquals(31, actual.values.sum(), "The C10 first-batch direct-glyph baseline must not grow.")
    }

    @Test
    fun closeAndCirclePlaySitesUseExactSemanticFacades() {
        val sourceRoot = resolveMainPackageRoot()

        CLOSE_CALLS.forEach { (relativePath, expectedCount) ->
            val source = maskCommentsAndLiterals(sourceRoot.resolve(relativePath).readText())
            assertEquals(expectedCount, countCall(source, "rememberAppCloseIcon"), relativePath)
            assertEquals(
                0,
                countCall(source, "rememberAppClearIcon"),
                "$relativePath must not confuse dismiss/close with clearing input.",
            )
        }

        val spaceSource = maskCommentsAndLiterals(sourceRoot.resolve("feature/space/SpaceScreen.kt").readText())
        assertEquals(4, countCall(spaceSource, "rememberAppPlayCircleIcon"))
        assertEquals(0, countCall(spaceSource, "rememberAppPlayIcon"))

        val videoCardsSource = maskCommentsAndLiterals(
            sourceRoot.resolve("feature/dynamic/components/VideoCards.kt").readText(),
        )
        assertEquals(1, countCall(videoCardsSource, "rememberAppPlayCircleFilledIcon"))
        assertEquals(0, countCall(videoCardsSource, "rememberAppPlayIcon"))
    }

    private fun findDirectGlyphs(source: String): Sequence<String> {
        return DIRECT_GLYPH.findAll(maskCommentsAndLiterals(source)).map { match ->
            DOT_WHITESPACE.replace(match.value, ".")
        }
    }

    private fun findAliasedIconReceivers(source: String): Sequence<String> {
        return ICON_RECEIVER_ALIAS.findAll(maskCommentsAndLiterals(source)).map { match ->
            match.groupValues[1]
        }
    }

    private fun countCall(source: String, name: String): Int {
        return Regex("""\b${Regex.escape(name)}\s*\(""").findAll(source).count()
    }

    /** Keeps newlines stable while excluding examples and names inside comments or literals. */
    private fun maskCommentsAndLiterals(source: String): String {
        val masked = source.toCharArray()
        var index = 0

        fun mask(position: Int) {
            if (masked[position] != '\n' && masked[position] != '\r') masked[position] = ' '
        }

        while (index < source.length) {
            when {
                source.startsWith("//", index) -> {
                    while (index < source.length && source[index] != '\n') {
                        mask(index)
                        index += 1
                    }
                }

                source.startsWith("/*", index) -> {
                    var depth = 0
                    while (index < source.length) {
                        when {
                            source.startsWith("/*", index) -> {
                                depth += 1
                                mask(index)
                                if (index + 1 < source.length) mask(index + 1)
                                index += 2
                            }

                            source.startsWith("*/", index) -> {
                                depth -= 1
                                mask(index)
                                if (index + 1 < source.length) mask(index + 1)
                                index += 2
                                if (depth == 0) break
                            }

                            else -> {
                                mask(index)
                                index += 1
                            }
                        }
                    }
                }

                source.startsWith("\"\"\"", index) -> {
                    repeat(3) { offset -> mask(index + offset) }
                    index += 3
                    while (index < source.length && !source.startsWith("\"\"\"", index)) {
                        mask(index)
                        index += 1
                    }
                    repeat(3) { offset ->
                        if (index + offset < source.length) mask(index + offset)
                    }
                    index = (index + 3).coerceAtMost(source.length)
                }

                source[index] == '"' || source[index] == '\'' -> {
                    val delimiter = source[index]
                    mask(index)
                    index += 1
                    var escaped = false
                    while (index < source.length) {
                        val character = source[index]
                        mask(index)
                        index += 1
                        if (!escaped && character == delimiter) break
                        escaped = !escaped && character == '\\'
                    }
                }

                else -> index += 1
            }
        }
        return masked.concatToString()
    }

    private fun phaseThreeSourceFiles(sourceRoot: File): Sequence<File> {
        return PHASE_THREE_FEATURE_ROOTS
            .asSequence()
            .flatMap { relativeRoot ->
                sourceRoot.resolve(relativeRoot)
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
            }
            .filterNot { file ->
                val relativePath = file.relativeTo(sourceRoot).invariantSeparatorsPath
                relativePath == "feature/live/LivePlayerScreen.kt" ||
                    relativePath.startsWith("feature/live/components/")
            }
    }

    private fun resolveMainPackageRoot(): File {
        return listOf(
            File("app/src/main/java/com/android/purebilibili"),
            File("src/main/java/com/android/purebilibili"),
        ).firstOrNull(File::isDirectory)
            ?: error("Cannot locate app main package root from ${File(".").absolutePath}")
    }

    private data class DirectGlyphDebt(
        val relativePath: String,
        val glyph: String,
    )

    private companion object {
        val DIRECT_GLYPH = Regex(
            """\b(?:Icons|CupertinoIcons)(?:\s*\.\s*[A-Za-z][A-Za-z0-9_]*){2,}""",
        )
        val DOT_WHITESPACE = Regex("""\s*\.\s*""")
        val ICON_RECEIVER_ALIAS = Regex(
            """(?m)^\s*import\s+(?:androidx\.compose\.material\.icons\.Icons|io\.github\.alexzhirkevich\.cupertino\.icons\.CupertinoIcons)(?:\.[A-Za-z][A-Za-z0-9_]*)*\s+as\s+([A-Za-z][A-Za-z0-9_]*)""",
        )
        val PHASE_THREE_FEATURE_ROOTS = listOf(
            "feature/dynamic",
            "feature/list",
            "feature/live",
            "feature/message",
            "feature/partition",
            "feature/profile",
            "feature/search",
            "feature/space",
        )
        val CLOSE_CALLS = mapOf(
            "feature/dynamic/components/DynamicCommentSheet.kt" to 1,
            "feature/dynamic/components/ImagePreviewDialog.kt" to 2,
            "feature/profile/OfficialWallpaperSheet.kt" to 1,
            "feature/profile/SplashWallpaperPickerSheet.kt" to 1,
            "feature/space/SpaceScreen.kt" to 1,
        )
        val REVIEWED_DIRECT_GLYPH_DEBT = mapOf(
            DirectGlyphDebt("feature/dynamic/components/DrawGrid.kt", "CupertinoIcons.Default.Star") to 1,
            DirectGlyphDebt("feature/dynamic/components/DynamicCard.kt", "CupertinoIcons.Default.Link") to 1,
            DirectGlyphDebt("feature/dynamic/components/DynamicCard.kt", "CupertinoIcons.Default.Trash") to 2,
            DirectGlyphDebt("feature/dynamic/components/DynamicOpusLinkCard.kt", "CupertinoIcons.Default.Link") to 1,
            DirectGlyphDebt("feature/dynamic/components/DynamicTopBar.kt", "CupertinoIcons.Default.ListBullet") to 1,
            DirectGlyphDebt("feature/dynamic/components/DynamicTopBar.kt", "CupertinoIcons.Default.RectangleStack") to 1,
            DirectGlyphDebt("feature/list/CommonListScreen.kt", "Icons.Rounded.CheckCircle") to 1,
            DirectGlyphDebt("feature/list/CommonListScreen.kt", "Icons.Rounded.RadioButtonUnchecked") to 1,
            DirectGlyphDebt("feature/live/LiveAreaScreen.kt", "Icons.Outlined.Star") to 1,
            DirectGlyphDebt("feature/live/LiveAreaScreen.kt", "Icons.Outlined.StarBorder") to 2,
            DirectGlyphDebt("feature/message/ChatScreen.kt", "Icons.AutoMirrored.Filled.Send") to 1,
            DirectGlyphDebt("feature/profile/OfficialWallpaperSheet.kt", "CupertinoIcons.Default.CheckmarkCircle") to 1,
            DirectGlyphDebt("feature/profile/ProfileScreen.kt", "CupertinoIcons.Default.Link") to 1,
            DirectGlyphDebt("feature/profile/ProfileScreen.kt", "CupertinoIcons.Default.Trash") to 2,
            DirectGlyphDebt("feature/profile/SplashWallpaperPickerSheet.kt", "CupertinoIcons.Default.CheckmarkCircle") to 2,
            DirectGlyphDebt("feature/profile/WallpaperAdjustmentSheet.kt", "Icons.Outlined.PhoneAndroid") to 2,
            DirectGlyphDebt("feature/profile/WallpaperAdjustmentSheet.kt", "Icons.Outlined.TabletAndroid") to 2,
            DirectGlyphDebt("feature/search/SearchTrendingScreen.kt", "Icons.Rounded.North") to 1,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.AutoMirrored.Outlined.Sort") to 2,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.Outlined.Bolt") to 1,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.Outlined.ContentCopy") to 1,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.Outlined.GridView") to 1,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.Outlined.Menu") to 1,
            DirectGlyphDebt("feature/space/SpaceScreen.kt", "Icons.Outlined.ViewAgenda") to 1,
        )
    }
}
