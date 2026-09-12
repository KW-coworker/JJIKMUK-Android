package com.coworker.jjikmuk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coworker.jjikmuk.data.local.entity.ChatConversationEntity
import com.coworker.jjikmuk.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatHistoryDao {

    @Query(
        """
        SELECT *
        FROM chat_conversations
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeConversations(): Flow<List<ChatConversationEntity>>

    @Query(
        """
        SELECT
            conversation.id,
            conversation.title,
            CASE
                WHEN conversation.preview LIKE '%' || :query || '%' THEN conversation.preview
                WHEN conversation.title LIKE '%' || :query || '%' THEN conversation.preview
                ELSE (
                    SELECT matchedMessage.text
                    FROM chat_messages AS matchedMessage
                    WHERE matchedMessage.conversationId = conversation.id
                        AND matchedMessage.text LIKE '%' || :query || '%'
                    ORDER BY matchedMessage.createdAt DESC, matchedMessage.id DESC
                    LIMIT 1
                )
            END AS preview,
            conversation.isPinned,
            conversation.createdAt,
            conversation.updatedAt
        FROM chat_conversations AS conversation
        LEFT JOIN chat_messages AS message
            ON conversation.id = message.conversationId
        WHERE conversation.title LIKE '%' || :query || '%'
            OR conversation.preview LIKE '%' || :query || '%'
            OR message.text LIKE '%' || :query || '%'
        ORDER BY conversation.isPinned DESC, conversation.updatedAt DESC
        """,
    )
    fun observeConversations(query: String): Flow<List<ChatConversationEntity>>

    @Query(
        """
        SELECT *
        FROM chat_messages
        WHERE conversationId = :conversationId
        ORDER BY createdAt ASC, id ASC
        """,
    )
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ChatConversationEntity)

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query(
        """
        UPDATE chat_conversations
        SET preview = :preview, updatedAt = :updatedAt
        WHERE id = :conversationId
        """,
    )
    suspend fun updateConversationPreview(
        conversationId: String,
        preview: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE chat_conversations
        SET title = :title, updatedAt = :updatedAt
        WHERE id = :conversationId
        """,
    )
    suspend fun updateConversationTitle(
        conversationId: String,
        title: String,
        updatedAt: Long,
    )

    @Query("UPDATE chat_conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updatePinned(
        conversationId: String,
        isPinned: Boolean,
    )

    @Query("DELETE FROM chat_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM chat_conversations WHERE id IN (:conversationIds)")
    suspend fun deleteConversations(conversationIds: List<String>)
}
