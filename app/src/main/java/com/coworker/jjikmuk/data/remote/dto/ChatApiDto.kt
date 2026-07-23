package com.coworker.jjikmuk.data.remote.dto

data class ChatApiRequest(
    val message: String,
    val profiles: List<ChatApiProfile> = emptyList(),
    val barcode: String? = null,
    val product: ChatApiProduct? = null,
    val chat_history: List<ChatApiHistoryMessage> = emptyList(),
)

data class ChatApiProfile(
    val id: Long?,
    val nickname: String,
    val allergies: String?,
    val diseases: String? = null,
    val specialDiet: String? = null,
    val dislikedIngredients: String? = null,
)

data class ChatApiHistoryMessage(
    val role: String,
    val content: String,
)

data class ChatApiProduct(
    val productName: String,
    val barcode: String,
    val allergy: List<String>,
    val rawMaterials: String,
)

data class ChatApiResponse(
    val task_type: String? = null,
    val answer_source: String? = null,
    val pipeline_stage: String? = null,
    val intent: String? = null,
    val answer: String,
    val risk_level: String? = null,
    val reasons: List<String> = emptyList(),
    val profile_name: String? = null,
    val active_profile_names: List<String> = emptyList(),
    val recommended_questions: List<String> = emptyList(),
    val recommended_products: List<ChatApiRecommendedProduct> = emptyList(),
)

data class ChatApiRecommendedProduct(
    val product_name: String? = null,
    val food_code: String? = null,
    val prdlst_dcnm: String? = null,
    val barcode: String? = null,
    val rawmtrl_nm: String? = null,
    val allergy: List<String> = emptyList(),
    val allergy_info: List<String> = emptyList(),
    val match_confidence: Double? = null,
    val product_found: Boolean? = null,
    val reason: String? = null,
    val reasons: List<String> = emptyList(),
    val selection_type: String? = null,
)
