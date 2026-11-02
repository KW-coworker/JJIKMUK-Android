package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.ProductSearchApiResponse
import com.coworker.jjikmuk.data.remote.dto.ProductDetailApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("api/products/id/{productId}")
    suspend fun getProductDetailById(@Path("productId") productId: Long, @Query("analysisMode") analysisMode: String? = null, @Query("includeSelf") includeSelf: Boolean? = null, @Query("familyProfileIds") familyProfileIds: List<Long>? = null): ProductDetailApiResponse


    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("keyword") keyword: String,
        @Query("userId") userId: Long? = null,
        @Query("allergies") allergies: List<String> = emptyList(),
        @Query("analysisMode") analysisMode: String? = null, @Query("includeSelf") includeSelf: Boolean? = null, @Query("familyProfileIds") familyProfileIds: List<Long>? = null,
    ): ProductSearchApiResponse

    @GET("api/products/{barcode}")
    suspend fun getProductDetail(
        @Path("barcode") barcode: String,
        @Query("userId") userId: Long? = null,
        @Query("allergies") allergies: List<String> = emptyList(),
        @Query("analysisMode") analysisMode: String? = null, @Query("includeSelf") includeSelf: Boolean? = null, @Query("familyProfileIds") familyProfileIds: List<Long>? = null,
    ): ProductDetailApiResponse
}
