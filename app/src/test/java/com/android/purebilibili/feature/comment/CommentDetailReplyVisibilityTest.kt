package com.android.purebilibili.feature.comment

import androidx.lifecycle.ViewModelStore
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.ReplyContent
import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.CommentRepository
import com.android.purebilibili.feature.video.viewmodel.SubReplySortMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CommentDetailReplyVisibilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val accounts = MutableStateFlow(AccountSessionIdentity(1L, 1L))
    private lateinit var model: CommentDetailViewModel
    private val root = ReplyItem(rpid = 10L, oid = 1001L, count = 6, rcount = 6)
    private val first = ReplyItem(rpid = 20L, root = 10L, parent = 10L)
    private val second = ReplyItem(rpid = 30L, root = 10L, parent = 10L)
    private val sent = ReplyItem(rpid = 50L, content = ReplyContent(message = "local"))
    private var remotePage: (Long, Int, Long, Int, String?, Long) -> ReplyData = { oid, _, rootId, _, _, _ ->
        page(listOf(first, second), root = root.copy(rpid = rootId, oid = oid))
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(CommentRepository, TokenManager)
        every { TokenManager.accountIdentity } returns accounts
        SentCommentReplyStores.clear()
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            Result.success(remotePage(arg(0), arg(1), arg(2), arg(3), arg(4), arg(5)))
        }
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(sent)
        coEvery { CommentRepository.deleteCommentForSubject(any(), any(), any()) } returns Result.success(Unit)
        model = CommentDetailViewModel()
        store.put("detail", model)
        model.loadInitial(1001L, root.rpid)
        dispatcher.scheduler.runCurrent()
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        SentCommentReplyStores.clear()
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `opening a just sent BGM reply shows its cached root before the unreadable target reaches the server`() = runTest(dispatcher) {
        val bgmRoot = root.copy(replyType = 47)
        val local = sent.copy(root = 10L, parent = 10L, oid = 1001L, replyType = 47)
        SentCommentReplyStores.forSubject(1001L, 47).record(local, listOf(bgmRoot))
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            if (arg<Long>(5) != 0L) Result.failure(IllegalStateException("Target not propagated"))
            else Result.success(page(listOf(first, second), root = bgmRoot))
        }
        model.loadInitial(1001L, 10L, type = 47, targetReplyId = 50L)
        assertEquals(10L, model.subReplyState.value.rootReply?.rpid)
        assertEquals(listOf(50L), ids())
        runCurrent()
        assertNull(model.subReplyState.value.error)
        assertEquals(listOf(20L, 30L, 50L), ids())
        assertEquals(50L, model.subReplyState.value.targetReplyId)
    }

    @Test
    fun `a failed detail read does not hide an already accepted cross screen reply`() = runTest(dispatcher) {
        SentCommentReplyStores.forSubject(1001L, 47).record(
            sent.copy(root = 10L, parent = 10L, oid = 1001L, replyType = 47),
            listOf(root.copy(replyType = 47))
        )
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(IllegalStateException("offline"))
        model.loadInitial(1001L, 10L, type = 47, targetReplyId = 50L)
        runCurrent()
        assertEquals(10L, model.subReplyState.value.rootReply?.rpid)
        assertEquals(listOf(50L), ids())
        assertEquals("offline", model.subReplyState.value.error)
    }

    @Test
    fun `nested reply immediately appears at thread tail instead of beside its loaded parent`() = runTest(dispatcher) {
        model.showReplyInput(first)
        model.sendReply("local")
        runCurrent()

        assertEquals(listOf(20L, 30L, 50L), ids())
        assertEquals(10L, model.subReplyState.value.items.last().root)
        assertEquals(20L, model.subReplyState.value.items.last().parent)
        assertEquals(model.subReplyState.value.items, model.subReplyState.value.baseItems)
        assertFalse(model.isSending.value)
        assertFalse(model.showCommentInput.value)
    }

    @Test
    fun `unacknowledged replies remain after every subsequently loaded remote page in send order`() = runTest(dispatcher) {
        remotePage = { _, _, _, _, offset, _ ->
            when (offset) {
                null -> page(listOf(first), "page2")
                "page2" -> page(listOf(second), "page3")
                else -> page(listOf(ReplyItem(rpid = 40L, root = 10L)))
            }
        }
        model.refresh()
        runCurrent()
        send(first)
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(sent.copy(rpid = 51L))
        send(first)
        assertEquals(listOf(20L, 50L, 51L), ids())

        model.loadMore()
        runCurrent()
        assertEquals(listOf(20L, 30L, 50L, 51L), ids())
        model.loadMore()
        runCurrent()
        assertEquals(listOf(20L, 30L, 40L, 50L, 51L), ids())
        assertTrue(model.subReplyState.value.isEnd)
    }

    @Test
    fun `server acknowledgement uses remote object and position across pages without duplicates`() = runTest(dispatcher) {
        send(first)
        val serverReply = sent.copy(root = 10L, parent = 20L, like = 17, content = ReplyContent(message = "server"))
        remotePage = { _, _, _, _, offset, _ ->
            if (offset == null) page(listOf(second, serverReply), "more")
            else page(listOf(serverReply, first))
        }
        model.refresh()
        runCurrent()
        assertEquals(listOf(30L, 50L), ids())
        model.loadMore()
        runCurrent()

        assertEquals(listOf(30L, 50L, 20L), ids())
        assertEquals(serverReply, model.subReplyState.value.items[1])
        assertEquals(model.subReplyState.value.items, model.subReplyState.value.baseItems)
    }

    @Test
    fun `hot sorting keeps service ranking and confirmation content instead of the local overlay`() = runTest(dispatcher) {
        send(first)
        val serverReply = sent.copy(root = 10L, parent = 20L, like = 99)
        val requestedModes = mutableListOf<Int>()
        remotePage = { _, _, _, mode, _, _ ->
            requestedModes += mode
            page(listOf(second, serverReply, first))
        }
        model.setSortMode(SubReplySortMode.HOT)
        runCurrent()

        assertEquals(SubReplySortMode.HOT, model.subReplyState.value.sortMode)
        assertEquals(listOf(SubReplySortMode.HOT.apiMode), requestedModes)
        assertEquals(listOf(30L, 50L, 20L), ids())
        assertEquals(serverReply, model.subReplyState.value.items[1])
    }

    @Test
    fun `stale refresh and switching sort preserve a pending local tail without inflating count`() = runTest(dispatcher) {
        send(first)
        val count = model.subReplyState.value.totalCount
        model.refresh()
        runCurrent()
        assertEquals(listOf(20L, 30L, 50L), ids())
        model.setSortMode(SubReplySortMode.HOT)
        runCurrent()
        assertEquals(listOf(20L, 30L, 50L), ids())
        assertEquals(count, model.subReplyState.value.totalCount)
    }

    @Test
    fun `deleting local reply is idempotent and stale pages cannot resurrect it`() = runTest(dispatcher) {
        send(first)
        model.deleteComment(sent.rpid)
        runCurrent()
        val count = model.subReplyState.value.totalCount
        model.deleteComment(sent.rpid)
        runCurrent()
        assertEquals(count, model.subReplyState.value.totalCount)
        assertEquals(listOf(20L, 30L), ids())

        remotePage = { _, _, _, _, _, _ -> page(listOf(first, sent.copy(root = 10L), second)) }
        model.refresh()
        runCurrent()
        assertEquals(listOf(20L, 30L), ids())
        assertEquals(model.subReplyState.value.items, model.subReplyState.value.baseItems)
    }

    @Test
    fun `conversation mode preserves the existing unfiltered item contract when sending and paging`() = runTest(dispatcher) {
        remotePage = { _, _, _, _, offset, _ ->
            if (offset == null) page(listOf(first), "more") else page(listOf(second))
        }
        model.refresh()
        runCurrent()
        model.openConversation(first)
        send(first)
        model.loadMore()
        runCurrent()

        assertEquals(first, model.subReplyState.value.conversationAnchor)
        assertEquals(listOf(20L, 30L, 50L), ids())
        assertEquals(model.subReplyState.value.items, model.subReplyState.value.baseItems)
        model.closeConversation()
        assertNull(model.subReplyState.value.conversationAnchor)
        assertEquals(listOf(20L, 30L, 50L), ids())
    }

    @Test
    fun `subject root and type changes reject a late send and clear the prior local thread`() = runTest(dispatcher) {
        for ((oid, rootId, type) in listOf(Triple(2002L, 10L, 1), Triple(1001L, 11L, 1), Triple(1001L, 10L, 17))) {
            model.loadInitial(1001L, 10L)
            runCurrent()
            val response = CompletableDeferred<Result<ReplyItem?>>()
            coEvery {
                CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
            } coAnswers { response.await() }
            model.showReplyInput(first)
            model.sendReply("pending")
            runCurrent()
            model.loadInitial(oid, rootId, type)
            runCurrent()
            response.complete(Result.success(sent))
            runCurrent()

            assertFalse(ids().contains(sent.rpid))
            assertEquals(rootId, model.subReplyState.value.rootReply?.rpid)
            assertFalse(model.isSending.value)
        }
    }

    @Test
    fun `closing and reopening the same root rejects a result from the old composer session`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<ReplyItem?>>()
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers { response.await() }
        model.showReplyInput(first)
        model.sendReply("pending")
        runCurrent()
        model.close()
        assertFalse(model.subReplyState.value.visible)
        model.loadInitial(1001L, 10L)
        runCurrent()
        response.complete(Result.success(sent))
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
        assertFalse(model.isSending.value)
        assertFalse(model.showCommentInput.value)
    }

    @Test
    fun `changing routed target on the same root starts a new session and ignores prior sends`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<ReplyItem?>>()
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers { response.await() }
        model.showReplyInput(first)
        model.sendReply("pending")
        runCurrent()
        model.loadInitial(1001L, 10L, targetReplyId = 30L)
        runCurrent()
        response.complete(Result.success(sent))
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
        assertEquals(30L, model.subReplyState.value.targetReplyId)
        assertFalse(model.isSending.value)
    }

    @Test
    fun `account switch clears sent overlays and drops a late result even after switching back`() = runTest(dispatcher) {
        send(first)
        val response = CompletableDeferred<Result<ReplyItem?>>()
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers { response.await() }
        model.showReplyInput(second)
        model.sendReply("pending")
        runCurrent()
        accounts.value = AccountSessionIdentity(2L, 2L)
        runCurrent()
        assertEquals(listOf(20L, 30L), ids())
        accounts.value = AccountSessionIdentity(1L, 3L)
        runCurrent()
        response.complete(Result.success(sent.copy(rpid = 51L)))
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
        assertFalse(model.isSending.value)
        assertFalse(model.showCommentInput.value)
    }

    @Test
    fun `deleting root invalidates its loading and sending sessions`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<ReplyItem?>>()
        coEvery {
            CommentRepository.addCommentForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers { response.await() }
        model.showReplyInput(first)
        model.sendReply("pending")
        runCurrent()
        model.deleteComment(root.rpid)
        runCurrent()
        response.complete(Result.success(sent))
        runCurrent()

        assertTrue(model.subReplyState.value.items.isEmpty())
        assertNull(model.subReplyState.value.rootReply)
        assertEquals("评论已删除", model.subReplyState.value.error)
        assertFalse(model.isSending.value)
    }

    @Test
    fun `reopening the same thread retains successful sends until remote propagation`() = runTest(dispatcher) {
        send(first)
        val count = model.subReplyState.value.totalCount
        model.close()
        model.loadInitial(1001L, 10L)
        runCurrent()

        assertEquals(listOf(20L, 30L, 50L), ids())
        assertEquals(count, model.subReplyState.value.totalCount)
        assertFalse(model.showCommentInput.value)
        assertFalse(model.isSending.value)
    }

    @Test
    fun `reopening retains deletion tombstones even when service still returns deleted reply`() = runTest(dispatcher) {
        send(first)
        model.deleteComment(sent.rpid)
        runCurrent()
        model.close()
        remotePage = { _, _, _, _, _, _ -> page(listOf(first, sent.copy(root = 10L), second)) }
        model.loadInitial(1001L, 10L)
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
    }

    @Test
    fun `switching accounts while closed does not reopen the screen or retain another accounts sends`() = runTest(dispatcher) {
        send(first)
        model.close()
        accounts.value = AccountSessionIdentity(2L, 2L)
        runCurrent()
        assertFalse(model.subReplyState.value.visible)
        model.loadInitial(1001L, 10L)
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
    }

    @Test
    fun `reopening a deleted root cannot revive its retained remote replies`() = runTest(dispatcher) {
        model.deleteComment(root.rpid)
        runCurrent()
        model.close()
        model.loadInitial(1001L, 10L)
        runCurrent()

        assertNull(model.subReplyState.value.rootReply)
        assertTrue(ids().isEmpty())
        assertEquals("评论已删除", model.subReplyState.value.error)
    }

    @Test
    fun `late deletion cannot remove a matching reply id from a different thread`() = runTest(dispatcher) {
        val deletion = CompletableDeferred<Result<Unit>>()
        coEvery { CommentRepository.deleteCommentForSubject(any(), any(), any()) } coAnswers { deletion.await() }
        model.deleteComment(first.rpid)
        runCurrent()
        model.loadInitial(2002L, 10L)
        runCurrent()
        deletion.complete(Result.success(Unit))
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
        assertEquals(2002L, model.subReplyState.value.rootReply?.oid)
    }

    @Test
    fun `late refresh response cannot replace a newly selected subject`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<ReplyData>>()
        coEvery {
            CommentRepository.getMobileSortedSubCommentsForSubject(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            if (arg<Long>(0) == 1001L) {
                withContext(NonCancellable) { response.await() }
            } else {
                Result.success(page(listOf(first, second), root = root.copy(oid = arg(0))))
            }
        }
        model.refresh()
        runCurrent()
        model.loadInitial(2002L, 10L)
        runCurrent()
        response.complete(Result.success(page(listOf(sent))))
        runCurrent()

        assertEquals(listOf(20L, 30L), ids())
        assertEquals(2002L, model.subReplyState.value.rootReply?.oid)
    }

    @Test
    fun `send response inherits captured non-video subject when server omits oid and type`() = runTest(dispatcher) {
        model.loadInitial(9001L, 10L, type = 33)
        runCurrent()
        send(first)

        val reply = model.subReplyState.value.items.last()
        assertEquals(sent.rpid, reply.rpid)
        assertEquals(9001L, reply.oid)
        assertEquals(33, reply.replyType)
        assertEquals(10L, reply.root)
        assertEquals(20L, reply.parent)
    }

    @Test
    fun `BGM main preview sends seed fresh detail instances in chronological send order`() = runTest(dispatcher) {
        val subjectStore = SentCommentReplyStores.forSubject(9001L, 47)
        val bgmRoot = root.copy(oid = 9001L, replyType = 47)
        val firstSent = sent.copy(root = 10L, parent = 20L, oid = 9001L, replyType = 47)
        val secondSent = firstSent.copy(rpid = 51L)
        subjectStore.record(firstSent, listOf(bgmRoot))
        subjectStore.record(secondSent, subjectStore.merge(listOf(bgmRoot)))
        assertEquals(listOf(51L, 50L), subjectStore.merge(listOf(bgmRoot)).single().replies.orEmpty().map { it.rpid })

        repeat(2) { index ->
            val detail = CommentDetailViewModel()
            store.put("bgm-detail-$index", detail)
            detail.loadInitial(9001L, 10L, type = 47)
            runCurrent()

            assertEquals(listOf(20L, 30L, 50L, 51L), detail.subReplyState.value.items.map { it.rpid })
            assertEquals(8, detail.subReplyState.value.totalCount)
            assertEquals(47, detail.subReplyState.value.items.last().replyType)
            detail.close()
        }
    }

    @Test
    fun `detail sends update the shared main preview and deletion is visible to new instances`() = runTest(dispatcher) {
        send(first)
        val shared = SentCommentReplyStores.forSubject(1001L, 1)
        val updatedRoot = shared.merge(listOf(root)).single()
        assertEquals(sent.rpid, updatedRoot.replies.orEmpty().first().rpid)
        assertEquals(7, updatedRoot.rcount)
        model.deleteComment(sent.rpid)
        runCurrent()
        assertTrue(shared.merge(listOf(root)).single().replies.orEmpty().isEmpty())

        remotePage = { _, _, _, _, _, _ -> page(listOf(first, sent.copy(root = 10L), second)) }
        val freshDetail = CommentDetailViewModel()
        store.put("fresh-after-delete", freshDetail)
        freshDetail.loadInitial(1001L, 10L)
        runCurrent()
        assertEquals(listOf(20L, 30L), freshDetail.subReplyState.value.items.map { it.rpid })
    }

    @Test
    fun `returning from another root seeds only that roots shared successful sends`() = runTest(dispatcher) {
        send(first)
        model.loadInitial(1001L, 11L)
        runCurrent()
        assertEquals(listOf(20L, 30L), ids())
        model.loadInitial(1001L, 10L)
        runCurrent()

        assertEquals(listOf(20L, 30L, 50L), ids())
    }

    private fun send(target: ReplyItem) {
        model.showReplyInput(target)
        model.sendReply("local")
        dispatcher.scheduler.runCurrent()
    }

    private fun ids() = model.subReplyState.value.items.map { it.rpid }

    private fun page(items: List<ReplyItem>, next: String = "", root: ReplyItem = this.root) = ReplyData(
        root = root,
        replies = items,
        grpcNextOffset = next,
        cursor = ReplyCursor(isEnd = next.isEmpty())
    )
}
