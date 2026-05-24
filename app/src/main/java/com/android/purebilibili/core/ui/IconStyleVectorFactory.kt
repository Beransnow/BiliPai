package com.android.purebilibili.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppIconStyle

data class IconAssetKey(
    val style: AppIconStyle,
    val roleName: String,
    val assetName: String,
    val variant: String = "custom-vector",
    val customName: String = assetName
)

private enum class IconMotif {
    HOME,
    GEAR,
    SEARCH,
    CLOSE,
    CLOCK,
    BOOKMARK,
    FOLDER,
    DOWNLOAD,
    UPLOAD,
    SHARE,
    DOCUMENT,
    CODE,
    HASH,
    SHIELD,
    REFRESH,
    GIFT,
    EYE,
    PLAY,
    MUSIC,
    WINDOW,
    TAP,
    COMMENT,
    HEART,
    BELL,
    PALETTE,
    WAND,
    GRID,
    USER,
    CLOUD,
    LINK,
    CHART,
    WARNING
}

internal fun resolveIconVisualMotifForAudit(assetName: String): String {
    return resolveIconMotif(assetName, assetName.stableOrdinal()).name
}

private val roleIconCache = mutableMapOf<IconAssetKey, ImageVector>()

internal fun createStyledRoleIcon(key: IconAssetKey, roleOrdinal: Int): ImageVector {
    return roleIconCache.getOrPut(key) {
        buildStyledRoleIcon(key, roleOrdinal)
    }
}

private fun buildStyledRoleIcon(key: IconAssetKey, roleOrdinal: Int): ImageVector {
    val normalizedOrdinal = roleOrdinal and 0xFFFF
    val motif = resolveIconMotif(key.assetName, normalizedOrdinal)

    return ImageVector.Builder(
        name = key.customName,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        drawMotif(motif, key.style, normalizedOrdinal)
        drawStyleAccent(key.style, normalizedOrdinal)
    }.build()
}

private fun resolveIconMotif(name: String, ordinal: Int): IconMotif {
    val token = name.lowercase()
    return when {
        token.hasAny("home", "recommend") -> IconMotif.HOME
        token.hasAny("donate", "gift") -> IconMotif.GIFT
        token.endsWith("_settings") || token.hasAny("gear") -> IconMotif.GEAR
        token.hasAny("search") -> IconMotif.SEARCH
        token.hasAny("clear", "close", "delete", "trash", "xmark") -> IconMotif.CLOSE
        token.hasAny("history", "time", "timer", "clock", "watch_later") -> IconMotif.CLOCK
        token.hasAny("bookmark", "favorite", "collection") -> IconMotif.BOOKMARK
        token.hasAny("folder", "path", "dir", "file_format", "media_write") -> IconMotif.FOLDER
        token.hasAny("download", "import", "icloud") -> IconMotif.DOWNLOAD
        token.hasAny("upload", "export", "backup") -> IconMotif.UPLOAD
        token.hasAny("share") -> IconMotif.SHARE
        token.hasAny("release") -> IconMotif.BOOKMARK
        token.hasAny("license", "article", "description", "logs", "note") -> IconMotif.DOCUMENT
        token.hasAny("verify", "verification", "consistency", "security", "permission", "shield", "lock") -> IconMotif.SHIELD
        token.hasAny("source", "github", "build", "code", "terminal", "json", "plugin", "rule") -> IconMotif.CODE
        token.hasAny("sha", "hash") -> IconMotif.HASH
        token.hasAny("update", "refresh", "restore", "reset", "replay") -> IconMotif.REFRESH
        token.hasAny("privacy", "visibility", "eye") -> IconMotif.EYE
        token.hasAny("play", "video", "quality") -> IconMotif.PLAY
        token.hasAny("audio", "music", "headphone") -> IconMotif.MUSIC
        token.hasAny("pip", "window", "fullscreen") -> IconMotif.WINDOW
        token.hasAny("gesture", "tap", "touch") -> IconMotif.TAP
        token.hasAny("comment", "message") -> IconMotif.COMMENT
        token.hasAny("like", "heart") -> IconMotif.HEART
        token.hasAny("notification", "bell") -> IconMotif.BELL
        token.hasAny("color", "palette", "paint", "skin", "wallpaper", "photo") -> IconMotif.PALETTE
        token.hasAny("animation", "sparkle", "blur", "glass", "magic", "tip", "hint", "wand") -> IconMotif.WAND
        token.hasAny("bottom", "top", "tab", "navigation", "sidebar", "list", "grid") -> IconMotif.GRID
        token.hasAny("profile", "user", "account", "person") -> IconMotif.USER
        token.hasAny("cloud", "webdav", "server") -> IconMotif.CLOUD
        token.hasAny("link", "open") -> IconMotif.LINK
        token.hasAny("chart", "analytics", "stats", "online", "data") -> IconMotif.CHART
        token.hasAny("warning", "error") -> IconMotif.WARNING
        else -> IconMotif.entries[ordinal % IconMotif.entries.size]
    }
}

