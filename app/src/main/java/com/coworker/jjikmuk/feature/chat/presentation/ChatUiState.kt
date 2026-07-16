package com.coworker.jjikmuk.feature.chat.presentation

import com.coworker.jjikmuk.domain.model.ChatProductCandidate

data class ChatUiState(
    val messages: List<ChatMessageUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val productCandidates: List<ChatProductCandidate> = emptyList(),
)
