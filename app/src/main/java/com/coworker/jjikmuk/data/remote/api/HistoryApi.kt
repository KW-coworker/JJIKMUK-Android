package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.ProductDto
import retrofit2.Response
import retrofit2.http.*

interface HistoryApi {
    @GET("api/histories/user/{userId}")
    suspend fun list(@Header("Authorization") authorization: String, @Path("userId") userId: Long): Response<HistoryEnvelope>

    @POST("api/histories/events")
    suspend fun record(@Header("Authorization") authorization: String, @Body event: HistoryEvent): Response<Unit>
}

data class HistoryEnvelope(val data: List<HistoryEntry?>? = null)
data class HistoryEntry(val id: Long = 0, val actionType: String? = null,
    val scannedAt: String? = null, val product: ProductDto? = null)
data class HistoryEvent(val barcode: String, val actionType: String = "DETAIL_VIEW")
