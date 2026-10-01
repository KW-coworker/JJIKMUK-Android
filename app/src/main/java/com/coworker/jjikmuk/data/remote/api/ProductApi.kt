package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.ProductSearchApiResponse
import com.coworker.jjikmuk.data.remote.dto.ProductDetailApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("keyword") keyword: String,
        @Query("userId") userId: Long? = null,
        @Query("allergies") allergies: List<String> = emptyList(),
    ): ProductSearchApiResponse

    @GET("api/products/safe-recommendations")
    suspend fun getSafeRecommendations(
        @Query("filters") filters: List<String> = emptyList(),
        @Query("allergies") allergies: List<String> = emptyList(),
        @Query("limit") limit: Int = 6,
        @Query("userId") userId: Long? = null,
    ): ProductSearchApiResponse

    @GET("api/products/{barcode}")
    suspend fun getProductDetail(
        @Path("barcode") barcode: String,
        @Query("userId") userId: Long? = null,
        @Query("allergies") allergies: List<String> = emptyList(),
    ): ProductDetailApiResponse
}
