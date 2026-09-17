package com.coworker.jjikmuk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val preview: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
