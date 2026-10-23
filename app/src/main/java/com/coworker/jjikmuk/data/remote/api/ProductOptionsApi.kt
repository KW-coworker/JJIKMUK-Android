package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.*
import retrofit2.http.*

interface ProductOptionsApi {
    @GET("api/products/{barcode}/variants")
    suspend fun variants(@Path("barcode") barcode: String): ProductVariantsResponse
    @GET("api/recommendations/products/{barcode}")
    suspend fun alternatives(@Path("barcode") barcode: String, @Query("limit") limit: Int = 20): ProductAlternativesResponse
}
data class ProductVariantsResponse(val data: ProductVariantsDto? = null)
data class ProductVariantsDto(val selectedOptionKey: String = "", val options: List<ProductCapacityDto> = emptyList())
data class ProductCapacityDto(
    val key: String = "", val label: String = "", val amount: Double? = null,
    val unit: String? = null, val barcode: String = "", val data: ProductDetailDataDto? = null,
)
data class ProductAlternativesResponse(val data: ProductAlternativesDto? = null)
data class ProductAlternativesDto(
    val items: List<AlternativeItemDto> = emptyList(), val fallbackItems: List<AlternativeItemDto> = emptyList(),
    val resultMode: String = "ACTION_REQUIRED", val notice: String? = null,
)
data class AlternativeItemDto(
    val product: ProductDto? = null, val safety: AlternativeSafetyDto? = null,
    val verificationRequired: Boolean = false,
)
data class AlternativeSafetyDto(val status: String? = null)
