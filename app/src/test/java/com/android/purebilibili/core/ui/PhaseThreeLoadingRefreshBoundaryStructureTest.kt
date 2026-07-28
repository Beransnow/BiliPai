package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhaseThreeLoadingRefreshBoundaryStructureTest {

    @Test
    fun rendererScannerAllowsOnlyExplicitMaterialProgress() {
        val sample = """
            AdaptiveLoadingIndicator()
            LoadingIndicator()
            PullToRefreshBox()
            contentModifier.pullToRefresh()
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            CircularProgressIndicator(progress = { progressFraction })
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progressFraction },
            )
        """.trimIndent()

        assertEquals(
            listOf(
                "AdaptiveLoadingIndicator",
                "LoadingIndicator",
                "PullToRefreshBox",
                "pullToRefresh",
                "CircularProgressIndicator",
                "LinearProgressIndicator",
            ),
            findForbiddenRendererCalls(sample).map(RendererCall::name).toList(),
        )
    }

    @Test
    fun phaseThreeOrdinaryFeaturesRejectLegacyAndIndeterminateLoadingRenderers() {
        val sourceRoot = resolveMainPackageRoot()
        val offenders = phaseThreeSourceFiles(sourceRoot)
            .flatMap { file ->
                val source = file.readText()
                findForbiddenRendererCalls(source).map { call ->
                    val relativePath = file.relativeTo(sourceRoot).invariantSeparatorsPath
                    "$relativePath:${call.line}: ${call.name}"
                }
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            buildString {
                appendLine("Phase 3 ordinary features must use neutral App loading/refresh entry points.")
                appendLine("Direct Circular/Linear indicators are allowed only for explicit determinate progress (progress = ...).")
                append(offenders.joinToString(separator = "\n"))
            },
        )
    }

    @Test
    fun phaseThreeNeutralCallsStayAtTheMigratedBaseline() {
        val sourceRoot = resolveMainPackageRoot()
        val source = phaseThreeSourceFiles(sourceRoot)
            .joinToString(separator = "\n") { file -> maskCommentsAndLiterals(file.readText()) }

        assertEquals(
            44,
            Regex("""\bAppLoadingIndicator\s*\(""").findAll(source).count(),
            "Phase 3 loading calls changed; audit removed or added loading states.",
        )
        assertEquals(
            9,
            Regex("""\bAppPullToRefreshBox\s*\(""").findAll(source).count(),
            "Phase 3 refresh calls changed; audit removed or added refresh hosts.",
        )
        assertEquals(
            2,
            Regex("""\bLinearProgressIndicator\s*\(""").findAll(source).count(),
            "The two explicit Space watch-progress indicators must remain determinate.",
        )
    }

    private fun findForbiddenRendererCalls(source: String): Sequence<RendererCall> {
        val code = maskCommentsAndLiterals(source)
        return RENDERER_CALL.findAll(code).mapNotNull { match ->
            val name = match.groupValues[1]
            val openParenthesis = code.indexOf('(', startIndex = match.range.first + name.length)
            val closeParenthesis = findMatchingParenthesis(code, openParenthesis)
            val hasExplicitProgress = closeParenthesis != null && hasTopLevelProgressArgument(
                code.substring(openParenthesis + 1, closeParenthesis),
            )
            val isForbidden = name in LEGACY_RENDERERS ||
                name in RAW_INDETERMINATE_RENDERERS ||
                name in RAW_REFRESH_RENDERERS ||
                (name in MATERIAL_PROGRESS_RENDERERS && !hasExplicitProgress)

            if (isForbidden) {
                RendererCall(
                    name = name,
                    line = code.take(match.range.first).count { it == '\n' } + 1,
                )
            } else {
                null
            }
        } + MATERIAL_PULL_REFRESH_IMPORT.findAll(code).map { match ->
            RendererCall(
                name = "androidx.compose.material3.pulltorefresh import",
                line = code.take(match.range.first).count { it == '\n' } + 1,
            )
        }
    }

    private fun findMatchingParenthesis(code: String, openParenthesis: Int): Int? {
        if (openParenthesis < 0) return null
        var depth = 0
        for (index in openParenthesis until code.length) {
            when (code[index]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun hasTopLevelProgressArgument(arguments: String): Boolean {
        var parenthesisDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        var index = 0

        while (index < arguments.length) {
            when (arguments[index]) {
                '(' -> parenthesisDepth += 1
                ')' -> parenthesisDepth -= 1
                '[' -> bracketDepth += 1
                ']' -> bracketDepth -= 1
                '{' -> braceDepth += 1
                '}' -> braceDepth -= 1
            }

            if (
                parenthesisDepth == 0 &&
                bracketDepth == 0 &&
                braceDepth == 0 &&
                arguments.regionMatches(index, "progress", 0, "progress".length) &&
                (index == 0 || !Character.isJavaIdentifierPart(arguments[index - 1])) &&
                (index + "progress".length == arguments.length ||
                    !Character.isJavaIdentifierPart(arguments[index + "progress".length]))
            ) {
                var equalsIndex = index + "progress".length
                while (equalsIndex < arguments.length && arguments[equalsIndex].isWhitespace()) {
                    equalsIndex += 1
                }
                if (equalsIndex < arguments.length && arguments[equalsIndex] == '=') return true
            }
            index += 1
        }
        return false
    }

    /** Keeps source offsets/newlines stable while excluding names inside comments and literals. */
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

    private fun isStageFourLivePlayerSource(relativePath: String): Boolean {
        // This package is currently mounted only by the stage 4 live-player surface.
        return relativePath == "feature/live/LivePlayerScreen.kt" ||
            relativePath.startsWith("feature/live/components/")
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
                isStageFourLivePlayerSource(file.relativeTo(sourceRoot).invariantSeparatorsPath)
            }
    }

    private fun resolveMainPackageRoot(): File {
        return listOf(
            File("app/src/main/java/com/android/purebilibili"),
            File("src/main/java/com/android/purebilibili"),
        ).firstOrNull(File::isDirectory)
            ?: error("Cannot locate app main package root from ${File(".").absolutePath}")
    }

    private data class RendererCall(
        val name: String,
        val line: Int,
    )

    private companion object {
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
        val LEGACY_RENDERERS = setOf(
            "AdaptiveLoadingIndicator",
            "CutePersonLoadingIndicator",
            "AdaptivePullToRefreshBox",
        )
        val MATERIAL_PROGRESS_RENDERERS = setOf(
            "CircularProgressIndicator",
            "LinearProgressIndicator",
        )
        val RAW_INDETERMINATE_RENDERERS = setOf(
            "LoadingIndicator",
            "InfiniteProgressIndicator",
            "MiuixCircularProgressIndicator",
            "MiuixInfiniteProgressIndicator",
            "CupertinoActivityIndicator",
        )
        val RAW_REFRESH_RENDERERS = setOf(
            "PullToRefreshBox",
            "PullToRefresh",
            "MiuixPullToRefresh",
            "pullToRefresh",
        )
        val RENDERER_CALL = Regex(
            """\b(AdaptiveLoadingIndicator|CutePersonLoadingIndicator|AdaptivePullToRefreshBox|CircularProgressIndicator|LinearProgressIndicator|LoadingIndicator|InfiniteProgressIndicator|MiuixCircularProgressIndicator|MiuixInfiniteProgressIndicator|CupertinoActivityIndicator|PullToRefreshBox|PullToRefresh|MiuixPullToRefresh|pullToRefresh)\s*\(""",
        )
        val MATERIAL_PULL_REFRESH_IMPORT = Regex(
            """\bimport\s+androidx\.compose\.material3\.pulltorefresh(?:\.[A-Za-z0-9_*]+)?""",
        )
    }
}
