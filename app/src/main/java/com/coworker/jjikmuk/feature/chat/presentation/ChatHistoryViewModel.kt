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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val histories: StateFlow<List<ChatHistoryUiModel>> =
        searchQuery
            .flatMapLatest { query ->
                val trimmedQuery = query.trim()
                if (trimmedQuery.isEmpty()) {
                    chatHistoryRepository.observeConversations()
                } else {
                    chatHistoryRepository.observeConversations(trimmedQuery)
                }
            }
            .map { conversations ->
                conversations.map { conversation -> conversation.toUiModel() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

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
