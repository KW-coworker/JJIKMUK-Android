package com.coworker.jjikmuk.feature.auth.photo

import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex

data class PhotoState(val generation: Long = -1, val photo: ProfilePhoto? = null,
    val busy: Boolean = false, val loaded: Boolean = false, val mustReload: Boolean = false,
    val failure: AuthFailure? = null)

/** Account-scoped, memory-only bytes. No disk/HTTP/image-loader cache and no persistent URI. */
@Singleton
class AuthPhotos @Inject constructor(
    private val repository: AuthRepository,
    private val sessions: AuthSessionManager,
    private val requests: AuthenticatedRequests,
) {
    private val mutableState = MutableStateFlow(PhotoState())
    val state = mutableState.asStateFlow()
    private var operationGeneration = -1L
    private var operationMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            sessions.state.collect { session ->
                if (state.value.generation != session.generation || session.credentials == null)
                    mutableState.value = PhotoState(generation = session.generation)
            }
        }
    }

    suspend fun load(generation: Long = sessions.state.value.generation) = perform(mutation = false, expectedGeneration = generation) { generation -> fetch(generation) }

    suspend fun upload(photo: ProfilePhoto, generation: Long = sessions.state.value.generation) = perform(mutation = true, expectedGeneration = generation) { generation ->
        when (val result = requests.execute(requireCompletedProfile = false) {
            if (current(generation)) repository.uploadPhoto(it, photo) else changed()
        }) {
            is AuthResult.Failure -> result
            is AuthResult.Success -> fetch(generation) // Display the server's normalized copy, not a guessed local success.
        }
    }

    suspend fun delete() = perform(mutation = true) { generation ->
        when (val result = requests.execute(requireCompletedProfile = false) {
            if (current(generation)) repository.deletePhoto(it) else changed()
        }) {
            is AuthResult.Failure -> result
            is AuthResult.Success -> AuthResult.Success(null)
        }
    }

    private suspend fun fetch(generation: Long): AuthResult<ProfilePhoto?> =
        when (val result = requests.execute(retrySafeRead = true, requireCompletedProfile = false) {
            if (current(generation)) repository.photo(it) else changed()
        }) {
            is AuthResult.Failure -> if (result.error.status == 404 && result.error.code == "PHOTO_NOT_FOUND")
                AuthResult.Success(null) else result
            is AuthResult.Success -> result
        }

    private suspend fun perform(mutation: Boolean, expectedGeneration: Long = sessions.state.value.generation, call: suspend (Long) -> AuthResult<ProfilePhoto?>): AuthResult<ProfilePhoto?> {
        val generation = expectedGeneration
        if (!current(generation)) return changed()
        val mutex = synchronized(this) {
            if (operationGeneration != generation) {
                operationGeneration = generation
                operationMutex = Mutex()
            }
            operationMutex
        }
        if (!mutex.tryLock()) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "REQUEST_IN_PROGRESS"))
        try {
            if (!current(generation)) return changed()
            if (state.value.generation != generation) mutableState.value = PhotoState(generation)
            if (mutation && state.value.mustReload) return AuthResult.Failure(
                AuthFailure(AuthFailureKind.Server, code = "PHOTO_RELOAD_REQUIRED"))
            mutableState.update { it.copy(busy = true, failure = null) }
            try {
                val result = call(generation)
                currentCoroutineContext().ensureActive()
                if (!current(generation)) return changed()
                when (result) {
                    is AuthResult.Success -> mutableState.value = PhotoState(generation, result.value, loaded = true)
                    is AuthResult.Failure -> mutableState.update { it.copy(busy = false, failure = result.error,
                        mustReload = it.mustReload || (mutation && (result.error.outcomeUnknown || result.error.code == "AUTH_REFRESHED_RETRY_REQUIRED"))) }
                }
                return result
            } catch (cancelled: CancellationException) {
                if (current(generation)) mutableState.update { it.copy(busy = false,
                    mustReload = it.mustReload || mutation, failure = AuthFailure(AuthFailureKind.Network)) }
                throw cancelled
            }
        } finally { mutex.unlock() }
    }

    private fun current(generation: Long) = sessions.state.value.generation == generation && sessions.state.value.credentials != null
    private fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
}
