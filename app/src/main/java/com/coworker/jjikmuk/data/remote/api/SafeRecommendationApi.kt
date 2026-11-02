package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.*
import retrofit2.http.*

interface SafeRecommendationApi {
    @GET("api/users/me") suspend fun getMe(): MyProfileResponse
    @GET("api/products/safe-recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int = 6,
        @Query("cursor") cursor: String? = null,
        @Query("excludeBarcodes") excluded: List<String> = emptyList(),
        @Query("analysisMode") analysisMode: String? = null,
        @Query("includeSelf") includeSelf: Boolean? = null,
        @Query("familyProfileIds") familyProfileIds: List<Long>? = null,
    ): SafeRecommendationResponse
}
