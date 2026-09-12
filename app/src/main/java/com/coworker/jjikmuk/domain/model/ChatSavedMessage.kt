package com.coworker.jjikmuk.domain.model

data class ChatSavedMessage(
    val id: Long,
    val text: String,
    val isMine: Boolean,
)
