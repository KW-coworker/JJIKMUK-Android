package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun refresh(refreshToken: String) = request(200,
        { it.refresh(RefreshBody(refreshToken)) }) {
        AuthResponseMapper.login(it, requireRefresh = true)
    }

    override suspend fun resetPassword(email: String, proof: String, newPassword: String) = request(200,
        { it.reset(ResetBody(normalizeAuthEmail(email), proof, newPassword)) }) {
        requireNotNull(it.message)
        Unit
    }

    override suspend fun logout(accessToken: String) = request(200,
        { it.logout("Bearer $accessToken") }) {
        val data = requireNotNull(it.data)
        val userId = requireNotNull(data.userId).also { id -> require(id > 0) }
        require(data.serverTokenRevoked == true)
        LogoutReceipt(userId, serverTokenRevoked = true)
    }

    override suspend fun me(accessToken: String) = request(200,
        { it.me("Bearer $accessToken") }) { AuthResponseMapper.profile(requireNotNull(it.data)) }

    override suspend fun patchProfile(accessToken: String, patch: ProfilePatch) = request(200,
        { it.patchProfile("Bearer $accessToken", patch) }) { AuthResponseMapper.profile(requireNotNull(it.data)) }

    override suspend fun completeProfile(accessToken: String, input: ProfileSubmission) = request(200,
        { it.complete("Bearer $accessToken", input) }) {
        AuthResponseMapper.profile(requireNotNull(it.data)).also { profile -> require(profile.profileCompleted) }
    }

    override suspend fun google(idToken: String) = request(200,
        { it.google(GoogleBody(idToken)) }, AuthResponseMapper::google)

    override suspend fun linkGoogle(linkToken: String, currentPassword: String) = request(200,
        { it.linkGoogle(GoogleLinkBody(linkToken, currentPassword)) }) { AuthResponseMapper.login(it, requireRefresh = true) }

    override suspend fun cancelGoogleLink(linkToken: String) = request(200,
        { it.cancelGoogle(GoogleCancelBody(linkToken)) }, ::message)

    override suspend fun photo(accessToken: String): AuthResult<ProfilePhoto> = withContext(Dispatchers.IO) {
        request(200, { it.photo("Bearer $accessToken") }) { body ->
            body.use {
                val type = requireNotNull(it.contentType()).let { mime -> "${mime.type}/${mime.subtype}" }
                require(type in setOf("image/jpeg", "image/png"))
                require(it.contentLength() <= ProfilePhoto.MAX_BYTES)
                val output = java.io.ByteArrayOutputStream()
                it.byteStream().use { stream ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= ProfilePhoto.MAX_BYTES)
                        output.write(buffer, 0, count)
                    }
                }
                val bytes = output.toByteArray()
                require(bytes.isNotEmpty())
                ProfilePhoto(bytes, type)
            }
        }
    }

    override suspend fun uploadPhoto(accessToken: String, photo: ProfilePhoto): AuthResult<PhotoUploadReceipt> {
        if (photo.bytes.isEmpty() || photo.bytes.size > ProfilePhoto.MAX_BYTES)
            return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 413, "PHOTO_TOO_LARGE", "file"))
        if (photo.contentType !in setOf("image/jpeg", "image/png"))
            return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "PHOTO_INVALID", "file"))
        val file = MultipartBody.Part.createFormData("file", if (photo.contentType == "image/png") "profile.png" else "profile.jpg",
            photo.bytes.toRequestBody(photo.contentType.toMediaType()))
        return request(200, { it.uploadPhoto("Bearer $accessToken", file) }) {
            val receipt = requireNotNull(it.data)
            require(receipt.photoUrl == "/api/users/me/photo" && receipt.authenticationRequired == true)
            PhotoUploadReceipt(receipt.photoUrl, true)
        }
    }

    override suspend fun deletePhoto(accessToken: String) = request(200,
        { it.deletePhoto("Bearer $accessToken") }, ::message)

    override suspend fun families(accessToken: String) = request(200, { it.families("Bearer $accessToken") }) {
        requireNotNull(it.data).map(::family).also { list -> require(list.map { f -> f.id }.distinct().size == list.size) }
    }

    override suspend fun family(accessToken: String, id: Long) = request(200, {
        require(id > 0); it.family("Bearer $accessToken", id)
    }) { family(requireNotNull(it.data)).also { f -> require(f.id == id) } }

    override suspend fun createFamily(accessToken: String, input: ProfileSubmission) = request(201,
        { it.createFamily("Bearer $accessToken", input) }) { family(requireNotNull(it.data)) }

    override suspend fun patchFamily(accessToken: String, id: Long, version: Long, patch: ProfilePatch) = request(200, {
        require(id > 0 && version >= 0)
        val body = authJson().toJsonTree(patch, ProfilePatch::class.java).asJsonObject.apply { addProperty("version", version) }
        it.patchFamily("Bearer $accessToken", id, body.toString().toRequestBody("application/json".toMediaType()))
    }) { family(requireNotNull(it.data)).also { f -> require(f.id == id && f.version >= version) } }

    override suspend fun deleteFamily(accessToken: String, id: Long, version: Long) = request(200, {
        require(id > 0 && version >= 0); it.deleteFamily("Bearer $accessToken", id, version)
    }, ::message)

    override suspend fun changePassword(accessToken: String, userId: Long, currentPassword: String, newPassword: String) = request(200, {
        require(userId > 0); it.changePassword("Bearer $accessToken", userId, ChangePasswordBody(currentPassword, newPassword))
    }, ::message)

    override suspend fun deleteAccount(accessToken: String, userId: Long) = request(200, {
        require(userId > 0); it.deleteAccount("Bearer $accessToken", userId)
    }, ::message)

    private fun message(body: MessageBody) { require(!body.message.isNullOrBlank()) }
    private fun family(body: FamilyBody) = FamilyProfile(
        requireNotNull(body.id).also { require(it > 0) }, requireNotNull(body.nickname).also { require(it.isNotBlank()) },
        body.allergies, body.specialDiet, body.diseases, body.dislikedIngredients,
        requireNotNull(body.version).also { require(it >= 0) },
    )

    private suspend fun <T, R> request(status: Int, call: suspend (AuthApi) -> Response<T>,
        decode: (T) -> R): AuthResult<R> {
        val configuredApi = api ?: return AuthResult.Failure(AuthFailure(AuthFailureKind.NotConfigured))
        return try {
            val response = call(configuredApi)
            if (!response.isSuccessful) {
                AuthResult.Failure(AuthErrorMapper.decode(response.code(),
                    response.errorBody()?.use { it.string() }, response.headers()["Retry-After"], nowMillis()))
            } else {
                try {
                    require(response.code() == status)
                    AuthResult.Success(decode(requireNotNull(response.body())))
                } finally { (response.body() as? okhttp3.ResponseBody)?.close() }
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
                    // A zero-byte DELETE body also marks bodyless destructive calls as one-shot.
                    val body = request.body ?: if (request.method == "DELETE") ByteArray(0).toRequestBody() else null
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
