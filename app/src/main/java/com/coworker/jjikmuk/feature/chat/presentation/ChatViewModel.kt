package com.coworker.jjikmuk.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class ChatViewModel @Inject constructor(private val chatRepository: ChatRepository,
    private val chatHistoryRepository: ChatHistoryRepository, private val targets: AnalysisTargetRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    val targetState = targets.state
    private var initialMessage: String? = null
    private var conversationId: String? = null
    private var pendingConversationTitle: String? = null
    private var pendingProductQuestion: String? = null
    private var nextMessageId = 1L
    private var observeMessagesJob: Job? = null
    private var requestJob: Job? = null
    private var awaitingTargets = false
    private var revision = 0L
    private var contextSelection: AnalysisTargetSelection? = null

    init {
        viewModelScope.launch {
            targetState.collect { state ->
                val previous = contextSelection
                if (previous != null && (state.account != previous.account ||
                    (!awaitingTargets && state.selection != null && state.selection != previous))) {
                    reset()
                    if (state.account != null) appendMessage("분석 대상을 변경했어요. 새 질문부터 선택한 대상의 조건을 적용해요.", false)
                }
                if (!awaitingTargets && state.selection != null) contextSelection = state.selection
            }
        }
    }
    fun selectTarget(id: String, checked: Boolean) {
        val account = targets.captureAccount()
        viewModelScope.launch { targets.setSelected(id, checked, account) }
    }
    private fun reset() {
        revision++; requestJob?.cancel(); observeMessagesJob?.cancel()
        requestJob = null; observeMessagesJob = null; conversationId = null
        pendingConversationTitle = null; pendingProductQuestion = null; initialMessage = null
        awaitingTargets = false
        contextSelection = null; nextMessageId = 1; _uiState.value = ChatUiState()
    }
    fun start(conversationId: String?, initialMessage: String) {
        if (this.conversationId == conversationId && this.initialMessage == initialMessage &&
            (conversationId != null || initialMessage.isNotBlank())) return
        reset()
        this.conversationId = conversationId; this.initialMessage = initialMessage
        contextSelection = targets.state.value.selection
        if (conversationId != null) observeMessages(conversationId)
        else if (initialMessage.isNotBlank()) sendMessage(initialMessage)
    }
    fun sendMessage(message: String) {
        val text = message.trim()
        if (text.isNotEmpty()) sendMessageInternal(text, text, null)
    }
    fun dismissProductCandidates() { _uiState.update { it.copy(productCandidates = emptyList()) } }
    fun selectProductCandidate(product: ChatProductCandidate) {
        if (product !in _uiState.value.productCandidates) return
        sendMessageInternal(product.productName, pendingProductQuestion ?: product.productName, product)
    }
    private fun sendMessageInternal(display: String, message: String, product: ChatProductCandidate?) {
        if (_uiState.value.isLoading) return
        val account = targets.captureAccount()
        if (account == null) { appendMessage("로그인 상태를 확인해 주세요.", false); return }
        val ticket = revision
        appendMessage(display, true)
        _uiState.update { it.copy(isLoading = true, productCandidates = emptyList()) }
        awaitingTargets = true
        requestJob = viewModelScope.launch {
            try {
                val selection = targets.awaitSelection(account)
                if (ticket != revision) return@launch
                if (selection == null) {
                    appendMessage("분석 대상을 불러오지 못했어요. 가족 목록과 로그인 상태를 확인해 주세요.", false)
                    return@launch
                }
                awaitingTargets = false
                if (contextSelection != null && contextSelection != selection) {
                    if (product != null) {
                        reset()
                        appendMessage("분석 대상이 바뀌었어요. 상품을 다시 선택하거나 질문해 주세요.", false)
                        return@launch
                    }
                    observeMessagesJob?.cancel(); observeMessagesJob = null
                    conversationId = null; pendingConversationTitle = null; pendingProductQuestion = null
                    nextMessageId = 1; _uiState.value = ChatUiState(isLoading = true)
                    appendMessage(display, true)
                }
                contextSelection = selection
                fun current() = ticket == revision && targets.isCurrent(selection)
                if (!current()) return@launch
                val id = ensureConversation(display)
                pendingConversationTitle?.let { title ->
                    chatHistoryRepository.createConversation(id, title, display)
                    if (!current()) return@launch
                    pendingConversationTitle = null
                }
                chatHistoryRepository.addMessage(id, display, true)
                if (!current()) return@launch
                val response = chatRepository.sendMessage(message, product, selection)
                if (!current()) return@launch
                appendMessage(response.answer, false)
                chatHistoryRepository.addMessage(id, response.answer, false)
                if (!current()) return@launch
                pendingProductQuestion = message.takeIf { response.productCandidates.isNotEmpty() }
                _uiState.update { it.copy(productCandidates = response.productCandidates) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (ticket == revision) appendMessage("요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.", false)
            } finally {
                if (ticket == revision) {
                    awaitingTargets = false
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    private fun ensureConversation(message: String): String {
        conversationId?.let { return it }
        return UUID.randomUUID().toString().also { conversationId = it; pendingConversationTitle = createChatTitle(message) }
    }
    private fun observeMessages(id: String) {
        val ticket = revision
        observeMessagesJob = viewModelScope.launch {
            chatHistoryRepository.observeMessages(id).collect { messages ->
                if (ticket != revision) return@collect
                nextMessageId = (messages.maxOfOrNull { it.id } ?: 0) + 1
                _uiState.update { it.copy(messages = messages.map { ChatMessageUiModel(it.id, it.text, it.isMine) }) }
            }
        }
    }
    private fun appendMessage(text: String, isMine: Boolean) {
        val message = ChatMessageUiModel(nextMessageId++, text, isMine)
        _uiState.update { it.copy(messages = it.messages + message) }
    }
}

fun createChatTitle(message: String): String {
    val trimmed = message.trim()
    if (trimmed.isEmpty()) return "새 대화"
    return if (trimmed.length <= 12) trimmed else trimmed.take(12) + ".."
}
