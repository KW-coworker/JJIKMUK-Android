package com.coworker.jjikmuk.feature.auth.domain

open class TestAuthRepository : AuthRepository {
    var sends = 0
    var verifies = 0
    var resets = 0
    var lastCode = ""
    var lastPurpose: EmailPurpose? = null
    var sendResult: AuthResult<EmailDispatch>? = null
    var verifyResult: AuthResult<EmailProof>? = null
    var resetResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var nicknameResult: AuthResult<Boolean> = AuthResult.Success(true)
    override suspend fun sendEmail(email: String, purpose: EmailPurpose): AuthResult<EmailDispatch> {
        sends++; lastPurpose = purpose
        return sendResult ?: AuthResult.Success(EmailDispatch(email, purpose, 300))
    }
    override suspend fun verifyEmail(email: String, purpose: EmailPurpose, code: String): AuthResult<EmailProof> {
        verifies++; lastCode = code; lastPurpose = purpose
        return verifyResult ?: AuthResult.Success(EmailProof("test-proof", email, purpose, purpose.proofLifetimeSeconds))
    }
    override suspend fun nicknameAvailable(nickname: String) = nicknameResult
    override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> = error("Not used by this test")
    override suspend fun login(email: String, password: String): AuthResult<AuthLogin> = error("Not used by this test")
    override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> =
        AuthResult.Failure(AuthFailure(AuthFailureKind.NotConfigured))
    override suspend fun resetPassword(email: String, proof: String, newPassword: String): AuthResult<Unit> {
        resets++
        return resetResult
    }
    override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> = error("Not used by this test")
}
