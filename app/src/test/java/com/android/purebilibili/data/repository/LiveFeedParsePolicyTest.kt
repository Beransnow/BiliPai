package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.LiveFeedCard
import com.android.purebilibili.data.model.response.LiveFeedCardData
import com.android.purebilibili.data.model.response.LiveFeedIndexData
import com.android.purebilibili.data.model.response.LiveFeedModuleBlock
import com.android.purebilibili.data.model.response.LiveFeedRoomCard
import com.android.purebilibili.data.model.response.LiveRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFeedParsePolicyTest {

    @Test
    fun feedRoomCardMapsToLiveRoomWithSystemCover() {
        val room = LiveFeedRoomCard(
            roomid = 42,
            title = "Hello",
            uname = "UP",
            cover = "https://cover",
            systemCover = "https://frame",
            areaName = "娱乐",
            online = 123,
        ).toLiveRoom()

        assertEquals(42L, room.roomid)
        assertEquals("https://cover", room.cover)
        assertEquals("https://frame", room.systemCover)
        assertEquals("娱乐", room.areaName)
        assertEquals("https://frame", room.displayCover(preferFirstFrame = true))
        assertEquals("https://cover", room.displayCover(preferFirstFrame = false))
    }

    @Test
    fun displayCoverPrefersFirstFrameWhenRequested() {
        val room = LiveRoom(
            roomid = 1,
            cover = "cover",
            systemCover = "frame",
            keyframe = "key",
        )
        assertEquals("frame", room.displayCover(preferFirstFrame = true))
        assertEquals("cover", room.displayCover(preferFirstFrame = false))
    }

    @Test
    fun feedIndexDataContainsRoomFollowAndAreaCards() {
        val data = LiveFeedIndexData(
            cardList = listOf(
                LiveFeedCard(
                    cardType = "my_idol_v1",
                    cardData = LiveFeedCardData(
                        myIdolV1 = LiveFeedModuleBlock(
                            list = listOf(LiveFeedRoomCard(roomid = 7, title = "followed"))
                        )
                    )
                ),
                LiveFeedCard(
                    cardType = "area_entrance_v3",
                    cardData = LiveFeedCardData(
                        areaEntranceV3 = LiveFeedModuleBlock(
                            list = listOf(
                                LiveFeedRoomCard(
                                    title = "游戏",
                                    areaV2Id = 1,
                                    areaV2ParentId = 2,
                                )
                            )
                        )
                    )
                ),
                LiveFeedCard(
                    cardType = "small_card_v1",
                    cardData = LiveFeedCardData(
                        smallCardV1 = LiveFeedRoomCard(roomid = 9, title = "room")
                    )
                ),
            ),
            hasMore = 1,
        )

        val rooms = data.cardList.orEmpty().mapNotNull { card ->
            card.cardData?.smallCardV1?.takeIf { card.cardType == "small_card_v1" }
        }
        val follows = data.cardList.orEmpty().flatMap { card ->
            if (card.cardType == "my_idol_v1") card.cardData?.myIdolV1?.list.orEmpty() else emptyList()
        }
        val areas = data.cardList.orEmpty().flatMap { card ->
            if (card.cardType == "area_entrance_v3") card.cardData?.areaEntranceV3?.list.orEmpty() else emptyList()
        }

        assertEquals(1, rooms.size)
        assertEquals(9L, rooms.first().roomid)
        assertEquals(1, follows.size)
        assertEquals(7L, follows.first().roomid)
        assertEquals(1, areas.size)
        assertEquals("游戏", areas.first().title)
        assertTrue(data.hasMore == 1)
    }
}
