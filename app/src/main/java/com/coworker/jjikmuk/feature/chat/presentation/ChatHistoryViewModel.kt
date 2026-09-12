package com.coworker.jjikmuk.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.ChatConversation
import com.coworker.jjikmuk.domain.repository.ChatHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
) : ViewModel() {

    val histories: StateFlow<List<ChatHistoryUiModel>> =
        chatHistoryRepository.observeConversations()
            .map { conversations ->
                conversations.map { conversation -> conversation.toUiModel() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun togglePinned(history: ChatHistoryUiModel) {
        viewModelScope.launch {
            chatHistoryRepository.updatePinned(
                conversationId = history.id,
                isPinned = !history.isPinned,
            )
        }
    }

    fun deleteChat(historyId: String) {
        viewModelScope.launch {
            chatHistoryRepository.deleteConversation(historyId)
        }
    }

    fun deleteChats(historyIds: List<String>) {
        viewModelScope.launch {
            chatHistoryRepository.deleteConversations(historyIds)
        }
    }

    private fun ChatConversation.toUiModel(): ChatHistoryUiModel {
        return ChatHistoryUiModel(
            id = id,
            title = title,
            preview = preview,
            time = chatHistoryTimeFormat.format(Date(updatedAt)).lowercase(Locale.US),
            isPinned = isPinned,
        )
    }

    private companion object {
        val chatHistoryTimeFormat = SimpleDateFormat("h:mm a", Locale.US)
    }
}
