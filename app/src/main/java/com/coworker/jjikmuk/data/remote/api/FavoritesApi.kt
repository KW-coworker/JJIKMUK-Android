package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.ProductDto
import retrofit2.Response
import retrofit2.http.*

interface FavoritesApi {
    @GET("api/users/me/favorites")
    suspend fun list(@Header("Authorization") authorization: String, @Query("page") page: Int,
        @Query("size") size: Int = 50): Response<FavoriteEnvelope<FavoritePage>>
    @GET("api/users/me/favorites/{productId}")
    suspend fun state(@Header("Authorization") authorization: String, @Path("productId") productId: Long): Response<FavoriteEnvelope<FavoriteStatus>>
    @PUT("api/users/me/favorites/{productId}")
    suspend fun add(@Header("Authorization") authorization: String, @Path("productId") productId: Long): Response<FavoriteEnvelope<FavoriteStatus>>
    @DELETE("api/users/me/favorites/{productId}")
    suspend fun remove(@Header("Authorization") authorization: String, @Path("productId") productId: Long): Response<FavoriteEnvelope<FavoriteStatus>>
}

data class FavoriteEnvelope<T>(val data: T? = null)
data class FavoritePage(val items: List<FavoriteItem?>? = null, val page: Int? = null,
    val size: Int? = null, val hasNext: Boolean? = null)
data class FavoriteItem(val productId: Long? = null, val barcode: String? = null,
    val product: ProductDto? = null)
data class FavoriteStatus(val productId: Long? = null, val isFavorite: Boolean? = null)
