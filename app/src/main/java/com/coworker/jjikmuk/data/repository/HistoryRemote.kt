package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.local.preference.ProductViewOwner
import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.di.BackendRetrofit
import com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.google.gson.JsonParseException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit

internal interface HistoryRemote {
    suspend fun list(owner: ProductViewOwner): AuthResult<List<HistoryEntry?>>
    suspend fun record(owner: ProductViewOwner, barcode: String): AuthResult<Unit>
}

class HttpHistoryRemote internal constructor(
    private val api: HistoryApi,
    private val sessions: AuthSessionManager,
    private val requests: AuthenticatedRequests,
) : HistoryRemote {
    @Inject constructor(@BackendRetrofit retrofit: Retrofit, sessions: AuthSessionManager, requests: AuthenticatedRequests) :
        this(retrofit.create(HistoryApi::class.java), sessions, requests)

    override suspend fun list(owner: ProductViewOwner) = requests.execute(retrySafeRead = true) { token ->
        if (!current(owner)) changed() else call({ api.list("Bearer $token", owner.userId) }) {
            requireNotNull(it.body()?.data)
        }
    }

    override suspend fun record(owner: ProductViewOwner, barcode: String) = requests.execute { token ->
        if (!current(owner)) changed() else call({ api.record("Bearer $token", HistoryEvent(barcode)) }) { Unit }
    }

    private fun current(owner: ProductViewOwner) = sessions.state.value.let {
        it.canEnterHome && it.credentials?.userId == owner.userId && it.generation == owner.generation
    }
    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))

    private suspend fun <T, R> call(request: suspend () -> Response<T>, decode: (Response<T>) -> R): AuthResult<R> = try {
        val response = request()
        if (!response.isSuccessful) AuthResult.Failure(AuthErrorMapper.decode(response.code(),
            response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], System.currentTimeMillis()))
        else AuthResult.Success(decode(response))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
    catch (_: JsonParseException) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
    catch (_: IllegalArgumentException) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
}
