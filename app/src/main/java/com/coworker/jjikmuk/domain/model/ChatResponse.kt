package com.coworker.jjikmuk.domain.model

data class ChatResponse(
    val answer: String,
    val productCandidates: List<ChatProductCandidate> = emptyList(),
    val errorCode: String? = null,
    /** 서버 recommended_questions를 정리한 후속 질문(최대 3개). */
    val recommendedQuestions: List<String> = emptyList(),
)

data class ChatProductCandidate(
    val productName: String,
    val brandName: String,
    val barcode: String?,
    val rawMaterials: String?,
    val allergyLabels: List<String>,
    val reason: String?,
)
