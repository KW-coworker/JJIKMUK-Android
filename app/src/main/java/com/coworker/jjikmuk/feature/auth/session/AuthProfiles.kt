package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes me/PATCH responses so an older read cannot overwrite a completed edit. */
@Singleton
class AuthProfiles @Inject constructor(
    private val repository: AuthRepository,
    private val sessions: AuthSessionManager,
    private val requests: AuthenticatedRequests,
) {
    private val mutex = Mutex()

    suspend fun load(): AuthResult<AuthProfile> = perform {
        requests.execute(retrySafeRead = true, requireCompletedProfile = false, request = repository::me)
    }

    suspend fun patch(patch: ProfilePatch): AuthResult<AuthProfile> {
        val generation = sessions.state.value.generation
        return perform {
            requests.execute(requireCompletedProfile = false) { token ->
                if (sessions.state.value.generation != generation) changed() else repository.patchProfile(token, patch)
            }
        }
    }

    suspend fun complete(input: ProfileSubmission, generation: Long = sessions.state.value.generation): AuthResult<AuthProfile> = perform {
        if (sessions.state.value.generation != generation) return@perform changed()
        requests.execute(requireCompletedProfile = false) { token ->
            if (sessions.state.value.generation != generation) changed() else repository.completeProfile(token, input)
        }
    }

    private suspend fun perform(request: suspend () -> AuthResult<AuthProfile>): AuthResult<AuthProfile> {
        val generation = sessions.state.value.generation
        return mutex.withLock {
            if (sessions.state.value.generation != generation) return@withLock changed()
            when (val result = request()) {
                is AuthResult.Failure -> result
                is AuthResult.Success -> sessions.acceptProfile(generation, result.value)
            }
        }
    }

    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
}
