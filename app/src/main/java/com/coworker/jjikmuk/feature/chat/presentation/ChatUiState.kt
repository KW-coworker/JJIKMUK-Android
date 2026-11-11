package com.coworker.jjikmuk.feature.chat.presentation

import com.coworker.jjikmuk.domain.model.ChatProductCandidate

data class ChatUiState(
    val messages: List<ChatMessageUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val productCandidates: List<ChatProductCandidate> = emptyList(),
    val error: ChatErrorUiModel? = null,
    val title: String? = null,
    val notice: String? = null,
    val canRetry: Boolean = false,
    /** 가장 최근 봇 답변 아래에 보여 줄 후속 질문. 방금 받은 답변에만 채우고 대화 기록에서는 복원하지 않는다. */
    val suggestedQuestions: List<String> = emptyList(),
)
