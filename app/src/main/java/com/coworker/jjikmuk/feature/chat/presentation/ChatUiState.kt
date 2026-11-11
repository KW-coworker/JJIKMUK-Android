package com.coworker.jjikmuk.feature.chat.presentation

import com.coworker.jjikmuk.domain.model.ChatProductCandidate

data class ChatUiState(
    val messages: List<ChatMessageUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val productCandidates: List<ChatProductCandidate> = emptyList(),
    /** 마지막 요청의 오류. 항상 하나만 있고 메시지 목록 맨 아래에 그린다. */
    val error: ChatErrorUiModel? = null,
)
