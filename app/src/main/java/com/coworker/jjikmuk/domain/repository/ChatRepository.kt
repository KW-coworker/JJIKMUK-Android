package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.model.ChatResponse

interface ChatRepository {
    suspend fun sendMessage(
        message: String,
        selectedProduct: ChatProductCandidate? = null,
    ): ChatResponse
}
