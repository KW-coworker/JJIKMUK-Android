package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.chat.presentation.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatFollowUpViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Targets : AnalysisTargetRepository {
        private val account = FamilyAccount(1, 1)
        override val state = MutableStateFlow(AnalysisTargetsState(account,
            selection = AnalysisTargetSelection(account, true, emptyList(), "self"), ready = true))
        fun choose(self: Boolean, ids: List<Long>) { state.value = state.value.copy(selection = AnalysisTargetSelection(account, self, ids, "changed")) }
        override suspend fun refresh(account: FamilyAccount?, confirmChanges: Boolean) = Unit
        override fun captureAccount() = state.value.account
        override suspend fun awaitSelection(account: FamilyAccount?) = state.value.selection?.takeIf { it.account == account }
        override fun isCurrent(selection: AnalysisTargetSelection) = state.value.selection == selection
        override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?) = false
    }
    private class History : ChatHistoryRepository {
        val saved = mutableListOf<String>()
        override fun observeConversations(): Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeConversations(query: String): Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeMessages(conversationId: String): Flow<List<ChatSavedMessage>> = flowOf(emptyList())
        override suspend fun createConversation(conversationId: String, title: String, preview: String) = Unit
        override suspend fun addMessage(conversationId: String, text: String, isMine: Boolean): Long { saved += text; return saved.size.toLong() }
        override suspend fun updateTitle(conversationId: String, title: String) = Unit
        override suspend fun updatePinned(conversationId: String, isPinned: Boolean) = Unit
        override suspend fun deleteConversation(conversationId: String) = Unit
        override suspend fun deleteConversations(conversationIds: List<String>) = Unit
    }
    private class Store(private val targets: Targets) : StoredChatStore {
        val calls = mutableListOf<StoredChatQuestion>()
        val answers = ArrayDeque<ChatResponse>()
        var gate: CompletableDeferred<Unit>? = null
        var sendFailure: AuthFailure? = null
        var oldTurns = emptyList<StoredChatTurn>()
        override fun captureAccount() = targets.captureAccount()
        override suspend fun create(account: FamilyAccount, clientId: String, title: String): AuthResult<ChatConversation> =
            AuthResult.Success(ChatConversation("12", title, "", false, 0))
        override suspend fun rooms(account: FamilyAccount, page: Int) = AuthResult.Success(StoredChatRooms(emptyList(), page, false))
        override suspend fun room(account: FamilyAccount, id: Long) = AuthResult.Success(ChatConversation(id.toString(), "서버 대화", "", false, 0))
        override suspend fun turns(account: FamilyAccount, id: Long, after: Long) =
            AuthResult.Success(StoredChatTurns(oldTurns, oldTurns.lastOrNull()?.id ?: after, false))
        override suspend fun turn(account: FamilyAccount, id: Long, clientId: String): AuthResult<StoredChatTurn> =
            AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404))
        override suspend fun send(account: FamilyAccount, question: StoredChatQuestion): AuthResult<StoredChatTurn> {
            calls += question
            gate?.await()
            sendFailure?.let { return AuthResult.Failure(it) }
            return AuthResult.Success(StoredChatTurn(calls.size.toLong(), question.clientMessageId, question.message, question.barcode,
                "SUCCEEDED", if (answers.isEmpty()) ChatResponse("서버 답변") else answers.removeFirst()))
        }
        override suspend fun delete(account: FamilyAccount, id: Long) = AuthResult.Success(Unit)
    }
    private val chips = listOf("이거 먹어도 돼?", "대두 들어 있어?", "나트륨이 얼마야?")
    private fun answer(questions: List<String> = chips, candidates: List<ChatProductCandidate> = emptyList()) =
        ChatResponse("새우깡에는 밀가루가 들어 있어요.", candidates, recommendedQuestions = questions)
    private fun setUp(): Triple<Targets, Store, ChatViewModel> {
        val targets = Targets(); val store = Store(targets)
        return Triple(targets, store, ChatViewModel(store, History(), targets))
    }

    @Test fun `successful answer fills suggested questions`() = runTest {
        val (_, store, vm) = setUp()
        store.answers.add(answer())
        vm.start(null, "새우깡 성분 알려줘"); runCurrent()
        assertEquals(chips, vm.uiState.value.suggestedQuestions)
        vm.finishTyping()
        assertFalse(vm.uiState.value.messages.last().isMine)
    }

    @Test fun `sending a new message clears suggestions while waiting`() = runTest {
        val (_, store, vm) = setUp()
        store.answers.add(answer())
        vm.start(null, "새우깡 성분 알려줘"); runCurrent()
        store.gate = CompletableDeferred()
        vm.sendMessage("다른 질문"); runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
        store.gate!!.complete(Unit); runCurrent()
    }

    @Test fun `error clears suggestions`() = runTest {
        val (_, store, vm) = setUp()
        store.answers.add(answer())
        vm.start(null, "새우깡 성분 알려줘"); runCurrent()
        store.sendFailure = AuthFailure(AuthFailureKind.Server, 400, "CHAT_REQUEST_FAILED")
        vm.sendMessage("나트륨이 얼마야?"); runCurrent()
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
    }

    @Test fun `candidate response does not show suggestions`() = runTest {
        val (_, store, vm) = setUp()
        store.answers.add(answer(candidates = listOf(ChatProductCandidate("새우깡 90G", "농심", "880", null, emptyList(), null))))
        vm.start(null, "새우깡 먹어도 돼?"); runCurrent()
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
    }

    @Test fun `retry clears stale suggestions and shows new ones after success`() = runTest {
        val (_, store, vm) = setUp()
        store.sendFailure = AuthFailure(AuthFailureKind.Server, 400, "CHAT_REQUEST_FAILED")
        vm.start(null, "새우깡 성분 알려줘"); runCurrent()
        assertNotNull(vm.uiState.value.error)
        store.sendFailure = null; store.gate = CompletableDeferred(); store.answers.add(answer(listOf("당류는?")))
        vm.retry(); runCurrent()
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
        store.gate!!.complete(Unit); runCurrent()
        assertEquals(listOf("당류는?"), vm.uiState.value.suggestedQuestions)
    }

    @Test fun `target change clears suggestions`() = runTest {
        val (targets, store, vm) = setUp()
        store.answers.add(answer())
        vm.start(null, "새우깡 성분 알려줘"); runCurrent()
        targets.choose(false, listOf(41)); runCurrent()
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
    }

    @Test fun `chip question goes through sendMessage as user bubble and server message`() = runTest {
        val (_, store, vm) = setUp()
        store.answers.add(answer())
        vm.start(null, "새우깡 성분 알려줘"); runCurrent(); vm.finishTyping()
        val chip = vm.uiState.value.suggestedQuestions[2]
        vm.sendMessage(chip); runCurrent()
        assertEquals(chip, store.calls.last().message)
        assertEquals(1, vm.uiState.value.messages.count { it.isMine && it.text == chip })
    }

    @Test fun `chip after picking product keeps same barcode`() = runTest {
        val (_, store, vm) = setUp()
        val candidate = ChatProductCandidate("새우깡 90G", "농심", "8801043014809", null, emptyList(), null)
        store.answers.add(ChatResponse("어떤 상품인가요?", listOf(candidate)))
        store.answers.add(answer())
        vm.start(null, "새우깡 먹어도 돼?"); runCurrent(); vm.finishTyping(); advanceUntilIdle()
        vm.selectProductCandidate(candidate); runCurrent(); vm.finishTyping()
        vm.sendMessage(vm.uiState.value.suggestedQuestions.first()); runCurrent()
        assertEquals("8801043014809", store.calls.last().barcode)
    }

    @Test fun `reopened conversation does not restore suggestions`() = runTest {
        val (_, store, vm) = setUp()
        store.oldTurns = listOf(StoredChatTurn(1, "c1", "새우깡 성분 알려줘", null, "SUCCEEDED", answer()))
        vm.start("12", ""); runCurrent()
        assertEquals(2, vm.uiState.value.messages.size)
        assertTrue(vm.uiState.value.suggestedQuestions.isEmpty())
    }
}
