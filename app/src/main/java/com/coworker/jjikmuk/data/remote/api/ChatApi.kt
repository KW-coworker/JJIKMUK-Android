package com.coworker.jjikmuk.data.remote.api

import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.data.remote.dto.ChatApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

interface ChatApi {
    @POST("api/chat")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: ChatApiRequest,
    ): ChatApiResponse
}
