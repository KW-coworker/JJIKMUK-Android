package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.di.BackendRetrofit
import com.coworker.jjikmuk.domain.repository.FavoriteAccount
import com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.google.gson.JsonParseException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit

internal interface FavoritesRemote {
    suspend fun list(account: FavoriteAccount, page: Int): AuthResult<FavoritePage>
    suspend fun state(account: FavoriteAccount, id: Long): AuthResult<FavoriteStatus>
    suspend fun set(account: FavoriteAccount, id: Long, favorite: Boolean): AuthResult<FavoriteStatus>
}

class HttpFavoritesRemote internal constructor(private val api: FavoritesApi,
    private val sessions: AuthSessionManager, private val requests: AuthenticatedRequests) : FavoritesRemote {
    @Inject constructor(@BackendRetrofit retrofit: Retrofit, sessions: AuthSessionManager, requests: AuthenticatedRequests) :
        this(retrofit.create(FavoritesApi::class.java), sessions, requests)

    override suspend fun list(account: FavoriteAccount, page: Int) = requests.execute(retrySafeRead = true) { token ->
        guarded(account) { call { api.list("Bearer $token", page) } }
    }
    override suspend fun state(account: FavoriteAccount, id: Long) = requests.execute(retrySafeRead = true) { token ->
        guarded(account) { call { api.state("Bearer $token", id) } }
    }
    override suspend fun set(account: FavoriteAccount, id: Long, favorite: Boolean) = requests.execute { token ->
        guarded(account) { call { if (favorite) api.add("Bearer $token", id) else api.remove("Bearer $token", id) } }
    }
    private suspend fun <T> guarded(account: FavoriteAccount, block: suspend () -> AuthResult<T>): AuthResult<T> {
        val current = sessions.state.value
        return if (current.canEnterHome && current.credentials?.userId == account.userId && current.generation == account.generation)
            block() else AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
    }
    private suspend fun <T> call(block: suspend () -> Response<FavoriteEnvelope<T>>): AuthResult<T> = try {
        val response = block()
        if (!response.isSuccessful) AuthResult.Failure(AuthErrorMapper.decode(response.code(),
            response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], System.currentTimeMillis()))
        else AuthResult.Success(requireNotNull(response.body()?.data))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
    catch (_: JsonParseException) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
    catch (_: IllegalArgumentException) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
}
