package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex

/** Server family IDs only; the local Room UUIDs are never migrated implicitly. */
class AuthFamilies @Inject constructor(private val repository: AuthRepository, private val requests: AuthenticatedRequests) {
    suspend fun list() = requests.execute(retrySafeRead = true, request = repository::families)
    suspend fun get(id: Long) = requests.execute(retrySafeRead = true) { repository.family(it, id) }
    suspend fun create(input: ProfileSubmission) = requests.execute { repository.createFamily(it, input) }
    suspend fun patch(id: Long, version: Long, patch: ProfilePatch) = requests.execute { repository.patchFamily(it, id, version, patch) }
    suspend fun delete(id: Long, version: Long) = requests.execute { repository.deleteFamily(it, id, version) }
}

/** UI confirmation and account-owned local-data cleanup must be provided by the caller. */
class AuthAccountActions @Inject constructor(private val repository: AuthRepository,
    private val requests: AuthenticatedRequests, private val sessions: AuthSessionManager) {
    private val mutex = Mutex()
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit> = perform { token, id ->
        repository.changePassword(token, id, currentPassword, newPassword)
    }
    suspend fun deleteAccount(): AuthResult<Unit> = perform(repository::deleteAccount)

    private suspend fun perform(call: suspend (String, Long) -> AuthResult<Unit>): AuthResult<Unit> {
        val snapshot = sessions.state.value
        val id = snapshot.credentials?.userId ?: return changed()
        // No queued second mutation after a double tap; caller retries only after resolving its state.
        if (!mutex.tryLock()) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "REQUEST_IN_PROGRESS"))
        try {
            if (!sessions.isSameSession(snapshot)) return changed()
            val result = requests.execute { call(it, id) }
            if (result is AuthResult.Success) sessions.clearIfCurrent(snapshot.generation)
            return result
        } finally { mutex.unlock() }
    }

    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
}
