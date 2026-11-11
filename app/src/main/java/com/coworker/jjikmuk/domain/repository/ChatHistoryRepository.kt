package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ChatConversation
import com.coworker.jjikmuk.domain.model.ChatSavedMessage
import kotlinx.coroutines.flow.Flow

data class ChatHistoryState(val account: FamilyAccount? = null, val loading: Boolean = false,
    val message: String? = null, val hasNext: Boolean = false)

interface ChatHistoryRepository {
    val state: kotlinx.coroutines.flow.StateFlow<ChatHistoryState> get() = kotlinx.coroutines.flow.MutableStateFlow(ChatHistoryState())
    suspend fun refresh() {}
    suspend fun loadMore() {}

    fun observeConversations(): Flow<List<ChatConversation>>

    fun observeConversations(query: String): Flow<List<ChatConversation>>

    fun observeTitle(conversationId: String, serverTitle: String): Flow<String> = kotlinx.coroutines.flow.flowOf(serverTitle)

    fun observeMessages(conversationId: String): Flow<List<ChatSavedMessage>>

    suspend fun createConversation(
        conversationId: String,
        title: String,
        preview: String,
    )

    suspend fun addMessage(
        conversationId: String,
        text: String,
        isMine: Boolean,
    ): Long

    suspend fun updateTitle(
        conversationId: String,
        title: String,
    )

    suspend fun updatePinned(
        conversationId: String,
        isPinned: Boolean,
    )

    suspend fun deleteConversation(conversationId: String)

    suspend fun deleteConversations(conversationIds: List<String>)
}
