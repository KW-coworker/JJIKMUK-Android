package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** OCR/chat may incur external processing. Refresh before dispatch, never replay after dispatch. */
class AuthorizedOperations @Inject constructor(
    private val sessions: AuthSessionManager, private val repository: AuthRepository,
) {
    suspend fun <T> execute(request: suspend (String) -> AuthResult<T>): AuthResult<T> {
        val owner = sessions.state.value.generation
        val access = sessions.accessForRequest(repository)
        currentCoroutineContext().ensureActive()
        if (access is AuthResult.Failure) return access
        val snapshot = (access as AuthResult.Success).value
        if (snapshot.generation != owner || !sessions.isSameSession(snapshot)) return changed()
        val result = request(snapshot.credentials!!.accessToken)
        currentCoroutineContext().ensureActive()
        if (!sessions.isSameSession(snapshot)) return changed()
        if (result is AuthResult.Failure && result.error.status == 401) {
            // A late old-token 401 must not revoke a new account or an already rotated token pair.
            sessions.rejectAccess(snapshot)
        }
        return result
    }
    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
}
