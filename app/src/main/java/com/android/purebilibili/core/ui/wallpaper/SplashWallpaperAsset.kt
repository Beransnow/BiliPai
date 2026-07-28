package com.android.purebilibili.core.ui.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

internal data class SplashWallpaperDecodeSize(
    val widthPx: Int,
    val heightPx: Int,
)

internal fun resolveSplashWallpaperDecodeSize(
    windowWidthPx: Int,
    windowHeightPx: Int,
): SplashWallpaperDecodeSize = SplashWallpaperDecodeSize(
    widthPx = windowWidthPx.coerceAtLeast(1),
    heightPx = windowHeightPx.coerceAtLeast(1),
)

/** Reads only image bounds for local launch assets; remote assets fall back to painter metadata. */
internal suspend fun probeSplashWallpaperAspectRatio(
    context: Context,
    rawUri: String,
): Float? = withContext(Dispatchers.IO) {
    if (rawUri.isBlank()) return@withContext null
    val uri = Uri.parse(rawUri)
    val input = when (uri.scheme?.lowercase()) {
        "content", "android.resource" -> context.contentResolver.openInputStream(uri)
        "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
        null, "" -> File(rawUri).takeIf(File::isFile)?.let(::FileInputStream)
        else -> null
    } ?: return@withContext null

    input.use { stream ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth.toFloat() / options.outHeight.toFloat()
        } else {
            null
        }
    }
}
