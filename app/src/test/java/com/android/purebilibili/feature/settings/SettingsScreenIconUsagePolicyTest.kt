package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SettingsScreenIconUsagePolicyTest {

    @Test
    fun settingsUiFiles_doNotBypassGlobalIconStyleWithDirectIconLiterals() {
        val offenders = settingsUiSourceFiles()
            .flatMap { file ->
                directIconLiteralPattern.findAll(file.readText()).map { match ->
                    "${file.relativeTo(projectRoot())}:${match.range.first + 1}"
                }
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "设置 UI 不允许直接写死 Icons/CupertinoIcons，必须走全局图标风格入口：$offenders"
        )
    }

    @Test
    fun settingsUiFiles_doNotReuseInlineIconKeysWithinSameFile() {
        val offenders = settingsUiSourceFiles()
            .mapNotNull { file ->
                val keys = file.readText().extractSettingsIconKeys()
                val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                if (duplicates.isEmpty()) null else "${file.relativeTo(projectRoot())}: $duplicates"
            }

        assertTrue(
            offenders.isEmpty(),
            "同一设置 UI 文件内不同设置项不允许复用同一个全局图标键：$offenders"
        )
    }

    private fun settingsUiSourceFiles(): List<File> {
        val root = projectRoot()
        val settingsDir = File(root, "app/src/main/java/com/android/purebilibili/feature/settings")
        return settingsDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name in iconPolicyFiles }
            .toList()
    }

    private fun String.extractSettingsIconKeys(): List<String> {
        val inlineKeys = inlineIconPattern.findAll(this).map { "inline:${it.groupValues[1]}" }
        val semanticKeys = semanticIconPattern.findAll(this).map { match ->
            val call = match.value
            val explicitKey = semanticIconKeyPattern.find(call)?.groupValues?.get(1)
            "semantic:${explicitKey ?: match.groupValues[1]}"
        }
        return (inlineKeys + semanticKeys).toList()
    }

    private fun projectRoot(): File {
        val candidates = listOf(File("."), File(".."))
        return candidates.first { File(it, "app/src/main/java").exists() }.canonicalFile
    }

    private companion object {
        private val iconPolicyFiles = setOf(
            "SettingsSemanticIconPolicy.kt",
            "SettingsEntryVisualPolicy.kt"
        )
        private val directIconLiteralPattern = Regex("\\bCupertinoIcons\\b|\\bIcons\\.")
        private val inlineIconPattern = Regex("rememberSettingsInlineIcon\\(\\s*\"([^\"]+)\"")
        private val semanticIconPattern = Regex(
            """rememberSettingsSemanticIcon\(\s*SettingsIconRole\.([A-Z0-9_]+).*?\)""",
            RegexOption.DOT_MATCHES_ALL
        )
        private val semanticIconKeyPattern = Regex("iconKey\\s*=\\s*\"([^\"]+)\"")
    }
}
