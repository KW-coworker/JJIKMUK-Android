package com.coworker.jjikmuk.feature.auth.data

import retrofit2.Response
import retrofit2.http.*

/** New contracts are decoded separately; refresh orchestration and Google UI are subsequent steps. */
internal interface AuthApi {
    @POST("api/auth/email/send") suspend fun send(@Body body: SendBody): Response<Envelope<DispatchBody>>
    @POST("api/auth/email/verify") suspend fun verify(@Body body: VerifyBody): Response<Envelope<ProofBody>>
    @GET("api/auth/nicknames/availability") suspend fun nickname(@Query("nickname") nickname: String): Response<Envelope<NicknameBody>>
    @POST("api/auth/signup") suspend fun signUp(@Body body: SignupBody): Response<Envelope<UserBody>>
    @POST("api/auth/login") suspend fun login(@Body body: LoginBody): Response<LoginResponse>
    @POST("api/auth/password/reset") suspend fun reset(@Body body: ResetBody): Response<MessageBody>
    @POST("api/auth/logout") suspend fun logout(@Header("Authorization") authorization: String): Response<Envelope<LogoutBody>>
}
internal class SendBody(val email: String, val purpose: String)
internal class VerifyBody(val email: String, val purpose: String, val code: String)
internal class LoginBody(val email: String, val password: String)
internal class ResetBody(val email: String, val verificationToken: String, val newPassword: String)
internal class SignupBody(val email: String, val password: String, val nickname: String,
    val verificationToken: String, val allergies: String?, val specialDiet: String?)
internal class Envelope<T>(val data: T?)
internal class DispatchBody(val email: String?, val purpose: String?, val expiresInSeconds: Int?,
    val expiresAt: String?, val resendAvailableAt: String?)
internal class ProofBody(val email: String?, val purpose: String?, val verificationToken: String?, val expiresInSeconds: Int?,
    val expiresAt: String?)
internal class NicknameBody(val available: Boolean?)
internal class UserBody(val userId: Long?)
internal class LogoutBody(val userId: Long?, val serverTokenRevoked: Boolean?)
internal class MessageBody(val message: String?)
internal class LoginResponse(val token: String?, val tokenType: String?, val userId: Long?, val email: String?,
    val expiresAt: String?, val expiresInSeconds: Long?, val profileCompleted: Boolean?,
    val refreshToken: String?, val refreshExpiresAt: String?, val authProvider: String?,
    val hasPassword: Boolean?, val isNewUser: Boolean?, val code: String?, val loginGranted: Boolean?)
internal class ProfileBody(val id: Long?, val email: String?, val nickname: String?,
    val allergies: String?, val diseases: String?, val specialDiet: String?, val dislikedIngredients: String?,
    val role: String?, val authProvider: String?, val profileCompleted: Boolean?,
    val hasPassword: Boolean?, val photoUrl: String?)
