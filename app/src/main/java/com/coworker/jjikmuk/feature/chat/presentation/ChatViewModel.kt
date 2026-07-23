package com.coworker.jjikmuk.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var initialMessage: String? = null
    private var pendingProductQuestion: String? = null
    private var nextMessageId = 1L

    fun start(initialMessage: String) {
        if (this.initialMessage == initialMessage) return

        this.initialMessage = initialMessage
        pendingProductQuestion = null
        _uiState.value = ChatUiState()
        nextMessageId = 1L
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
        _uiState.update { state ->
            state.copy(productCandidates = emptyList())
        }

        appendMessage(
            text = displayMessage,
            isMine = true,
        )

        viewModelScope.launch {
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
