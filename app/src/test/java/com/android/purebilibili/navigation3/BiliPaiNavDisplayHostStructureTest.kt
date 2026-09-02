package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavDisplayHostStructureTest {
    @Test
    fun appHostsMiuixNavigationInsideGlobalSharedTransitionLayout() {
        val appNavigation = source("navigation/AppNavigation.kt")
        val host = source("navigation3/BiliPaiNavDisplayHost.kt")

        assertTrue(appNavigation.contains("SharedTransitionProvider(enabled = sharedVideoCardTransitionEnabled)"))
        assertTrue(host.contains("NavDisplay("))
        assertTrue(host.contains("MiuixSharedTransitionEntryBridge("))
        assertTrue(host.contains("participatesInSharedTransition = participatesInSharedCardTransition"))
        assertFalse(host.contains("if (participatesInSharedCardTransition)"))
        assertTrue(host.contains("miuixSharedElementNavTransition("))
        assertFalse(host.contains("miuixVideoCardNavTransition("))
        assertFalse(host.contains("LocalMiuixVideoCardTransitionState"))
    }

    @Test
    fun entryBridgeUsesDeferredAnimatedVisibilityForPredictiveLifecycle() {
        val bridge = source("navigation3/MiuixSharedTransitionEntryBridge.kt")

        assertTrue(bridge.contains("DeferredTransitionState(initiallyVisible)"))
        assertTrue(bridge.contains("visibilityState.defer(true)"))
        assertTrue(bridge.contains("visibilityState.defer(false)"))
        assertTrue(bridge.contains("visibilityState.animateTo(true)"))
        assertTrue(bridge.contains("visibilityState.animateTo(false)"))
        assertTrue(bridge.contains("HoldVisible -> visibilityState.animateTo(true)"))
        assertTrue(bridge.contains("DeferredAnimatedVisibility("))
        assertTrue(bridge.contains("ProvideAnimatedVisibilityScope(this)"))
    }

    @Test
    fun videoEntryTransitionDoesNotOwnCardGeometry() {
        val transition = source("navigation3/predictiveback/MiuixVideoCardNavTransition.kt")

        assertFalse(transition.contains("sourceBounds"))
        assertFalse(transition.contains("scaleX ="))
        assertFalse(transition.contains("scaleY ="))
        assertFalse(transition.contains("translationX ="))
        assertFalse(transition.contains("translationY ="))
        assertFalse(transition.contains("landingScale"))
    }

    @Test
    fun destinationRouteCanArmSharedTransitionOnTheClickFrame() {
        val host = source("navigation3/BiliPaiNavDisplayHost.kt")

        assertTrue(host.contains("sourceMetadata.sourceRoute"))
        assertTrue(host.contains("?: resolveCardMorphDestinationSourceRoute(currentKey)"))
    }

    private fun source(relativePath: String): String {
        val repoPath = File("app/src/main/java/com/android/purebilibili/$relativePath")
        if (repoPath.isFile) return repoPath.readText()
        return File("src/main/java/com/android/purebilibili/$relativePath").readText()
    }
}
