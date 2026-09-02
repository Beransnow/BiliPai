package com.android.purebilibili.navigation3

internal data class BiliPaiVideoSource(
    val route: String?,
    val key: String?
)

internal fun resolveBiliPaiVideoSource(
    bvid: String,
    explicitSourceRoute: String?,
    currentKey: BiliPaiNavKey?,
    previousSourceRoute: String?
): BiliPaiVideoSource {
    val route = normalizeBiliPaiVideoSourceRoute(
        explicitSourceRoute ?: when (currentKey) {
            is BiliPaiNavKey.VideoDetail -> {
                // Prefer explicit related host `video/{parent}` when provided by callers.
                // Without explicit: keep list origin (home/search/…) so multi-hop returns
                // still land on the original card, not an intermediate detail.
                previousSourceRoute
                    ?.takeIf { it.isNotBlank() }
                    ?: currentKey.sourceRoute
                    ?: "video/${currentKey.bvid}"
            }
            null -> previousSourceRoute
            else -> currentKey.toLegacyRoute()
        }
    )
    return BiliPaiVideoSource(
        route = route,
        key = route?.takeIf { bvid.isNotBlank() }?.let { "$it:$bvid" }
    )
}

internal fun normalizeBiliPaiVideoSourceRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        normalized
    } else {
        normalized.substringBefore("?")
    }
}

/** Related-detail hops use source route `video/{parentBvid}`. */
internal fun isRelatedVideoCardMorphSourceRoute(sourceRoute: String?): Boolean {
    val route = sourceRoute?.substringBefore('?')?.trim().orEmpty()
    return route.startsWith("video/")
}

internal fun resolveVideoCardTransitionEnabledForSource(
    cardTransitionEnabled: Boolean,
    relatedVideoTransitionEnabled: Boolean,
    sourceRoute: String?,
): Boolean = cardTransitionEnabled && (
    relatedVideoTransitionEnabled || !isRelatedVideoCardMorphSourceRoute(sourceRoute)
)

/**
 * Card morph mode is part of the navigation meaning, not a card-local animation choice.
 * Every video-card source, including a related card inside a retained detail page, uses the same
 * whole-card treatment so cover and metadata always travel as one visual unit.
 */
internal enum class BiliPaiVideoCardTransitionMode {
    NONE,
    STANDARD_SHARED_BOUNDS,
}

internal fun resolveBiliPaiVideoCardTransitionMode(
    cardTransitionEnabled: Boolean,
    reduceMotion: Boolean,
    sourceRoute: String?,
): BiliPaiVideoCardTransitionMode {
    if (
        !cardTransitionEnabled ||
        reduceMotion ||
        sourceRoute?.substringBefore('?').isNullOrBlank()
    ) {
        return BiliPaiVideoCardTransitionMode.NONE
    }
    // Standard sharedBounds measures the live source/target layouts. If the Lazy item disappeared,
    // Compose simply runs the unmatched enter/exit fallback.
    return BiliPaiVideoCardTransitionMode.STANDARD_SHARED_BOUNDS
}

/**
 * Compatibility gate retained for callers while the implementation is standard sharedBounds.
 * Click-time bounds no longer control eligibility.
 */
internal fun shouldUseVideoCardSharedBoundsTransition(
    cardTransitionEnabled: Boolean,
    reduceMotion: Boolean,
    sourceRoute: String?,
): Boolean = resolveBiliPaiVideoCardTransitionMode(
    cardTransitionEnabled = cardTransitionEnabled,
    reduceMotion = reduceMotion,
    sourceRoute = sourceRoute,
) != BiliPaiVideoCardTransitionMode.NONE
