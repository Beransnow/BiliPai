package com.android.purebilibili.feature.dynamic

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.CommentRepository
import com.android.purebilibili.feature.comment.SentCommentReplyStores
import com.android.purebilibili.feature.video.viewmodel.SubReplySortMode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the real send callback, subject lifecycle and paging consumers, not policy forwarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class DynamicCommentSendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val identities = MutableStateFlow(AccountSessionIdentity(101L, 1L))
    private val root = ReplyItem(
        rpid = 10L, count = 2, rcount = 2,
        replies = listOf(ReplyItem(rpid = 20L, root = 10L, parent = 10L)),
    )
    private val otherRoot = ReplyItem(rpid = 110L)
    private val subject = dynamic("1000", 3000L)
    private val otherSubject = dynamic("2000", 4000L)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(SettingsManager, TokenManager, CommentRepository, NetworkModule, AppDatabase.Companion, Logger)
        every { TokenManager.accountIdentity } returns identities
        every { TokenManager.csrfCache } returns "csrf"
        SentCommentReplyStores.clear()
        every { SettingsManager.getIncrementalTimelineRefresh(any()) } returns flowOf(false)
        every { NetworkModule.api } returns mockk(relaxed = true)
        every { AppDatabase.getDatabase(any()) } returns mockk(relaxed = true)
        every { Logger.d(any(), any<String>()) } just Runs
        every { Logger.w(any(), any<String>(), any()) } just Runs
        every { Logger.e(any(), any<String>(), any()) } just Runs
        coEvery { CommentRepository.getCommentCountForSubject(any(), any()) } returns Result.success(3)
        coEvery {
            CommentRepository.getMobileCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            Result.success(ReplyData(
                replies = listOf(if (firstArg<Long>() == 3000L) root else otherRoot),
                cursor = ReplyCursor(allCount = 3, isEnd = true),
            ))
        }
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(threadPage(listOf(ReplyItem(rpid = 20L, root = 10L)), "page2"))
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(ReplyItem(rpid = 40L))
    }

    @AfterTest
    fun tearDown() {
        SentCommentReplyStores.clear()
        unmockkObject(SettingsManager, TokenManager, CommentRepository, NetworkModule, AppDatabase.Companion, Logger)
        Dispatchers.resetMain()
    }

    @Test
    fun `main list reply callback fills omitted subject fields and immediately updates collapsed preview`() = runTest(dispatcher) {
        withViewModel { owner ->
            owner.openCommentSheet(subject)
            runCurrent()
            owner.startCommentReply(owner.commentUiState.value.comments.single())
            var success: Boolean? = null
            owner.postComment(subject.id_str, "sent from main") { result, _ -> success = result }
            runCurrent()

            assertEquals(true, success)
            val visibleRoot = owner.commentUiState.value.comments.single()
            val sent = visibleRoot.replies.orEmpty().first()
            assertEquals(listOf(40L, 20L), visibleRoot.replies.orEmpty().map { it.rpid })
            assertEquals(3, visibleRoot.rcount)
            assertEquals(4, owner.commentUiState.value.totalCount)
            assertEquals(3000L, sent.oid)
            assertEquals(11, sent.replyType)
            assertEquals(10L, sent.root)
            assertEquals(10L, sent.parent)
            assertFalse(owner.subReplyState.value.visible)
            coVerify(exactly = 1) {
                CommentRepository.addCommentForSubject(3000L, 11, "sent from main", 10L, 10L, emptyList(), false)
            }
            // No successful-send reload is needed to make the item visible.
            coVerify(exactly = 1) {
                CommentRepository.getMobileCommentsForSubject(3000L, 11, any(), any(), any(), any(), any())
            }
            owner.refreshComments()
            runCurrent()
            assertEquals(40L, owner.commentUiState.value.comments.single().replies.orEmpty().first().rpid)
            owner.closeCommentSheet()
            owner.openCommentSheet(subject)
            runCurrent()
            assertEquals(40L, owner.commentUiState.value.comments.single().replies.orEmpty().first().rpid)
        }
    }

    @Test
    fun `opening a newly sent dynamic reply does not require its unpropagated server target`() = runTest(dispatcher) {
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            if (arg<Long>(5) == 40L) Result.failure(IllegalStateException("Target not propagated"))
            else Result.success(threadPage(listOf(ReplyItem(rpid = 20L, root = 10L)), ""))
        }
        withViewModel { owner ->
            owner.openCommentSheet(subject)
            runCurrent()
            owner.startCommentReply(root)
            owner.postComment(subject.id_str, "new reply") { _, _ -> }
            runCurrent()
            owner.openSubReply(owner.commentUiState.value.comments.single(), 40L)
            assertEquals(listOf(40L), owner.subReplyState.value.items.map { it.rpid })
            runCurrent()
            assertNull(owner.subReplyState.value.error)
            assertEquals(listOf(20L, 40L), owner.subReplyState.value.items.map { it.rpid })
        }
    }

    @Test
    fun `real thread send follows every subsequent page and echoed reply takes server hot rank`() = runTest(dispatcher) {
        val first = ReplyItem(rpid = 20L, root = 10L, parent = 10L)
        val second = ReplyItem(rpid = 30L, root = 10L, parent = 10L)
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            val data = when (arg<String?>(4)) {
                "page2" -> threadPage(listOf(second), "page3")
                "page3" -> threadPage(listOf(ReplyItem(rpid = 40L, root = 10L, like = 7)), "")
                else -> threadPage(listOf(first), "page2")
            }
            Result.success(data)
        }
        withViewModel { owner ->
            owner.openCommentSheet(subject)
            runCurrent()
            owner.openSubReply(owner.commentUiState.value.comments.single())
            runCurrent()
            assertEquals(listOf(20L), owner.subReplyState.value.items.map { it.rpid })
            owner.startCommentReply(first)
            owner.postComment(subject.id_str, "thread send") { _, _ -> }
            runCurrent()
            assertEquals(listOf(20L, 40L), owner.subReplyState.value.items.map { it.rpid })
            assertEquals(20L, owner.subReplyState.value.items.last().parent)
            owner.loadMoreSubReplies()
            runCurrent()
            assertEquals(listOf(20L, 30L, 40L), owner.subReplyState.value.items.map { it.rpid })
            owner.loadMoreSubReplies()
            runCurrent()
            assertEquals(listOf(20L, 30L, 40L), owner.subReplyState.value.items.map { it.rpid })
            assertEquals(7, owner.subReplyState.value.items.last().like)
            assertTrue(owner.subReplyState.value.isEnd)

            val echoed = ReplyItem(rpid = 40L, root = 10L, like = 100)
            coEvery {
                CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
            } returns Result.success(threadPage(listOf(echoed, first, second), ""))
            owner.setSubReplySortMode(SubReplySortMode.HOT)
            runCurrent()
            assertEquals(listOf(40L, 20L, 30L), owner.subReplyState.value.items.map { it.rpid })
            assertEquals(100, owner.subReplyState.value.items.first().like)
            assertEquals(owner.subReplyState.value.items, owner.subReplyState.value.baseItems)
            owner.closeSubReply()
            owner.openSubReply(owner.commentUiState.value.comments.single())
            runCurrent()
            assertEquals(listOf(40L, 20L, 30L), owner.subReplyState.value.items.map { it.rpid })
        }
    }

    @Test
    fun `delayed callback cannot move a reply into a different dynamic or its composer`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<ReplyItem?>>()
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers { pending.await() }
        withViewModel { owner ->
            owner.openCommentSheet(subject)
            runCurrent()
            owner.startCommentReply(root)
            var callbackCount = 0
            owner.postComment(subject.id_str, "old subject") { _, _ -> callbackCount++ }
            runCurrent()
            owner.openCommentSheet(otherSubject)
            runCurrent()
            owner.startCommentReply(otherRoot)
            val activeSession = owner.commentComposerDrafts.value.sessionId
            pending.complete(Result.success(ReplyItem(rpid = 40L)))
            runCurrent()

            assertEquals(0, callbackCount)
            assertEquals(otherSubject.id_str, owner.commentUiState.value.dynamicId)
            assertEquals(listOf(110L), owner.commentUiState.value.comments.map { it.rpid })
            assertTrue(owner.commentUiState.value.comments.single().replies.orEmpty().isEmpty())
            assertEquals(110L, owner.commentReplyTarget.value?.rootRpid)
            assertEquals(activeSession, owner.commentComposerDrafts.value.sessionId)
            assertTrue(owner.commentComposerVisible.value)
            owner.openCommentSheet(subject)
            runCurrent()
            assertEquals(listOf(20L), owner.commentUiState.value.comments.single().replies.orEmpty().map { it.rpid })
        }
    }

    @Test
    fun `account observer rejects cancellation resistant old send and clears same subject overlay`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<ReplyItem?>>()
        withViewModel { owner ->
            owner.openCommentSheet(subject)
            runCurrent()
            owner.startCommentReply(root)
            owner.postComment(subject.id_str, "previous successful send") { _, _ -> }
            runCurrent()
            assertEquals(40L, owner.commentUiState.value.comments.single().replies.orEmpty().first().rpid)

            coEvery {
                CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
            } coAnswers { withContext(NonCancellable) { pending.await() } }
            owner.startCommentReply(root)
            var callbackCount = 0
            owner.postComment(subject.id_str, "old account in flight") { _, _ -> callbackCount++ }
            runCurrent()
            identities.value = AccountSessionIdentity(202L, 2L)
            runCurrent()
            assertNull(owner.commentUiState.value.dynamicId)
            assertTrue(owner.commentUiState.value.comments.isEmpty())
            pending.complete(Result.success(ReplyItem(rpid = 50L)))
            runCurrent()
            owner.openCommentSheet(subject)
            runCurrent()

            assertEquals(0, callbackCount)
            assertEquals(listOf(20L), owner.commentUiState.value.comments.single().replies.orEmpty().map { it.rpid })
            assertEquals(3, owner.commentUiState.value.totalCount)
            assertFalse(owner.isSendingComment.value)
            owner.openSubReply(owner.commentUiState.value.comments.single())
            runCurrent()
            assertEquals(listOf(20L), owner.subReplyState.value.items.map { it.rpid })
        }
    }

    @Test
    fun `another dynamic ViewModel restores subject sends after owner closes and changes target`() = runTest(dispatcher) {
        withViewModel { firstOwner ->
            firstOwner.openCommentSheet(subject)
            runCurrent()
            firstOwner.startCommentReply(root)
            firstOwner.postComment(subject.id_str, "cross owner send") { _, _ -> }
            runCurrent()
            firstOwner.closeCommentSheet()
            firstOwner.openCommentSheet(otherSubject)
            runCurrent()
            assertTrue(firstOwner.commentUiState.value.comments.single().replies.orEmpty().isEmpty())
            withViewModel { nextOwner ->
                nextOwner.openCommentSheet(subject)
                runCurrent()
                assertEquals(listOf(40L, 20L),
                    nextOwner.commentUiState.value.comments.single().replies.orEmpty().map { it.rpid })
                nextOwner.openSubReply(nextOwner.commentUiState.value.comments.single())
                runCurrent()
                assertEquals(listOf(20L, 40L), nextOwner.subReplyState.value.items.map { it.rpid })
            }
        }
    }

    @Test
    fun `shared deletion tombstones filter stale detail pages and routed roots in another ViewModel`() = runTest(dispatcher) {
        coEvery { CommentRepository.deleteCommentForSubject(any(), any(), any()) } returns Result.success(Unit)
        withViewModel { firstOwner ->
            firstOwner.openCommentSheet(subject)
            runCurrent()
            firstOwner.startCommentReply(root)
            firstOwner.postComment(subject.id_str, "delete across owners") { _, _ -> }
            runCurrent()
            firstOwner.deleteDynamicComment(40L) { _, _ -> }
            runCurrent()
            coEvery {
                CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
            } returns Result.success(threadPage(
                listOf(ReplyItem(rpid = 20L, root = 10L), ReplyItem(rpid = 40L, root = 10L)), "",
            ))
            withViewModel { nextOwner ->
                nextOwner.openCommentSheet(subject)
                runCurrent()
                nextOwner.openSubReply(nextOwner.commentUiState.value.comments.single())
                runCurrent()
                assertEquals(listOf(20L), nextOwner.subReplyState.value.items.map { it.rpid })
                assertEquals(listOf(20L),
                    nextOwner.commentUiState.value.comments.single().replies.orEmpty().map { it.rpid })
                firstOwner.deleteDynamicComment(10L) { _, _ -> }
                runCurrent()
                nextOwner.refreshSubReplies()
                runCurrent()
                assertFalse(nextOwner.subReplyState.value.visible)
                nextOwner.refreshComments()
                runCurrent()
                assertTrue(nextOwner.commentUiState.value.comments.isEmpty())
                assertFalse(nextOwner.openSubReplyFromRoute(10L))
            }
        }
    }

    private suspend fun TestScope.withViewModel(block: suspend TestScope.(DynamicViewModel) -> Unit) {
        val preferences = mockk<SharedPreferences>(relaxed = true)
        every { preferences.getString(any(), any()) } answers { secondArg<String?>() }
        every { preferences.getStringSet(any(), any()) } answers { secondArg<Set<String>?>() }
        val application = mockk<Application>(relaxed = true)
        every { application.applicationContext } returns application
        every { application.getSharedPreferences(any(), any()) } returns preferences
        val owner = DynamicViewModel(application)
        try {
            runCurrent()
            block(owner)
        } finally {
            owner.viewModelScope.cancel()
            runCurrent()
        }
    }

    private fun dynamic(id: String, oid: Long) = DynamicItem(
        id_str = id, type = "DYNAMIC_TYPE_DRAW",
        basic = DynamicBasic(comment_id_str = oid.toString(), comment_type = 11),
    )

    private fun threadPage(items: List<ReplyItem>, offset: String) = ReplyData(
        replies = items,
        cursor = ReplyCursor(allCount = 3, isEnd = offset.isEmpty()),
        grpcNextOffset = offset,
    )
}
