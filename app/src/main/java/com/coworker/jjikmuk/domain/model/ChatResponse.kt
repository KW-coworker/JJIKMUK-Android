package com.coworker.jjikmuk.domain.model

data class ChatResponse(
    val answer: String,
    val productCandidates: List<ChatProductCandidate> = emptyList(),
)

data class ChatProductCandidate(
    val productName: String,
    val brandName: String,
    val barcode: String?,
    val rawMaterials: String?,
    val allergyLabels: List<String>,
    val reason: String?,
)
