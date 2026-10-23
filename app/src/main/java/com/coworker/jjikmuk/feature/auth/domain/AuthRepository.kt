package com.coworker.jjikmuk.feature.auth.domain

/** OTP proof, Google ID token and service access token are deliberately different types. */
enum class EmailPurpose(val wireValue: String, val proofLifetimeSeconds: Int) {
    SignUp("signup", 1800), PasswordReset("password_reset", 300),
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthFailure) : AuthResult<Nothing>
}

enum class AuthFailureKind { NotConfigured, Network, InvalidResponse, Server }
data class AuthFailure(
    val kind: AuthFailureKind,
    val status: Int? = null,
    val code: String? = null,
    val field: String? = null,
) {
    // A lost response may have followed a successful mutation. Never replay automatically.
    val outcomeUnknown: Boolean get() = kind == AuthFailureKind.Network ||
        kind == AuthFailureKind.InvalidResponse || (status ?: 0) >= 500
}

data class EmailDispatch(val email: String, val purpose: EmailPurpose, val expiresInSeconds: Int)
// No generated toString: proofs and passwords must not enter logs through state/DTO logging.
class EmailProof(val token: String, val email: String, val purpose: EmailPurpose, val expiresInSeconds: Int)
class AuthLogin(val token: String, val userId: Long, val email: String, val expiresAt: String,
    val expiresInSeconds: Long, val profileCompleted: Boolean)
class SignUpSubmission(val email: String, val password: String, val nickname: String,
    val verificationToken: String, val allergies: String? = null, val specialDiet: String? = null)

interface AuthRepository {
    suspend fun sendEmail(email: String, purpose: EmailPurpose): AuthResult<EmailDispatch>
    suspend fun verifyEmail(email: String, purpose: EmailPurpose, code: String): AuthResult<EmailProof>
    suspend fun nicknameAvailable(nickname: String): AuthResult<Boolean>
    suspend fun signUp(input: SignUpSubmission): AuthResult<Long>
    suspend fun login(email: String, password: String): AuthResult<AuthLogin>
    suspend fun resetPassword(email: String, proof: String, newPassword: String): AuthResult<Unit>
    suspend fun logout(accessToken: String): AuthResult<Unit>
}

fun normalizeAuthEmail(email: String): String = email.trim().lowercase(java.util.Locale.ROOT)
