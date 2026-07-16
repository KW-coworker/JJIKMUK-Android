package com.coworker.jjikmuk.feature.chat.presentation

data class ChatMessageUiModel(
    val id: Long,
    val text: String,
    val isMine: Boolean,
)
