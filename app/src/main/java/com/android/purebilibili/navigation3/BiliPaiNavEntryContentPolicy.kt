package com.android.purebilibili.navigation3

internal enum class BiliPaiNavEntryContentRole {
    HOME,
    DYNAMIC,
    SEARCH,
    SETTINGS,
    HISTORY,
    FAVORITE,
    WATCH_LATER,
    DEFERRED_LEGACY_ROUTE
}

internal fun resolveBiliPaiNavEntryContentRole(key: BiliPaiNavKey): BiliPaiNavEntryContentRole {
    return when (key) {
        BiliPaiNavKey.Home -> BiliPaiNavEntryContentRole.HOME
        BiliPaiNavKey.Dynamic -> BiliPaiNavEntryContentRole.DYNAMIC
        BiliPaiNavKey.Search -> BiliPaiNavEntryContentRole.SEARCH
        BiliPaiNavKey.Settings -> BiliPaiNavEntryContentRole.SETTINGS
        BiliPaiNavKey.History -> BiliPaiNavEntryContentRole.HISTORY
        BiliPaiNavKey.Favorite -> BiliPaiNavEntryContentRole.FAVORITE
        BiliPaiNavKey.WatchLater -> BiliPaiNavEntryContentRole.WATCH_LATER
        else -> BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE
    }
}
