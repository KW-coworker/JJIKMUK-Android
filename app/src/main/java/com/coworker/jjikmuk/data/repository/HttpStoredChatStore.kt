package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.dto.ChatApiResponse
import com.coworker.jjikmuk.di.BackendRetrofit
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*

internal data class ChatEnvelope<T>(val data: T?)
internal data class CreateRoomBody(val clientConversationId: String, val title: String)
internal data class RoomBody(val id: Long?, val clientConversationId: String?, val title: String?, val updatedAt: String?)
internal data class RoomsBody(val items: List<RoomBody>?, val page: Int?, val hasNext: Boolean?)
internal data class TurnBody(val id: Long?, val clientMessageId: String?, val message: String?, val barcode: String?,
    val status: String?, val response: ChatApiResponse?, val errorCode: String?)
internal data class TurnsBody(val items: List<TurnBody>?, val nextAfterId: Long?, val hasNext: Boolean?)
internal interface StoredChatApi {
    @POST("api/chat/conversations") suspend fun create(@Header("Authorization") auth: String, @Body body: CreateRoomBody): Response<ChatEnvelope<RoomBody>>
    @GET("api/chat/conversations") suspend fun rooms(@Header("Authorization") auth: String, @Query("page") page: Int,
        @Query("size") size: Int = 20): Response<ChatEnvelope<RoomsBody>>
    @GET("api/chat/conversations/{id}") suspend fun room(@Header("Authorization") auth: String, @Path("id") id: Long): Response<ChatEnvelope<RoomBody>>
    @GET("api/chat/conversations/{id}/messages") suspend fun turns(@Header("Authorization") auth: String, @Path("id") id: Long,
        @Query("afterId") after: Long, @Query("limit") limit: Int = 20): Response<ChatEnvelope<TurnsBody>>
    @GET("api/chat/conversations/{id}/messages/by-client-id/{clientId}") suspend fun turn(@Header("Authorization") auth: String,
        @Path("id") id: Long, @Path("clientId") clientId: String): Response<ChatEnvelope<TurnBody>>
    @GET("api/chat/conversations/{id}/messages/{turnId}") suspend fun turnById(@Header("Authorization") auth: String,
        @Path("id") id: Long, @Path("turnId") turnId: Long): Response<ChatEnvelope<TurnBody>>
    @POST("api/chat") suspend fun send(@Header("Authorization") auth: String, @Body question: StoredChatQuestion): Response<ChatApiResponse>
    @DELETE("api/chat/conversations/{id}") suspend fun delete(@Header("Authorization") auth: String, @Path("id") id: Long): Response<Unit>
}

class HttpStoredChatStore internal constructor(private val api: StoredChatApi,
    private val sessions: AuthSessionManager, private val requests: AuthenticatedRequests) : StoredChatStore {
    @Inject constructor(@BackendRetrofit retrofit: Retrofit, sessions: AuthSessionManager, requests: AuthenticatedRequests) :
        this(retrofit.create(StoredChatApi::class.java), sessions, requests)
    override fun captureAccount(): FamilyAccount? = sessions.state.value.let { s ->
        s.credentials?.takeIf { s.canEnterHome }?.let { FamilyAccount(it.userId, s.generation) } }
    override suspend fun create(account: FamilyAccount, clientId: String, title: String) = call(account, false, 201,
        { api.create(it, CreateRoomBody(clientId, title)) }) { requireNotNull(it.body()?.data).domain() }
    override suspend fun rooms(account: FamilyAccount, page: Int) = call(account, true, 200, { api.rooms(it, page) }) {
        val body = requireNotNull(it.body()?.data)
        require(body.page == page && body.hasNext != null)
        val items = requireNotNull(body.items).map { room -> room.domain() }
        require(!body.hasNext || items.isNotEmpty())
        StoredChatRooms(items, page, body.hasNext)
    }
    override suspend fun room(account: FamilyAccount, id: Long) = call(account, true, 200, { api.room(it, id) }) {
        requireNotNull(it.body()?.data).domain().also { value -> require(value.id == id.toString()) }
    }
    override suspend fun turns(account: FamilyAccount, id: Long, after: Long) = call(account, true, 200, { api.turns(it, id, after) }) {
        val body = requireNotNull(it.body()?.data)
        val items = requireNotNull(body.items).map { value -> value.domain() }
        val next = requireNotNull(body.nextAfterId)
        val hasNext = requireNotNull(body.hasNext)
        require(items.all { value -> value.id > after } && next >= after)
        require(!hasNext || (items.isNotEmpty() && next > after))
        StoredChatTurns(items, next, hasNext)
    }
    override suspend fun turn(account: FamilyAccount, id: Long, clientId: String) = call(account, true, 200,
        { api.turn(it, id, clientId) }) { requireNotNull(it.body()?.data).domain().also { value -> require(value.clientMessageId == clientId) } }
    override suspend fun turnById(account: FamilyAccount, id: Long, turnId: Long, clientId: String) = call(account, true, 200,
        { api.turnById(it, id, turnId) }) { requireNotNull(it.body()?.data).domain().also { value ->
            require(value.id == turnId && value.clientMessageId == clientId)
        } }
    override suspend fun send(account: FamilyAccount, question: StoredChatQuestion) = call(account, false, 200, { api.send(it, question) }) {
        require(it.headers()["X-Jjikmuk-Conversation-Id"]?.toLongOrNull() == question.conversationId)
        val turnId = requireNotNull(it.headers()["X-Jjikmuk-Chat-Turn-Id"]?.toLongOrNull()).also { value -> require(value > 0) }
        val replayed = requireNotNull(it.headers()["X-Jjikmuk-Chat-Replayed"]?.toBooleanStrictOrNull())
        val response = requireNotNull(it.body())
        StoredChatTurn(turnId, question.clientMessageId, question.message, question.barcode, "SUCCEEDED", response.domain(), replayed = replayed)
    }
    override suspend fun delete(account: FamilyAccount, id: Long) = call(account, false, 204, { api.delete(it, id) }) { Unit }

    private suspend fun <T, R> call(account: FamilyAccount, read: Boolean, status: Int,
        action: suspend (String) -> Response<T>, decode: (Response<T>) -> R): AuthResult<R> = requests.execute(retrySafeRead = read) { token ->
        if (captureAccount() != account) AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
        else try {
            val response = action("Bearer $token")
            if (response.code() != status) AuthResult.Failure(AuthErrorMapper.decode(response.code(),
                response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], System.currentTimeMillis()))
            else if (captureAccount() != account) AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
            else AuthResult.Success(decode(response))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
        catch (_: Exception) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
    }
}
private fun RoomBody.domain() = ChatConversation(requireNotNull(id).also { require(it > 0) }.toString(),
    requireNotNull(title), "", false, LocalDateTime.parse(requireNotNull(updatedAt)).toInstant(ZoneOffset.UTC).toEpochMilli())
private fun ChatApiResponse.domain() = ChatResponse(answer, toProductCandidates())
private fun TurnBody.domain(): StoredChatTurn {
    require(status in setOf("PENDING", "SUCCEEDED", "FAILED"))
    require(status != "SUCCEEDED" || response != null)
    return StoredChatTurn(requireNotNull(id).also { require(it > 0) }, requireNotNull(clientMessageId),
        requireNotNull(message), barcode, requireNotNull(status), response?.domain(), errorCode)
}
