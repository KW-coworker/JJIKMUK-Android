package com.coworker.jjikmuk.feature.auth.data

import retrofit2.Response
import retrofit2.http.*

/** Google/linking and refresh are intentionally absent until their revised contracts arrive. */
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
internal class DispatchBody(val email: String?, val purpose: String?, val expiresInSeconds: Int?)
internal class ProofBody(val email: String?, val purpose: String?, val verificationToken: String?, val expiresInSeconds: Int?)
internal class NicknameBody(val available: Boolean?)
internal class UserBody(val userId: Long?)
internal class LogoutBody(val userId: Long?, val serverTokenRevoked: Boolean?)
internal class MessageBody(val message: String?)
internal class LoginResponse(val token: String?, val tokenType: String?, val userId: Long?, val email: String?,
    val expiresAt: String?, val expiresInSeconds: Long?, val profileCompleted: Boolean?)
internal class ErrorBody(val code: String?, val field: String?)
