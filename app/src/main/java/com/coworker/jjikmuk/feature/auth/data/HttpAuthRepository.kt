package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpAuthRepository private constructor(
    private val api: AuthApi?,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    override suspend fun sendEmail(email: String, purpose: EmailPurpose) = request(200,
        { it.send(SendBody(normalizeAuthEmail(email), purpose.wireValue)) }) { body ->
        val data = requireNotNull(body.data)
        require(data.email == normalizeAuthEmail(email) && data.purpose == purpose.wireValue)
        EmailDispatch(data.email, purpose, requireNotNull(data.expiresInSeconds).also { require(it in 1..300) },
            data.expiresAt?.let(AuthApiTime::parseMillis), data.resendAvailableAt?.let(AuthApiTime::parseMillis))
    }

    override suspend fun verifyEmail(email: String, purpose: EmailPurpose, code: String) = request(200,
        { it.verify(VerifyBody(normalizeAuthEmail(email), purpose.wireValue, code)) }) { body ->
        val data = requireNotNull(body.data)
        require(data.email == normalizeAuthEmail(email) && data.purpose == purpose.wireValue)
        EmailProof(requireNotNull(data.verificationToken).also { require(it.isNotBlank()) }, data.email,
            purpose, requireNotNull(data.expiresInSeconds).also { require(it in 1..purpose.proofLifetimeSeconds) },
            data.expiresAt?.let(AuthApiTime::parseMillis))
    }

    override suspend fun nicknameAvailable(nickname: String) = request(200,
        { it.nickname(normalizeAuthNickname(nickname)) }) { requireNotNull(it.data?.available) }

    override suspend fun signUp(input: SignUpSubmission) = request(201, {
        it.signUp(SignupBody(normalizeAuthEmail(input.email), input.password, normalizeAuthNickname(input.nickname),
            input.verificationToken, input.allergies, input.specialDiet))
    }) { requireNotNull(it.data?.userId).also { id -> require(id > 0) } }

    override suspend fun login(email: String, password: String) = request(200,
        { it.login(LoginBody(normalizeAuthEmail(email), password)) }) {
        AuthResponseMapper.login(it)
    }

    override suspend fun resetPassword(email: String, proof: String, newPassword: String) = request(200,
        { it.reset(ResetBody(normalizeAuthEmail(email), proof, newPassword)) }) {
        requireNotNull(it.message)
        Unit
    }

    override suspend fun logout(accessToken: String) = request(200,
        { it.logout("Bearer $accessToken") }) {
        requireNotNull(it.data?.userId)
        requireNotNull(it.data.serverTokenRevoked)
        Unit
    }

    private suspend fun <T, R> request(status: Int, call: suspend (AuthApi) -> Response<T>,
        decode: (T) -> R): AuthResult<R> {
        val configuredApi = api ?: return AuthResult.Failure(AuthFailure(AuthFailureKind.NotConfigured))
        return try {
            val response = call(configuredApi)
            if (!response.isSuccessful) {
                AuthResult.Failure(AuthErrorMapper.decode(response.code(),
                    response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], nowMillis()))
            } else {
                require(response.code() == status)
                AuthResult.Success(decode(requireNotNull(response.body())))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        } catch (_: JsonParseException) {
            AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse))
        } catch (_: IllegalArgumentException) {
            AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse))
        }
    }

    companion object {
        fun unconfigured(): AuthRepository = HttpAuthRepository(null)
        fun create(baseUrl: HttpUrl, nowMillis: () -> Long = System::currentTimeMillis): AuthRepository {
            require(baseUrl.username.isEmpty() && baseUrl.password.isEmpty())
            require(baseUrl.query == null && baseUrl.fragment == null && baseUrl.encodedPath == "/")
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val body = request.body
                    // Prevent follow-up replays (e.g. 503 + Retry-After: 0) of consuming operations.
                    val singleUseBody = body?.let { original -> object : RequestBody() {
                        override fun contentType() = original.contentType()
                        override fun contentLength() = original.contentLength()
                        override fun isOneShot() = true
                        override fun writeTo(sink: BufferedSink) = original.writeTo(sink)
                    } }
                    chain.proceed(request.newBuilder().method(request.method, singleUseBody).build())
                }
                .build() // No BODY logger, shared auth interceptor, cookies or automatic token refresh.
            return HttpAuthRepository(Retrofit.Builder().baseUrl(baseUrl).client(client)
                .addConverterFactory(GsonConverterFactory.create(authJson())).build().create(AuthApi::class.java), nowMillis)
        }
    }
}