private fun String.hasAny(vararg keywords: String): Boolean = keywords.any(::contains)

private fun String.stableOrdinal(): Int {
    return fold(0) { acc, char -> (acc * 31 + char.code) and 0x7FFF }
}

private fun ImageVector.Builder.drawMotif(
    motif: IconMotif,
    style: AppIconStyle,
    ordinal: Int
) {
    when (motif) {
        IconMotif.HOME -> drawStroke(style) {
            moveTo(4.5f, 11.5f)
            lineTo(12f, 5f)
            lineTo(19.5f, 11.5f)
            moveTo(7f, 10.5f)
            lineTo(7f, 19f)
            lineTo(17f, 19f)
            lineTo(17f, 10.5f)
            moveTo(10f, 19f)
            lineTo(10f, 14f)
            lineTo(14f, 14f)
            lineTo(14f, 19f)
        }
        IconMotif.GEAR -> drawStroke(style) {
            moveTo(12f, 8f)
            arcToRelative(4f, 4f, 0f, true, true, -0.1f, 0f)
            moveTo(12f, 3.5f)
            lineTo(12f, 6f)
            moveTo(12f, 18f)
            lineTo(12f, 20.5f)
            moveTo(3.5f, 12f)
            lineTo(6f, 12f)
            moveTo(18f, 12f)
            lineTo(20.5f, 12f)
            moveTo(6f, 6f)
            lineTo(7.8f, 7.8f)
            moveTo(16.2f, 16.2f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(16.2f, 7.8f)
            moveTo(7.8f, 16.2f)
            lineTo(6f, 18f)
        }
        IconMotif.SEARCH -> drawStroke(style) {
            moveTo(10.5f, 5f)
            arcToRelative(5.5f, 5.5f, 0f, true, true, -0.1f, 0f)
            moveTo(15f, 15f)
            lineTo(20f, 20f)
        }
        IconMotif.CLOSE -> drawStroke(style) {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
            moveTo(8f, 19.5f)
            lineTo(16f, 19.5f)
        }
        IconMotif.CLOCK -> drawStroke(style) {
            moveTo(12f, 4f)
            arcToRelative(8f, 8f, 0f, true, true, -0.1f, 0f)
            moveTo(12f, 8f)
            lineTo(12f, 12.5f)
            lineTo(15.5f, 15f)
        }
        IconMotif.BOOKMARK -> drawStroke(style) {
            moveTo(7f, 4.5f)
            lineTo(17f, 4.5f)
            lineTo(17f, 20f)
            lineTo(12f, 16.5f)
            lineTo(7f, 20f)
            close()
        }
        IconMotif.FOLDER -> drawStroke(style) {
            moveTo(4f, 7f)
            lineTo(9f, 7f)
            lineTo(11f, 9f)
            lineTo(20f, 9f)
            lineTo(20f, 18.5f)
            lineTo(4f, 18.5f)
            close()
        }
        IconMotif.DOWNLOAD -> drawStroke(style) {
            moveTo(12f, 4f)
            lineTo(12f, 14f)
            moveTo(8f, 10f)
            lineTo(12f, 14f)
            lineTo(16f, 10f)
            moveTo(5f, 18f)
            lineTo(19f, 18f)
        }
        IconMotif.UPLOAD -> drawStroke(style) {
            moveTo(12f, 20f)
            lineTo(12f, 10f)
            moveTo(8f, 14f)
            lineTo(12f, 10f)
            lineTo(16f, 14f)
            moveTo(5f, 6f)
            lineTo(19f, 6f)
        }
        IconMotif.SHARE -> drawStroke(style) {
            moveTo(7f, 12.5f)
            lineTo(17f, 7f)
            moveTo(7f, 12.5f)
            lineTo(17f, 17f)
            moveTo(6f, 10.5f)
            arcToRelative(2f, 2f, 0f, true, true, -0.1f, 0f)
            moveTo(18f, 5f)
            arcToRelative(2f, 2f, 0f, true, true, -0.1f, 0f)
            moveTo(18f, 15f)
            arcToRelative(2f, 2f, 0f, true, true, -0.1f, 0f)
        }
        IconMotif.DOCUMENT -> drawStroke(style) {
            moveTo(7f, 4f)
            lineTo(14f, 4f)
            lineTo(18f, 8f)
            lineTo(18f, 20f)
            lineTo(7f, 20f)
            close()
            moveTo(14f, 4f)
            lineTo(14f, 8f)
            lineTo(18f, 8f)
            moveTo(9.5f, 12f)
            lineTo(15.5f, 12f)
            moveTo(9.5f, 16f)
            lineTo(14f, 16f)
        }
        IconMotif.CODE -> drawStroke(style) {
            moveTo(9f, 7f)
            lineTo(5f, 12f)
            lineTo(9f, 17f)
            moveTo(15f, 7f)
            lineTo(19f, 12f)
            lineTo(15f, 17f)
            moveTo(13.5f, 5.5f)
            lineTo(10.5f, 18.5f)
        }
        IconMotif.HASH -> drawStroke(style) {
            moveTo(9f, 5f)
            lineTo(7f, 19f)
            moveTo(15f, 5f)
            lineTo(13f, 19f)
            moveTo(5f, 9f)
            lineTo(19f, 9f)
            moveTo(4f, 15f)
            lineTo(18f, 15f)
        }
        IconMotif.SHIELD -> drawStroke(style) {
            moveTo(12f, 4f)
            lineTo(18f, 6.5f)
            lineTo(17f, 14f)
            lineTo(12f, 20f)
            lineTo(7f, 14f)
            lineTo(6f, 6.5f)
            close()
            moveTo(9f, 12f)
            lineTo(11.3f, 14.3f)
            lineTo(15.5f, 10f)
        }
        IconMotif.REFRESH -> drawStroke(style) {
            moveTo(18f, 9f)
            arcTo(6f, 6f, 0f, false, false, 7f, 7f)
            moveTo(18f, 5f)
            lineTo(18f, 9f)
            lineTo(14f, 9f)
            moveTo(6f, 15f)
            arcTo(6f, 6f, 0f, false, false, 17f, 17f)
            moveTo(6f, 19f)
            lineTo(6f, 15f)
            lineTo(10f, 15f)
        }
        IconMotif.GIFT -> drawStroke(style) {
            moveTo(5f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            close()
            moveTo(12f, 10f)
            lineTo(12f, 20f)
            moveTo(4f, 8f)
            lineTo(20f, 8f)
            lineTo(20f, 10f)
            lineTo(4f, 10f)
            close()
            moveTo(12f, 8f)
            curveTo(9f, 4.5f, 6f, 5f, 7f, 8f)
            moveTo(12f, 8f)
            curveTo(15f, 4.5f, 18f, 5f, 17f, 8f)
        }
        IconMotif.EYE -> drawStroke(style) {
            moveTo(3.5f, 12f)
            curveTo(7f, 6.5f, 17f, 6.5f, 20.5f, 12f)
            curveTo(17f, 17.5f, 7f, 17.5f, 3.5f, 12f)
            moveTo(12f, 9f)
            arcToRelative(3f, 3f, 0f, true, true, -0.1f, 0f)
        }
        IconMotif.PLAY -> drawStroke(style) {
            moveTo(8f, 5f)
            lineTo(18f, 12f)
            lineTo(8f, 19f)
            close()
        }
        IconMotif.MUSIC -> drawStroke(style) {
            moveTo(16f, 5f)
            lineTo(16f, 15f)
            moveTo(10f, 8f)
            lineTo(16f, 6f)
            moveTo(10f, 8f)
            lineTo(10f, 17f)
            moveTo(8.5f, 17f)
            arcToRelative(2f, 2f, 0f, true, true, -0.1f, 0f)
            moveTo(14.5f, 15f)
            arcToRelative(2f, 2f, 0f, true, true, -0.1f, 0f)
        }
        IconMotif.WINDOW -> drawStroke(style) {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            lineTo(20f, 18f)
            lineTo(4f, 18f)
            close()
            moveTo(12f, 11f)
            lineTo(18f, 11f)
            lineTo(18f, 16f)
            lineTo(12f, 16f)
            close()
        }
        IconMotif.TAP -> drawStroke(style) {
            moveTo(12f, 5f)
            lineTo(12f, 13f)
            moveTo(12f, 13f)
            lineTo(15f, 10f)
            lineTo(18f, 13f)
            lineTo(15f, 20f)
            lineTo(10f, 20f)
            lineTo(7f, 15f)
            moveTo(7f, 6f)
            lineTo(5.5f, 4.5f)
            moveTo(17f, 6f)
            lineTo(18.5f, 4.5f)
        }
        IconMotif.COMMENT -> drawStroke(style) {
            moveTo(5f, 6f)
            lineTo(19f, 6f)
            lineTo(19f, 15f)
            lineTo(13f, 15f)
            lineTo(8f, 19f)
            lineTo(8f, 15f)
            lineTo(5f, 15f)
            close()
        }
        IconMotif.HEART -> drawStroke(style) {
            moveTo(12f, 19f)
            curveTo(6f, 15f, 4f, 11f, 6.5f, 8f)
            curveTo(8.5f, 5.8f, 11f, 7f, 12f, 9f)
            curveTo(13f, 7f, 15.5f, 5.8f, 17.5f, 8f)
            curveTo(20f, 11f, 18f, 15f, 12f, 19f)
        }
        IconMotif.BELL -> drawStroke(style) {
            moveTo(7f, 10f)
            curveTo(7f, 6.5f, 9f, 4.5f, 12f, 4.5f)
            curveTo(15f, 4.5f, 17f, 6.5f, 17f, 10f)
            lineTo(17f, 14f)
            lineTo(19f, 17f)
            lineTo(5f, 17f)
            lineTo(7f, 14f)
            close()
            moveTo(10f, 19f)
            lineTo(14f, 19f)
        }
        IconMotif.PALETTE -> drawStroke(style) {
            moveTo(12f, 4f)
            arcToRelative(8f, 8f, 0f, true, true, -3f, 15.4f)
            curveTo(7f, 18.5f, 8f, 15f, 11f, 15f)
            lineTo(14f, 15f)
            curveTo(17f, 15f, 20f, 13f, 19f, 10f)
            curveTo(18f, 6.5f, 15f, 4f, 12f, 4f)
            moveTo(8f, 10f)
            lineTo(8.1f, 10f)
            moveTo(12f, 8f)
            lineTo(12.1f, 8f)
            moveTo(16f, 10f)
            lineTo(16.1f, 10f)
        }
        IconMotif.WAND -> drawStroke(style) {
            moveTo(5f, 19f)
            lineTo(15f, 9f)
            moveTo(13f, 7f)
            lineTo(17f, 11f)
            moveTo(18f, 4f)
            lineTo(18f, 7f)
            moveTo(16.5f, 5.5f)
            lineTo(19.5f, 5.5f)
            moveTo(6f, 6f)
            lineTo(6f, 8f)
            moveTo(5f, 7f)
            lineTo(7f, 7f)
        }
        IconMotif.GRID -> drawStroke(style) {
            moveTo(5f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 10f)
            lineTo(5f, 10f)
            close()
            moveTo(14f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 10f)
            lineTo(14f, 10f)
            close()
            moveTo(5f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 19f)
            lineTo(5f, 19f)
            close()
            moveTo(14f, 14f)
            lineTo(19f, 14f)
            lineTo(19f, 19f)
            lineTo(14f, 19f)
            close()
        }
        IconMotif.USER -> drawStroke(style) {
            moveTo(12f, 5f)
            arcToRelative(3.5f, 3.5f, 0f, true, true, -0.1f, 0f)
            moveTo(5f, 20f)
            curveTo(6.5f, 15.5f, 17.5f, 15.5f, 19f, 20f)
        }
        IconMotif.CLOUD -> drawStroke(style) {
            moveTo(7f, 17f)
            lineTo(17f, 17f)
            curveTo(20f, 17f, 21f, 12f, 17f, 11f)
            curveTo(16f, 6f, 9f, 6f, 8f, 11f)
            curveTo(4f, 11f, 4f, 17f, 7f, 17f)
        }
        IconMotif.LINK -> drawStroke(style) {
            moveTo(9.5f, 14.5f)
            lineTo(14.5f, 9.5f)
            moveTo(10f, 8f)
            curveTo(12f, 6f, 15f, 6f, 17f, 8f)
            curveTo(19f, 10f, 19f, 13f, 17f, 15f)
            moveTo(14f, 16f)
            curveTo(12f, 18f, 9f, 18f, 7f, 16f)
            curveTo(5f, 14f, 5f, 11f, 7f, 9f)
        }
        IconMotif.CHART -> drawStroke(style) {
            moveTo(5f, 19f)
            lineTo(19f, 19f)
            moveTo(7f, 16f)
            lineTo(7f, 11f)
            moveTo(12f, 16f)
            lineTo(12f, 7f)
            moveTo(17f, 16f)
            lineTo(17f, 4f)
        }
        IconMotif.WARNING -> drawStroke(style) {
            moveTo(12f, 4f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
            moveTo(12f, 9f)
            lineTo(12f, 14f)
            moveTo(12f, 17f)
            lineTo(12.1f, 17f)
        }
    }
}

private fun ImageVector.Builder.drawStyleAccent(style: AppIconStyle, ordinal: Int) {
    when (style) {
        AppIconStyle.MATERIAL_SYMBOLS -> {
            drawFill {
                moveTo(17f, 3.5f)
                lineTo(20.5f, 3.5f)
                lineTo(20.5f, 7f)
                lineTo(17f, 7f)
                close()
            }
            drawStroke(style) {
                moveTo(5f, 21f)
                lineTo(19f, 21f)
            }
        }
        AppIconStyle.LUCIDE -> drawStroke(style) {
            moveTo(4f, 4f)
            lineTo(6.5f, 4f)
            moveTo(4f, 4f)
            lineTo(4f, 6.5f)
        }
        AppIconStyle.PHOSPHOR -> drawStroke(style) {
            moveTo(12f, 2.75f)
            arcToRelative(9.25f, 9.25f, 0f, true, true, -0.1f, 0f)
        }
        AppIconStyle.TABLER -> drawStroke(style) {
            moveTo(3.5f, 7.5f)
            lineTo(3.5f, 3.5f)
            lineTo(7.5f, 3.5f)
            moveTo(16.5f, 3.5f)
            lineTo(20.5f, 3.5f)
            lineTo(20.5f, 7.5f)
            moveTo(20.5f, 16.5f)
            lineTo(20.5f, 20.5f)
            lineTo(16.5f, 20.5f)
            moveTo(7.5f, 20.5f)
            lineTo(3.5f, 20.5f)
            lineTo(3.5f, 16.5f)
        }
    }
}

private fun ImageVector.Builder.drawFill(block: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero
    ) {
        block()
    }
}

private fun ImageVector.Builder.drawStroke(
    style: AppIconStyle,
    block: PathBuilder.() -> Unit
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = when (style) {
            AppIconStyle.MATERIAL_SYMBOLS -> 2.25f
            AppIconStyle.LUCIDE -> 2f
            AppIconStyle.PHOSPHOR -> 1.85f
            AppIconStyle.TABLER -> 2f
        },
        strokeLineCap = when (style) {
            AppIconStyle.TABLER -> StrokeCap.Square
            else -> StrokeCap.Round
        },
        strokeLineJoin = when (style) {
            AppIconStyle.TABLER -> StrokeJoin.Miter
            else -> StrokeJoin.Round
        },
        pathFillType = PathFillType.NonZero
    ) {
        block()
    }
}
