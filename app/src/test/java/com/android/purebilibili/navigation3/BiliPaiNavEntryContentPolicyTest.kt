package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals

class BiliPaiNavEntryContentPolicyTest {

    @Test
    fun topLevelKeysResolveToDedicatedContentRoles() {
        assertEquals(BiliPaiNavEntryContentRole.HOME, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Home))
        assertEquals(BiliPaiNavEntryContentRole.DYNAMIC, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Dynamic))
        assertEquals(BiliPaiNavEntryContentRole.SEARCH, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Search))
        assertEquals(BiliPaiNavEntryContentRole.SETTINGS, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Settings))
        assertEquals(BiliPaiNavEntryContentRole.HISTORY, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.History))
        assertEquals(BiliPaiNavEntryContentRole.FAVORITE, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Favorite))
        assertEquals(BiliPaiNavEntryContentRole.WATCH_LATER, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.WatchLater))
    }

    @Test
    fun detailKeysRemainDeferredUntilTheirLegacyRouteBodiesAreExtracted() {
        assertEquals(
            BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.VideoDetail("BV1"))
        )
        assertEquals(
            BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.ArticleDetail(articleId = 1L))
        )
        assertEquals(
            BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Live(roomId = 1L))
        )
    }
}
