package com.android.purebilibili.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.size.Scale

/**
 * Builds an [ImageRequest] only when one of its request-defining inputs changes.
 *
 * Keep visual policy at the call site: callers must pass the same crossfade, size, headers and
 * cache keys they used before moving request construction out of the composable body.
 */
@Composable
fun rememberImageRequest(
    data: Any?,
    widthPx: Int? = null,
    heightPx: Int? = null,
    referer: String? = null,
    crossfadeEnabled: Boolean? = null,
    crossfadeMillis: Int? = null,
    placeholderMemoryCacheKey: String? = null,
    memoryCacheKey: String? = null,
    diskCacheKey: String? = null,
    scale: Scale? = null,
): ImageRequest {
    val context = LocalContext.current
    return remember(
        context,
        data,
        widthPx,
        heightPx,
        referer,
        crossfadeEnabled,
        crossfadeMillis,
        placeholderMemoryCacheKey,
        memoryCacheKey,
        diskCacheKey,
        scale,
    ) {
        require(crossfadeEnabled == null || crossfadeMillis == null) {
            "Choose either boolean or duration crossfade semantics"
        }
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                when {
                    widthPx != null && heightPx != null -> size(widthPx, heightPx)
                    widthPx != null -> size(widthPx)
                }
                referer?.let { addHeader("Referer", it) }
                when {
                    crossfadeMillis != null -> crossfade(crossfadeMillis)
                    crossfadeEnabled != null -> crossfade(crossfadeEnabled)
                }
                placeholderMemoryCacheKey?.let { this.placeholderMemoryCacheKey(it) }
                memoryCacheKey?.let { this.memoryCacheKey(it) }
                diskCacheKey?.let { this.diskCacheKey(it) }
                scale?.let { this.scale(it) }
            }
            .build()
    }
}
