package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

internal enum class VideoCardSourceLayout {
    STACKED,
    SIDE_BY_SIDE,
    COVER_ONLY,
}

/**
 * Exact click-time cover request from the stationary source card.
 *
 * [coverUrl] / [coverCacheKey] / decode size must be the exact Coil request the list
 * AsyncImage uses at rest (including [com.android.purebilibili.feature.home.HomeCoverRequestSpec]
 * sized URL + `size(w,h)`). Detail resident / player-section covers reuse these so handoff
 * pixels match the stationary card — not `fixImageUrl` / default cache key.
 */
@Immutable
internal data class VideoCardSourceChromeSnapshot(
    /** Exact list-card cover request URL (sized / quality resolved). */
    val coverUrl: String = "",
    /** Exact list-card Coil memoryCacheKey / diskCacheKey. */
    val coverCacheKey: String = "",
    /** Coil `size(w,h)` from list HomeCoverRequestSpec; 0 = omit size(). */
    val coverDecodeWidthPx: Int = 0,
    val coverDecodeHeightPx: Int = 0,
)

/** Media-only click snapshot scoped to the active video navigation session. */
internal val LocalVideoCardSourceMediaSnapshot =
    compositionLocalOf<VideoCardSourceChromeSnapshot?> { null }

internal fun resolveVideoCardSourceLayout(
    cardBounds: Rect?,
    coverBounds: Rect?,
): VideoCardSourceLayout {
    val card = cardBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoCardSourceLayout.COVER_ONLY
    val cover = coverBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return VideoCardSourceLayout.COVER_ONLY
    val horizontalTolerance = card.width * 0.1f
    val verticalTolerance = card.height * 0.1f
    val coverSpansCardWidth = cover.left <= card.left + horizontalTolerance &&
        cover.right >= card.right - horizontalTolerance
    val coverSpansCardHeight = cover.top <= card.top + verticalTolerance &&
        cover.bottom >= card.bottom - verticalTolerance
    return when {
        coverSpansCardWidth && !coverSpansCardHeight -> VideoCardSourceLayout.STACKED
        coverSpansCardHeight && !coverSpansCardWidth -> VideoCardSourceLayout.SIDE_BY_SIDE
        else -> VideoCardSourceLayout.COVER_ONLY
    }
}
