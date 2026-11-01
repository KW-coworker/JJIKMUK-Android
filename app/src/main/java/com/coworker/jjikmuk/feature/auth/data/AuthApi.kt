package com.coworker.jjikmuk.feature.auth.data

import retrofit2.Response
import retrofit2.http.*

/** Service endpoints; Google UI and profile completion are subsequent steps. */
internal interface AuthApi {
    @POST("api/auth/email/send") suspend fun send(@Body body: SendBody): Response<Envelope<DispatchBody>>
    @POST("api/auth/email/verify") suspend fun verify(@Body body: VerifyBody): Response<Envelope<ProofBody>>
    @GET("api/auth/nicknames/availability") suspend fun nickname(@Query("nickname") nickname: String): Response<Envelope<NicknameBody>>
    @POST("api/auth/signup") suspend fun signUp(@Body body: SignupBody): Response<Envelope<UserBody>>
    @POST("api/auth/login") suspend fun login(@Body body: LoginBody): Response<LoginResponse>
    @POST("api/auth/refresh") suspend fun refresh(@Body body: RefreshBody): Response<LoginResponse>
    @POST("api/auth/password/reset") suspend fun reset(@Body body: ResetBody): Response<MessageBody>
    @GET("api/users/me") suspend fun me(@Header("Authorization") authorization: String): Response<Envelope<ProfileBody>>
    @PATCH("api/users/me") suspend fun patchProfile(@Header("Authorization") authorization: String,
        @Body patch: com.coworker.jjikmuk.feature.auth.domain.ProfilePatch): Response<Envelope<ProfileBody>>
    @POST("api/auth/logout/refresh") suspend fun logoutWithRefresh(@Body body: RefreshBody): Response<Envelope<LogoutBody>>
    @GET("api/auth/nicknames/availability") suspend fun ownNickname(@Header("Authorization") authorization: String, @Query("nickname") nickname: String): Response<Envelope<NicknameBody>>
    @POST("api/auth/logout") suspend fun logout(@Header("Authorization") authorization: String): Response<Envelope<LogoutBody>>
    @POST("api/users/me/profile/complete") suspend fun complete(@Header("Authorization") authorization: String,
        @Body body: com.coworker.jjikmuk.feature.auth.domain.ProfileSubmission): Response<Envelope<ProfileBody>>
    @POST("api/auth/google") suspend fun google(@Body body: GoogleBody): Response<com.google.gson.JsonObject>
    @POST("api/auth/google/link") suspend fun linkGoogle(@Body body: GoogleLinkBody): Response<LoginResponse>
    @POST("api/auth/google/link/cancel") suspend fun cancelGoogle(@Body body: GoogleCancelBody): Response<MessageBody>
    @Streaming @GET("api/users/me/photo") suspend fun photo(@Header("Authorization") authorization: String): Response<okhttp3.ResponseBody>
    @Multipart @PUT("api/users/me/photo") suspend fun uploadPhoto(@Header("Authorization") authorization: String,
        @Part file: okhttp3.MultipartBody.Part): Response<Envelope<PhotoReceiptBody>>
    @DELETE("api/users/me/photo") suspend fun deletePhoto(@Header("Authorization") authorization: String): Response<MessageBody>
    @GET("api/users/me/family-profiles") suspend fun families(@Header("Authorization") authorization: String): Response<Envelope<List<FamilyBody>>>
    @GET("api/users/me/family-profiles/{id}") suspend fun family(@Header("Authorization") authorization: String, @Path("id") id: Long): Response<Envelope<FamilyBody>>
    @POST("api/users/me/family-profiles") suspend fun createFamily(@Header("Authorization") authorization: String,
        @Body body: com.coworker.jjikmuk.feature.auth.domain.ProfileSubmission): Response<Envelope<FamilyBody>>
    @PATCH("api/users/me/family-profiles/{id}") suspend fun patchFamily(@Header("Authorization") authorization: String,
        @Path("id") id: Long, @Body body: okhttp3.RequestBody): Response<Envelope<FamilyBody>>
    @DELETE("api/users/me/family-profiles/{id}") suspend fun deleteFamily(@Header("Authorization") authorization: String,
        @Path("id") id: Long, @Query("version") version: Long): Response<MessageBody>
    @PUT("api/users/{id}/password") suspend fun changePassword(@Header("Authorization") authorization: String,
        @Path("id") id: Long, @Body body: ChangePasswordBody): Response<MessageBody>
    @DELETE("api/users/{id}") suspend fun deleteAccount(@Header("Authorization") authorization: String, @Path("id") id: Long): Response<MessageBody>
}
internal class GoogleBody(val idToken: String)
internal class GoogleLinkBody(val linkToken: String, val currentPassword: String)
internal class GoogleCancelBody(val linkToken: String)
internal class ChangePasswordBody(val currentPassword: String, val newPassword: String)
internal class PhotoReceiptBody(val photoUrl: String?, val authenticationRequired: Boolean?)
internal class FamilyBody(val id: Long?, val nickname: String?, val allergies: String?, val specialDiet: String?,
    val diseases: String?, val dislikedIngredients: String?, val version: Long?)
internal class SendBody(val email: String, val purpose: String)
internal class VerifyBody(val email: String, val purpose: String, val code: String)
internal class LoginBody(val email: String, val password: String)
internal class RefreshBody(val refreshToken: String)
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
