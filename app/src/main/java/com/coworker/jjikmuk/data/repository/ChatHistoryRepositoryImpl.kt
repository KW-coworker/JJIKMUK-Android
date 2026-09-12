package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.local.dao.ChatHistoryDao
import com.coworker.jjikmuk.data.local.entity.ChatConversationEntity
import com.coworker.jjikmuk.data.local.entity.ChatMessageEntity
import com.coworker.jjikmuk.domain.model.ChatConversation
import com.coworker.jjikmuk.domain.model.ChatSavedMessage
import com.coworker.jjikmuk.domain.repository.ChatHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepositoryImpl @Inject constructor(
    private val chatHistoryDao: ChatHistoryDao,
) : ChatHistoryRepository {

    override fun observeConversations(): Flow<List<ChatConversation>> {
        return chatHistoryDao.observeConversations().map { conversations ->
            conversations.map { conversation -> conversation.toDomain() }
        }
    }

    override fun observeConversations(query: String): Flow<List<ChatConversation>> {
        return chatHistoryDao.observeConversations(query).map { conversations ->
            conversations.map { conversation -> conversation.toDomain() }
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatSavedMessage>> {
        return chatHistoryDao.observeMessages(conversationId).map { messages ->
            messages.map { message -> message.toDomain() }
        }
    }

    override suspend fun createConversation(
        conversationId: String,
        title: String,
        preview: String,
    ) {
        val now = System.currentTimeMillis()
        chatHistoryDao.upsertConversation(
            ChatConversationEntity(
                id = conversationId,
                title = title,
                preview = preview,
                isPinned = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun addMessage(
        conversationId: String,
        text: String,
        isMine: Boolean,
    ): Long {
        val now = System.currentTimeMillis()
        val messageId = chatHistoryDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                text = text,
                isMine = isMine,
                createdAt = now,
            ),
        )
        chatHistoryDao.updateConversationPreview(
            conversationId = conversationId,
            preview = text,
            updatedAt = now,
        )
        return messageId
    }

    override suspend fun updateTitle(
        conversationId: String,
        title: String,
    ) {
        chatHistoryDao.updateConversationTitle(
            conversationId = conversationId,
            title = title,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun updatePinned(
        conversationId: String,
        isPinned: Boolean,
    ) {
        chatHistoryDao.updatePinned(
            conversationId = conversationId,
            isPinned = isPinned,
        )
    }

    override suspend fun deleteConversation(conversationId: String) {
        chatHistoryDao.deleteConversation(conversationId)
    }

    override suspend fun deleteConversations(conversationIds: List<String>) {
        if (conversationIds.isNotEmpty()) {
            chatHistoryDao.deleteConversations(conversationIds)
        }
    }

    private fun ChatConversationEntity.toDomain(): ChatConversation {
        return ChatConversation(
            id = id,
            title = title,
            preview = preview,
            isPinned = isPinned,
            updatedAt = updatedAt,
        )
    }

    private fun ChatMessageEntity.toDomain(): ChatSavedMessage {
        return ChatSavedMessage(
            id = id,
            text = text,
            isMine = isMine,
        )
    }
}
