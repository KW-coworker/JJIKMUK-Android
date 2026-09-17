package com.coworker.jjikmuk.domain.model

data class ChatConversation(
    val id: String,
    val title: String,
    val preview: String,
    val isPinned: Boolean,
    val updatedAt: Long,
)
