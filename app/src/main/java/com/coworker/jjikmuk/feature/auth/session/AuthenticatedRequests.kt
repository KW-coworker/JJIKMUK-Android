package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Use only with a trusted service API's explicit token parameter, never a shared host interceptor. */
class AuthenticatedRequests(private val sessions: AuthSessionManager, private val repository: AuthRepository) {
    /** Only callers performing a safe read may opt into one replay. Mutations default to no replay. */
    suspend fun <T> execute(
        retrySafeRead: Boolean = false,
        requireCompletedProfile: Boolean = true,
        request: suspend (accessToken: String) -> AuthResult<T>,
    ): AuthResult<T> {
        val access = sessions.accessForRequest(repository, requireCompletedProfile)
        if (access is AuthResult.Failure) return access
        val snapshot = (access as AuthResult.Success).value
        val result = request(snapshot.credentials!!.accessToken)
        currentCoroutineContext().ensureActive()
        if (!sessions.isSameSession(snapshot)) return rejected("SESSION_CHANGED")
        if (result is AuthResult.Failure && result.error.status == 403 && result.error.code == "PROFILE_INCOMPLETE") {
            sessions.requireProfileCompletion(snapshot)
            return result
        }
        if (result !is AuthResult.Failure || result.error.status != 401) return result
        if (result.error.code == "AUTH_TOKEN_INVALID") {
            sessions.rejectAccess(snapshot)
            return result
        }
        if (result.error.code == "AUTH_TOKEN_EXPIRED") {
            val refreshed = sessions.renewAccess(snapshot, repository)
            currentCoroutineContext().ensureActive()
            if (refreshed is AuthResult.Failure) return refreshed
            val updated = (refreshed as AuthResult.Success).value
            if (!sessions.isSameSession(updated)) return rejected("SESSION_CHANGED")
            if (requireCompletedProfile && !updated.canEnterHome) return rejected("AUTHENTICATION_REQUIRED")
            if (!retrySafeRead) return rejected("AUTH_REFRESHED_RETRY_REQUIRED")
            val retried = request(updated.credentials!!.accessToken)
            currentCoroutineContext().ensureActive()
            if (!sessions.isSameSession(updated)) return rejected("SESSION_CHANGED")
            if (retried is AuthResult.Failure && retried.error.status == 401 && retried.error.code == "AUTH_TOKEN_INVALID")
                sessions.rejectAccess(updated)
            return retried // No recursive refresh or third request, even if the server again says expired.
        }
        return result
    }

    private fun rejected(code: String) = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = code))
}
