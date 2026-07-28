package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDialogApiStructureTest {

    @Test
    fun neutralDialogApiDelegatesSlotsToExistingAdaptiveRenderers() {
        val apiSource = load("app/src/main/java/com/android/purebilibili/core/ui/AppDialogComponents.kt")
        val dialogRenderer = load("app/src/main/java/com/android/purebilibili/core/ui/iOSDialogComponents.kt")

        assertTrue(apiSource.contains("fun AppAlertDialog("))
        assertTrue(apiSource.contains("icon: @Composable (() -> Unit)? = null"))
        assertTrue(apiSource.contains(") = IOSAlertDialog("))
        assertTrue(apiSource.contains("icon = icon"))
        assertTrue(dialogRenderer.contains("icon = icon"))
        assertTrue(dialogRenderer.contains("icon()"))
    }

    @Test
    fun neutralSheetApiDelegatesContentAndDragHandleToExistingRenderer() {
        val apiSource = load("app/src/main/java/com/android/purebilibili/core/ui/AppDialogComponents.kt")

        assertTrue(apiSource.contains("fun AppModalBottomSheet("))
        assertTrue(apiSource.contains("containerColor: Color? = null"))
        assertTrue(apiSource.contains(") = IOSModalBottomSheet("))
        listOf(
            "modifier = modifier",
            "sheetState = sheetState",
            "containerColor = containerColor",
            "scrimColor = scrimColor",
            "presentationProgress = presentationProgress",
            "dragHandle = dragHandle",
            "windowInsets = windowInsets",
            "content = content",
        ).forEach { forwarding ->
            assertTrue(apiSource.contains(forwarding), "Sheet API does not forward $forwarding")
        }
        assertTrue(apiSource.contains("fun AppSheetDragHandle() = IOSDragHandle()"))
    }

    @Test
    fun phaseThreeDialogPilotUsesNeutralEntryPoints() {
        val fullyMigratedDialogs = listOf(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/InboxScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
        )
        val directAlert = Regex("""\bAlertDialog\s*\(""")

        fullyMigratedDialogs.forEach { path ->
            val source = load(path)
            assertTrue(source.contains("AppAlertDialog("), "Neutral dialog is missing in $path")
            assertFalse(directAlert.containsMatchIn(source), "Direct AlertDialog remains in $path")
        }

        val profileSource = load("app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt")
        val spaceSource = load("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")
        assertEquals(6, Regex("""\bAppAlertDialog\s*\(""").findAll(profileSource).count())
        assertFalse(directAlert.containsMatchIn(profileSource))
        assertEquals(2, Regex("""\bAppAlertDialog\s*\(""").findAll(spaceSource).count())
        assertFalse(directAlert.containsMatchIn(spaceSource))
    }

    @Test
    fun phaseThreeSheetsUseNeutralEntryPoint() {
        val expectedNeutralSheetCalls = mapOf(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt" to 1,
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt" to 1,
            "app/src/main/java/com/android/purebilibili/feature/profile/OfficialWallpaperSheet.kt" to 1,
            "app/src/main/java/com/android/purebilibili/feature/profile/WallpaperAdjustmentSheet.kt" to 2,
            "app/src/main/java/com/android/purebilibili/feature/profile/SplashWallpaperPickerSheet.kt" to 1,
        )
        val directSheet = Regex("""(?<![A-Za-z0-9_])(ModalBottomSheet|IOSModalBottomSheet)\s*\(""")

        expectedNeutralSheetCalls.forEach { (path, expectedCount) ->
            val source = load(path)
            assertEquals(
                expectedCount,
                Regex("""\bAppModalBottomSheet\s*\(""").findAll(source).count(),
                "Unexpected neutral sheet count in $path",
            )
            assertFalse(directSheet.containsMatchIn(source), "Direct sheet renderer remains in $path")
        }
    }

    @Test
    fun phaseThreeFeatureScopeRejectsDirectDialogAndSheetRenderers() {
        val roots = listOf(
            "app/src/main/java/com/android/purebilibili/feature/dynamic",
            "app/src/main/java/com/android/purebilibili/feature/list",
            "app/src/main/java/com/android/purebilibili/feature/message",
            "app/src/main/java/com/android/purebilibili/feature/partition",
            "app/src/main/java/com/android/purebilibili/feature/profile",
            "app/src/main/java/com/android/purebilibili/feature/search",
            "app/src/main/java/com/android/purebilibili/feature/space",
            "app/src/main/java/com/android/purebilibili/feature/live",
        )
        val stageFourLiveFiles = setOf(
            "LiveContributionRankSheet.kt",
            "LiveInteractionSheets.kt",
            "LivePlayerScreen.kt",
            "LiveSendDanmakuSheet.kt",
        )
        val directRenderer = Regex(
            """(?<![A-Za-z0-9_])(AlertDialog|ModalBottomSheet|IOSAlertDialog|IOSModalBottomSheet|IOSDialogAction|IOSDragHandle)\s*\(""",
        )

        roots.flatMap { resolve(it).walkTopDown().filter(File::isFile).toList() }
            .filter { it.extension == "kt" }
            .filterNot { it.name in stageFourLiveFiles }
            .forEach { file ->
                assertFalse(
                    directRenderer.containsMatchIn(file.readText()),
                    "Direct Dialog/Sheet renderer remains in ${file.path}",
                )
            }
    }

    private fun resolve(path: String): File {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized))
            .first { it.exists() }
    }

    private fun load(path: String): String = resolve(path).readText()
}
