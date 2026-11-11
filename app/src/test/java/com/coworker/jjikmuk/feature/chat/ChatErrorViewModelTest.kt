package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.chat.presentation.ChatViewModel
import com.coworker.jjikmuk.feature.chat.presentation.chatErrorFor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatErrorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Targets : AnalysisTargetRepository {
        private val account = FamilyAccount(1, 1)
        override val state = MutableStateFlow(AnalysisTargetsState(account,
            selection = AnalysisTargetSelection(account, true, emptyList(), "self"), ready = true))
        var loggedIn = true
        var selectionAvailable = true
        override fun captureAccount() = state.value.account.takeIf { loggedIn }
        override suspend fun awaitSelection(account: FamilyAccount?): AnalysisTargetSelection? =
            state.value.selection?.takeIf { selectionAvailable && it.account == account }
        override fun isCurrent(selection: AnalysisTargetSelection) = state.value.selection == selection
        override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?) = false
    }
    private class History : ChatHistoryRepository {
        val saved = mutableListOf<Pair<String, Boolean>>()
        override fun observeConversations(): Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeConversations(query: String): Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeMessages(conversationId: String): Flow<List<ChatSavedMessage>> = flowOf(emptyList())
        override suspend fun createConversation(conversationId: String, title: String, preview: String) = Unit
        override suspend fun addMessage(conversationId: String, text: String, isMine: Boolean): Long { saved += text to isMine; return saved.size.toLong() }
        override suspend fun updateTitle(conversationId: String, title: String) = Unit
        override suspend fun updatePinned(conversationId: String, isPinned: Boolean) = Unit
        override suspend fun deleteConversation(conversationId: String) = Unit
        override suspend fun deleteConversations(conversationIds: List<String>) = Unit
    }
    private class Chat : ChatRepository {
        val calls = mutableListOf<Pair<String, ChatProductCandidate?>>()
        val responses = ArrayDeque<ChatResponse>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun sendMessage(message: String, selectedProduct: ChatProductCandidate?, selection: AnalysisTargetSelection?): ChatResponse {
            calls += message to selectedProduct
            withContext(NonCancellable) { gate?.await() }
            return if (responses.isEmpty()) ChatResponse("서버 답변") else responses.removeFirst()
        }
    }
    private fun failure(code: String) = ChatResponse("서버 오류 문구", errorCode = code)

    @Test fun `failure shows error state without adding error text to messages or history`() = runTest {
        val history = History(); val chat = Chat().apply { responses.add(failure("NETWORK_UNAVAILABLE")) }
        val vm = ChatViewModel(chat, history, Targets())
        vm.start(null, "질문"); runCurrent()
        val state = vm.uiState.value
        assertEquals("인터넷에 연결되지 않았어요", state.error!!.title)
        assertTrue(state.error!!.retryable)
        assertEquals(listOf("질문"), state.messages.map { it.text })
        assertEquals(listOf("질문" to true), history.saved)
        assertFalse(state.isLoading)
    }

    @Test fun `retry resends same request without duplicating user bubble or saved user message`() = runTest {
        val history = History()
        val product = ChatProductCandidate("포키", "해태", "880", null, emptyList(), null)
        val chat = Chat().apply {
            responses.add(ChatResponse("선택해 주세요", listOf(product)))
            responses.add(failure("CHAT_REQUEST_FAILED"))
            responses.add(failure("CHAT_REQUEST_FAILED"))
        }
        val vm = ChatViewModel(chat, history, Targets())
        vm.start(null, "포키 먹어도 돼?"); runCurrent()
        vm.selectProductCandidate(product); runCurrent()
        vm.retry(); runCurrent()
        assertEquals(3, chat.calls.size)
        assertEquals(chat.calls[1], chat.calls[2])
        assertEquals("포키 먹어도 돼?", chat.calls[2].first)
        assertEquals(1, vm.uiState.value.messages.count { it.text == "포키" && it.isMine })
        assertEquals(1, history.saved.count { it == ("포키" to true) })
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `retry shows retrying state and ignores repeated taps`() = runTest {
        val chat = Chat().apply { responses.add(failure("NETWORK_UNAVAILABLE")) }
        val vm = ChatViewModel(chat, History(), Targets())
        vm.start(null, "질문"); runCurrent()
        chat.gate = CompletableDeferred()
        vm.retry(); vm.retry(); runCurrent()
        assertTrue(vm.uiState.value.error!!.isRetrying)
        assertEquals(2, chat.calls.size)
        chat.gate!!.complete(Unit); runCurrent()
        assertNull(vm.uiState.value.error)
    }

    @Test fun `successful retry clears error and adds answer as bot bubble and history`() = runTest {
        val history = History(); val chat = Chat().apply { responses.add(failure("NETWORK_UNAVAILABLE")) }
        val vm = ChatViewModel(chat, history, Targets())
        vm.start(null, "질문"); runCurrent()
        vm.retry(); runCurrent()
        val state = vm.uiState.value
        assertNull(state.error)
        assertEquals(listOf("질문" to true, "서버 답변" to false), state.messages.map { it.text to it.isMine })
        assertEquals(listOf("질문" to true, "서버 답변" to false), history.saved)
    }

    @Test fun `selection unavailable failure is retryable and saves user message once after recovery`() = runTest {
        val targets = Targets().apply { selectionAvailable = false }
        val history = History(); val chat = Chat()
        val vm = ChatViewModel(chat, history, targets)
        vm.start(null, "질문"); runCurrent()
        assertEquals("분석 대상을 불러오지 못했어요", vm.uiState.value.error!!.title)
        assertTrue(chat.calls.isEmpty())
        targets.selectionAvailable = true
        vm.retry(); runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(listOf("질문" to true, "서버 답변" to false), history.saved)
    }

    @Test fun `non retryable errors cannot be retried`() = runTest {
        for (code in listOf("ANALYSIS_SELECTION_CHANGED", "FAMILY_NOT_FOUND", "AUTH_TOKEN_EXPIRED")) {
            val chat = Chat().apply { responses.add(failure(code)) }
            val vm = ChatViewModel(chat, History(), Targets())
            vm.start(null, "질문"); runCurrent()
            assertFalse(code, vm.uiState.value.error!!.retryable)
            vm.retry(); runCurrent()
            assertEquals(code, 1, chat.calls.size)
        }
    }

    @Test fun `missing login shows login error without sending`() = runTest {
        val chat = Chat(); val vm = ChatViewModel(chat, History(), Targets().apply { loggedIn = false })
        vm.start(null, "질문"); runCurrent()
        assertEquals("다시 로그인해 주세요", vm.uiState.value.error!!.title)
        assertFalse(vm.uiState.value.error!!.retryable)
        assertTrue(chat.calls.isEmpty())
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test fun `new question clears previous error`() = runTest {
        val chat = Chat().apply { responses.add(failure("CHAT_REQUEST_FAILED")) }
        val vm = ChatViewModel(chat, History(), Targets())
        vm.start(null, "질문"); runCurrent()
        assertNotNull(vm.uiState.value.error)
        vm.sendMessage("다른 질문"); runCurrent()
        assertNull(vm.uiState.value.error)
        vm.retry(); runCurrent()
        assertEquals(2, chat.calls.size)
    }

    @Test fun `target change clears error`() = runTest {
        val targets = Targets()
        val chat = Chat().apply { responses.add(failure("NETWORK_UNAVAILABLE")) }
        val vm = ChatViewModel(chat, History(), targets)
        vm.start(null, "질문"); runCurrent()
        val account = targets.state.value.account!!
        targets.state.value = targets.state.value.copy(selection = AnalysisTargetSelection(account, false, listOf(41), "changed"))
        runCurrent()
        assertNull(vm.uiState.value.error)
    }

    @Test fun `error codes map to specified copy and retry policy`() {
        data class Expected(val title: String, val description: String, val retryable: Boolean)
        val network = Expected("인터넷에 연결되지 않았어요", "연결을 확인한 뒤 다시 시도해 주세요.", true)
        val server = Expected("답변을 가져오지 못했어요", "잠시 후 다시 시도해 주세요.", true)
        val unavailable = Expected("분석 대상을 불러오지 못했어요", "다시 시도하면 가족 목록을 다시 불러와요.", true)
        val changed = Expected("분석 대상이 바뀌었어요", "새 대상 기준으로 다시 질문해 주세요.", false)
        val family = Expected("선택한 가족을 찾을 수 없어요", "분석 대상을 다시 선택해 주세요.", false)
        val login = Expected("다시 로그인해 주세요", "로그인 정보가 만료됐어요.", false)
        val cases = mapOf(
            "NETWORK_UNAVAILABLE" to network,
            "CHAT_REQUEST_FAILED" to server, "INTERNAL_SERVER_ERROR" to server, null to server,
            "ANALYSIS_SELECTION_UNAVAILABLE" to unavailable,
            "ANALYSIS_SELECTION_CHANGED" to changed,
            "FAMILY_NOT_FOUND" to family, "CHAT_ANALYSIS_SELECTION_UNSUPPORTED" to family, "ANALYSIS_SELECTION_INVALID" to family,
            "AUTH_TOKEN_EXPIRED" to login, "AUTH_TOKEN_INVALID" to login, "AUTHENTICATION_REQUIRED" to login,
            "REFRESH_TOKEN_INVALID" to login, "REFRESH_TOKEN_REUSED" to login,
        )
        cases.forEach { (code, expected) ->
            val error = chatErrorFor(code)
            assertEquals(code, expected, Expected(error.title, error.description, error.retryable))
            assertFalse(code, error.isRetrying)
        }
    }
}
