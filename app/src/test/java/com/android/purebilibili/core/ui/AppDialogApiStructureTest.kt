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
        assertEquals(3, Regex("""\bAppAlertDialog\s*\(""").findAll(profileSource).count())
        assertEquals(3, directAlert.findAll(profileSource).count())
        assertEquals(1, Regex("""\bAppAlertDialog\s*\(""").findAll(spaceSource).count())
        assertEquals(1, directAlert.findAll(spaceSource).count())
    }

    @Test
    fun dynamicCommentSheetNoLongerCallsHistoricalIosEntryPoint() {
        val source = load(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        )

        assertTrue(source.contains("AppModalBottomSheet("))
        assertFalse(source.contains("IOSModalBottomSheet("))
    }

    private fun load(path: String): String {
        val normalized = path.removePrefix("app/")
        return listOf(File(path), File(normalized))
            .first { it.exists() }
            .readText()
    }
}
