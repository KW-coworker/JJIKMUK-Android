package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Use only with a trusted service API's explicit token parameter, never a shared host interceptor. */
class AuthenticatedRequests(private val sessions: AuthSessionManager) {
    suspend fun <T> execute(request: suspend (accessToken: String) -> AuthResult<T>): AuthResult<T> {
        val snapshot = sessions.activeSnapshot() ?: return rejected("AUTHENTICATION_REQUIRED")
        val result = request(snapshot.credentials!!.accessToken)
        currentCoroutineContext().ensureActive()
        if (!sessions.isCurrent(snapshot)) return rejected("SESSION_CHANGED")
        if (result is AuthResult.Failure && result.error.status == 401 &&
            result.error.code in setOf("AUTH_TOKEN_EXPIRED", "AUTH_TOKEN_INVALID")) {
            sessions.clearIfCurrent(snapshot.generation)
        }
        return result
    }

    private fun rejected(code: String) = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = code))
}
