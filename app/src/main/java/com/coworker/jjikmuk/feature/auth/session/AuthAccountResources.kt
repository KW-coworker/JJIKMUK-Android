package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex

/** Server family IDs only; the local Room UUIDs are never migrated implicitly. */
class AuthFamilies @Inject constructor(private val repository: AuthRepository, private val requests: AuthenticatedRequests,
    private val sessions: AuthSessionManager) {
    suspend fun list(generation: Long = sessions.state.value.generation) = execute(generation, true, repository::families)
    suspend fun get(id: Long, generation: Long = sessions.state.value.generation) = execute(generation, true) { repository.family(it, id) }
    suspend fun create(input: FamilySubmission, generation: Long = sessions.state.value.generation) = execute(generation) { repository.createFamily(it, input) }
    suspend fun patch(id: Long, version: Long, patch: FamilyPatch, generation: Long = sessions.state.value.generation) = execute(generation) { repository.patchFamily(it, id, version, patch) }
    suspend fun delete(id: Long, version: Long, generation: Long = sessions.state.value.generation) = execute(generation) { repository.deleteFamily(it, id, version) }
    private suspend fun <T> execute(generation: Long, read: Boolean = false, call: suspend (String) -> AuthResult<T>): AuthResult<T> =
        requests.execute(retrySafeRead = read) { token ->
            if (sessions.state.value.generation != generation) AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
            else call(token)
        }
}

/** Mutations never replay. UI confirms intent and handles an unknown outcome separately. */
class AuthAccountActions @Inject constructor(private val repository: AuthRepository,
    private val requests: AuthenticatedRequests, private val sessions: AuthSessionManager) {
    private val mutex = Mutex()
    suspend fun changePassword(currentPassword: String, newPassword: String, generation: Long = sessions.state.value.generation): AuthResult<Unit> = perform(generation) { token, id ->
        repository.changePassword(token, id, currentPassword, newPassword)
    }
    suspend fun deleteAccount(generation: Long = sessions.state.value.generation): AuthResult<Unit> = perform(generation, repository::deleteAccount)

    private suspend fun perform(generation: Long, call: suspend (String, Long) -> AuthResult<Unit>): AuthResult<Unit> {
        val snapshot = sessions.state.value
        val id = snapshot.credentials?.userId ?: return changed()
        if (snapshot.generation != generation) return changed()
        // No queued second mutation after a double tap; caller retries only after resolving its state.
        if (!mutex.tryLock()) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "REQUEST_IN_PROGRESS"))
        try {
            if (!sessions.isSameSession(snapshot)) return changed()
            val result = requests.execute { token ->
                // Refresh may suspend while another account signs in. Never send its token with the old ID.
                if (!sessions.isSameSession(snapshot)) changed() else call(token, id)
            }
            if (result is AuthResult.Success) sessions.clearIfCurrent(snapshot.generation)
            return result
        } finally { mutex.unlock() }
    }

    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
}
