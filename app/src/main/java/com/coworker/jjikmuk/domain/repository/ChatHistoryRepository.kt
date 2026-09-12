package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ChatConversation
import com.coworker.jjikmuk.domain.model.ChatSavedMessage
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeConversations(): Flow<List<ChatConversation>>

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
