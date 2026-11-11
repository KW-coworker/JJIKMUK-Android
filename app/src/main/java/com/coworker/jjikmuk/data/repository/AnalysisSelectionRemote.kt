package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.di.BackendRetrofit
import com.coworker.jjikmuk.domain.repository.FamilyAccount
import com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.IOException

data class SavedAnalysisSelection(val includeSelf: Boolean, val familyProfileIds: List<Long>,
    val version: Long, val configured: Boolean, val mode: String)
internal data class SelectionBody(val includeSelf: Boolean?, val familyProfileIds: List<Long>?,
    val version: Long?, val configured: Boolean?, val mode: String?)
internal data class SelectionEnvelope(val data: SelectionBody?)
internal data class SelectionWrite(val version: Long, val includeSelf: Boolean, val familyProfileIds: List<Long>)
internal interface SelectionApi {
    @GET("api/users/me/analysis-selection") suspend fun get(@Header("Authorization") auth: String): Response<SelectionEnvelope>
    @PUT("api/users/me/analysis-selection") suspend fun put(@Header("Authorization") auth: String,
        @Body body: SelectionWrite): Response<SelectionEnvelope>
}
internal interface AnalysisSelectionRemote {
    suspend fun get(account: FamilyAccount): AuthResult<SavedAnalysisSelection>
    suspend fun put(account: FamilyAccount, version: Long, includeSelf: Boolean, ids: List<Long>): AuthResult<SavedAnalysisSelection>
}
class HttpAnalysisSelectionRemote internal constructor(private val api: SelectionApi,
    private val sessions: AuthSessionManager, private val requests: AuthenticatedRequests) : AnalysisSelectionRemote {
    @Inject constructor(@BackendRetrofit retrofit: Retrofit, sessions: AuthSessionManager, requests: AuthenticatedRequests) :
        this(retrofit.create(SelectionApi::class.java), sessions, requests)
    private fun current(a: FamilyAccount) = sessions.state.value.let {
        it.canEnterHome && it.credentials?.userId == a.userId && it.generation == a.generation }
    override suspend fun get(account: FamilyAccount) = execute(account, true) { api.get(it) }
    override suspend fun put(account: FamilyAccount, version: Long, includeSelf: Boolean, ids: List<Long>) =
        execute(account, false) { token ->
            require(version >= 0 && ids.size <= 20 && ids.all { it > 0 } && ids.distinct().size == ids.size)
            api.put(token, SelectionWrite(version, includeSelf, ids))
        }
    private suspend fun execute(account: FamilyAccount, read: Boolean,
        call: suspend (String) -> Response<SelectionEnvelope>): AuthResult<SavedAnalysisSelection> =
        requests.execute(retrySafeRead = read) { token ->
            if (!current(account)) AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
            else try {
                val response = call("Bearer $token")
                if (!response.isSuccessful) AuthResult.Failure(AuthErrorMapper.decode(response.code(),
                    response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], System.currentTimeMillis()))
                else {
                    val body = requireNotNull(response.body()?.data)
                    val self = requireNotNull(body.includeSelf)
                    val ids = requireNotNull(body.familyProfileIds)
                    val version = requireNotNull(body.version)
                    val configured = requireNotNull(body.configured)
                    val mode = if (ids.isNotEmpty()) "selected" else if (self) "self" else "none"
                    require(version >= 0 && ids.size <= 20 && ids.all { it > 0 } && ids.distinct().size == ids.size)
                    require(body.mode == mode)
                    require(configured || (self && ids.isEmpty() && version == 0L))
                    AuthResult.Success(SavedAnalysisSelection(self, ids.sorted(), version, configured, mode))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
            catch (_: Exception) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
        }
}
