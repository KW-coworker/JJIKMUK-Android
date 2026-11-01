package com.coworker.jjikmuk.data.remote.dto

data class MyProfileResponse(val data: MyProfileDto? = null)
data class MyProfileDto(
    val id: Long = 0,
    val nickname: String = "",
    val allergies: String? = null,
    val diseases: String? = null,
    val specialDiet: String? = null,
    val dislikedIngredients: String? = null,
)
data class SafeRecommendationResponse(val data: SafeRecommendationData? = null)
data class SafeRecommendationData(
    val userId: Long = 0,
    val returnedBarcodes: List<String> = emptyList(),
    val categories: List<SafeRecommendationCategory> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val recommendationMode: String = "EMPTY",
    val notice: String = "",
)
data class SafeRecommendationCategory(val items: List<SafeRecommendationItem> = emptyList())
data class SafeRecommendationItem(
    val product: ProductDto? = null,
    val preferenceRelaxed: Boolean = false,
)
