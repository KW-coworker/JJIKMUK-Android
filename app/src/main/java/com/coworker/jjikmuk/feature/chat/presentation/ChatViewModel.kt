package com.coworker.jjikmuk.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.repository.ChatHistoryRepository
import com.coworker.jjikmuk.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var initialMessage: String? = null
    private var conversationId: String? = null
    private var pendingConversationTitle: String? = null
    private var pendingProductQuestion: String? = null
    private var nextMessageId = 1L
    private var observeMessagesJob: Job? = null

    fun start(
        conversationId: String?,
        initialMessage: String,
    ) {
        if (
            this.conversationId == conversationId &&
            this.initialMessage == initialMessage &&
            (conversationId != null || initialMessage.isNotBlank())
        ) {
            return
        }

        this.conversationId = conversationId
        this.initialMessage = initialMessage
        pendingConversationTitle = null
        pendingProductQuestion = null
        _uiState.value = ChatUiState()
        nextMessageId = 1L

        observeMessagesJob?.cancel()
        if (conversationId != null) {
            observeMessages(conversationId)
            return
        }

        if (initialMessage.isBlank()) return

        sendMessage(initialMessage)
    }

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return

        sendMessageInternal(
            displayMessage = trimmedMessage,
            requestMessage = trimmedMessage,
            selectedProduct = null,
        )
    }

    fun selectProductCandidate(product: ChatProductCandidate) {
        sendMessageInternal(
            displayMessage = product.productName,
            requestMessage = pendingProductQuestion ?: product.productName,
            selectedProduct = product,
        )
    }

    private fun sendMessageInternal(
        displayMessage: String,
        requestMessage: String,
        selectedProduct: ChatProductCandidate?,
    ) {
        val currentConversationId = ensureConversation(displayMessage)

        _uiState.update { state ->
            state.copy(productCandidates = emptyList())
        }

        appendMessage(
            text = displayMessage,
            isMine = true,
        )

        viewModelScope.launch {
            pendingConversationTitle?.let { title ->
                chatHistoryRepository.createConversation(
                    conversationId = currentConversationId,
                    title = title,
                    preview = displayMessage,
                )
                pendingConversationTitle = null
            }

            chatHistoryRepository.addMessage(
                conversationId = currentConversationId,
                text = displayMessage,
                isMine = true,
            )

            _uiState.update { state ->
                state.copy(isLoading = true)
            }

            val response = chatRepository.sendMessage(
                message = requestMessage,
                selectedProduct = selectedProduct,
            )

            appendMessage(
                text = response.answer,
                isMine = false,
            )

            chatHistoryRepository.addMessage(
                conversationId = currentConversationId,
                text = response.answer,
                isMine = false,
            )

            pendingProductQuestion = if (response.productCandidates.isNotEmpty()) {
                requestMessage
            } else {
                null
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    productCandidates = response.productCandidates,
                )
            }
        }
    }

    private fun ensureConversation(firstMessage: String): String {
        conversationId?.let { id -> return id }

        val newConversationId = UUID.randomUUID().toString()
        conversationId = newConversationId
        pendingConversationTitle = createChatTitle(firstMessage)

        return newConversationId
    }

    private fun observeMessages(conversationId: String) {
        observeMessagesJob = viewModelScope.launch {
            chatHistoryRepository.observeMessages(conversationId)
                .collect { messages ->
                    nextMessageId = (messages.maxOfOrNull { message -> message.id } ?: 0L) + 1L
                    _uiState.update { state ->
                        state.copy(
                            messages = messages.map { message ->
                                ChatMessageUiModel(
                                    id = message.id,
                                    text = message.text,
                                    isMine = message.isMine,
                                )
                            },
                            productCandidates = emptyList(),
                        )
                    }
                }
        }
    }

    private fun appendMessage(
        text: String,
        isMine: Boolean,
    ) {
        val message = ChatMessageUiModel(
            id = nextMessageId++,
            text = text,
            isMine = isMine,
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + message,
            )
        }
    }
}

fun createChatTitle(message: String): String {
    val trimmedMessage = message.trim()
    if (trimmedMessage.isEmpty()) return "새 대화"

    return if (trimmedMessage.length <= CHAT_TITLE_MAX_LENGTH) {
        trimmedMessage
    } else {
        trimmedMessage.take(CHAT_TITLE_MAX_LENGTH) + ".."
    }
}

private const val CHAT_TITLE_MAX_LENGTH = 12
