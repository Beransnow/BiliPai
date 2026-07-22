package com.android.purebilibili.core.store

const val APP_ICON_COMPONENT_PACKAGE_NAME = "com.android.purebilibili"
const val APP_ICON_COMPAT_ALIAS_CLASS_NAME = "$APP_ICON_COMPONENT_PACKAGE_NAME.MainActivityAlias3D"

const val DEFAULT_APP_ICON_KEY = "icon_blue_snow_maid"

private val CANONICAL_APP_ICON_KEYS = setOf(
    "icon_blue_snow_maid",
    "icon_3d",
    "icon_bilipai",
    "icon_bilipai_pink",
    "icon_bilipai_white",
    "icon_bilipai_monet"
)

private val LAUNCHER_ALIAS_SUFFIX_BY_KEY = mapOf(
    "icon_blue_snow_maid" to "MainActivityAliasBlueSnowMaid",
    "icon_3d" to "MainActivityAlias3DLauncher",
    "icon_bilipai" to "MainActivityAliasBiliPai",
    "icon_bilipai_pink" to "MainActivityAliasBiliPaiPink",
    "icon_bilipai_white" to "MainActivityAliasBiliPaiWhite",
    "icon_bilipai_monet" to "MainActivityAliasBiliPaiMonet"
)

private val NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY = mapOf(
    "icon_blue_snow_maid" to "MainActivityAliasBlueSnowMaidNoIcon",
    "icon_3d" to "MainActivityAlias3DNoIcon",
    "icon_bilipai" to "MainActivityAliasBiliPaiNoIcon",
    "icon_bilipai_pink" to "MainActivityAliasBiliPaiPinkNoIcon",
    "icon_bilipai_white" to "MainActivityAliasBiliPaiWhiteNoIcon",
    "icon_bilipai_monet" to "MainActivityAliasBiliPaiMonetNoIcon"
)

private val RETIRED_APP_ICON_ALIAS_SUFFIXES = setOf(
    "MainActivityAliasAnime",
    "MainActivityAliasFlat",
    "MainActivityAliasTelegramBlue",
    "MainActivityAliasDark",
    "MainActivityAliasYuki",
    "MainActivityAliasHeadphone",
    "MainActivityAliasAnimeNoIcon",
    "MainActivityAliasFlatNoIcon",
    "MainActivityAliasTelegramBlueNoIcon",
    "MainActivityAliasDarkNoIcon",
    "MainActivityAliasYukiNoIcon",
    "MainActivityAliasHeadphoneNoIcon"
)

fun normalizeAppIconKey(rawKey: String?): String {
    val key = rawKey?.trim().orEmpty()
    if (key.isEmpty()) return DEFAULT_APP_ICON_KEY

    return when (key) {
        "default", "Blue Snow Maid", "蓝雪女仆" -> "icon_blue_snow_maid"
        "3D" -> "icon_3d"
        "BiliPai", "bilipai", "Icon BiliPai" -> "icon_bilipai"
        "BiliPai Pink", "BiliPai 粉", "bilipai_pink" -> "icon_bilipai_pink"
        "BiliPai White", "BiliPai 白", "bilipai_white" -> "icon_bilipai_white"
        "BiliPai Monet", "BiliPai 莫奈", "bilipai_monet" -> "icon_bilipai_monet"
        else -> if (CANONICAL_APP_ICON_KEYS.contains(key)) key else DEFAULT_APP_ICON_KEY
    }
}

fun resolveAppIconLauncherAlias(
    packageName: String,
    rawKey: String?,
    splashIconVisible: Boolean = true
): String {
    val normalizedKey = normalizeAppIconKey(rawKey)
    val aliasMap = if (splashIconVisible) {
        LAUNCHER_ALIAS_SUFFIX_BY_KEY
    } else {
        NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY
    }
    val aliasSuffix = aliasMap[normalizedKey]
        ?: aliasMap.getValue(DEFAULT_APP_ICON_KEY)
    return "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix"
}

fun allManagedAppIconLauncherAliases(packageName: String): Set<String> {
    return (
        LAUNCHER_ALIAS_SUFFIX_BY_KEY.values +
            NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY.values +
            RETIRED_APP_ICON_ALIAS_SUFFIXES
        )
        .map { aliasSuffix -> "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix" }
        .plus(APP_ICON_COMPAT_ALIAS_CLASS_NAME)
        .toSet()
}
