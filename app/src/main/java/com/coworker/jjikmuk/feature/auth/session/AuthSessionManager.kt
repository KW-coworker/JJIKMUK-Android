package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serializes persistence and account transitions. Generation guards include the same account relogging. */
class AuthSessionManager(
    private val store: SessionStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val refreshMutex = Mutex()
    private val mutableState = MutableStateFlow(SessionSnapshot())
    val state = mutableState.asStateFlow()
    private var initialized = false

    suspend fun restore() = mutex.withLock {
        if (initialized) return@withLock
        initialized = true
        try {
            val saved = store.read()
            saved?.validate()
            if (saved?.refreshPending == true) clearLocked(failure = failure("REFRESH_OUTCOME_UNKNOWN"))
            else if (saved != null && saved.expiresAtMillis <= nowMillis() && !saved.hasUsableRefresh(nowMillis())) clearLocked()
            else mutableState.value = SessionSnapshot(credentials = saved)
        } catch (cancelled: CancellationException) {
            initialized = false
            throw cancelled
        } catch (_: Exception) {
            clearLocked() // Corrupt/missing key or unreadable storage: no authenticated UI.
        }
    }

    suspend fun beginLogin(): Long = withContext(NonCancellable) {
        mutex.withLock {
            initialized = true
            clearLocked()
            mutableState.value.generation
        }
    }

    suspend fun acceptLogin(attempt: Long, login: AuthLogin, requestStartedAt: Long): Boolean =
        withContext(NonCancellable) {
            mutex.withLock {
                if (mutableState.value.generation != attempt) return@withLock false
                try {
                    val credentials = credentials(login, requestStartedAt)
                    store.write(credentials)
                    mutableState.value = SessionSnapshot(attempt, credentials, validatedThisRun = true)
                    true
                } catch (_: Exception) {
                    clearLocked(storageFailed = true)
                    false
                }
            }
        }

    suspend fun clearIfCurrent(generation: Long) = withContext(NonCancellable) {
        mutex.withLock {
            if (mutableState.value.generation == generation) clearLocked()
        }
    }

    suspend fun clearForEmail(email: String) = withContext(NonCancellable) {
        mutex.withLock {
            val current = mutableState.value.credentials
            if (current != null && normalizeAuthEmail(current.email) == normalizeAuthEmail(email)) clearLocked()
        }
    }

    suspend fun clear() = withContext(NonCancellable) { mutex.withLock { clearLocked() } }

    /** Clear locally first. This captures the OLD token; its response can never clear a newer session. */
    suspend fun logout(repository: AuthRepository) {
        val previous = withContext(NonCancellable) {
            mutex.withLock {
                val token = mutableState.value.credentials?.accessToken
                clearLocked()
                token
            }
        }
        if (previous != null) repository.logout(previous)
    }

    /** Incomplete/restored users may request me/complete when explicitly allowed; this never grants Home. */
    suspend fun accessForRequest(repository: AuthRepository, requireCompletedProfile: Boolean = true): AuthResult<SessionSnapshot> {
        val snapshot = state.value
        val saved = snapshot.credentials ?: return rejected("AUTHENTICATION_REQUIRED")
        if (saved.refreshPending) return AuthResult.Failure(snapshot.failure ?: failure("REFRESH_OUTCOME_UNKNOWN"))
        if (requireCompletedProfile && !snapshot.canEnterHome) return rejected("AUTHENTICATION_REQUIRED")
        val result = if (!snapshot.refreshing && saved.expiresAtMillis > nowMillis()) AuthResult.Success(snapshot)
            else renewAccess(snapshot, repository)
        return if (requireCompletedProfile && result is AuthResult.Success && !result.value.canEnterHome)
            rejected("AUTHENTICATION_REQUIRED") else result
    }

    /** A stale 401 joins the prior rotation rather than consuming the replacement token a second time. */
    suspend fun renewAccess(observed: SessionSnapshot, repository: AuthRepository): AuthResult<SessionSnapshot> {
        currentCoroutineContext().ensureActive()
        return refreshMutex.withLock {
            // Once dispatch may consume Refresh, caller cancellation must not interrupt durable completion.
            withContext(NonCancellable) {
                var source: SessionSnapshot? = null
                val immediate: AuthResult<SessionSnapshot>? = mutex.withLock {
                    val current = state.value
                    val saved = current.credentials
                    if (current.generation != observed.generation || saved == null) return@withLock rejected("SESSION_CHANGED")
                    if (saved.refreshPending) return@withLock AuthResult.Failure(current.failure ?: failure("REFRESH_OUTCOME_UNKNOWN"))
                    if (saved !== observed.credentials) {
                        return@withLock if (saved.expiresAtMillis > nowMillis()) AuthResult.Success(current)
                            else rejected("AUTH_TOKEN_EXPIRED")
                    }
                    if (!saved.hasUsableRefresh(nowMillis())) {
                        val reason = failure("REFRESH_TOKEN_INVALID")
                        clearLocked(failure = reason)
                        return@withLock AuthResult.Failure(reason)
                    }
                    try {
                        store.write(saved.pendingRefresh()) // Must succeed BEFORE the consuming HTTP request.
                    } catch (_: Exception) {
                        clearLocked(storageFailed = true, failure = failure("SESSION_STORAGE_FAILED"))
                        return@withLock rejected("SESSION_STORAGE_FAILED")
                    }
                    source = current
                    mutableState.value = SessionSnapshot(current.generation, saved,
                        current.validatedThisRun, refreshing = true)
                    null
                }
                if (immediate != null) return@withContext immediate
                val previous = requireNotNull(source)
                val saved = requireNotNull(previous.credentials)
                val response = try { repository.refresh(requireNotNull(saved.refresh).token) }
                    catch (_: Exception) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
                mutex.withLock {
                    if (state.value.generation != previous.generation) return@withLock rejected("SESSION_CHANGED")
                    when (response) {
                        is AuthResult.Success -> {
                            val replacement = try {
                                credentials(response.value, 0).also {
                                    require(it.userId == saved.userId && normalizeAuthEmail(it.email) == normalizeAuthEmail(saved.email))
                                    val rotated = requireNotNull(it.refresh)
                                    require(rotated.token != saved.refresh!!.token)
                                    require(rotated.expiresAtMillis <= saved.refresh.expiresAtMillis) // Fixed lifetime, never extend.
                                }
                            } catch (_: Exception) {
                                clearLocked(failure = failure("REFRESH_OUTCOME_UNKNOWN"))
                                return@withLock rejected("REFRESH_OUTCOME_UNKNOWN")
                            }
                            try { store.write(replacement) } catch (_: Exception) {
                                clearLocked(storageFailed = true, failure = failure("SESSION_STORAGE_FAILED"))
                                return@withLock rejected("SESSION_STORAGE_FAILED")
                            }
                            val updated = SessionSnapshot(previous.generation, replacement, previous.validatedThisRun)
                            mutableState.value = updated
                            AuthResult.Success(updated)
                        }
                        is AuthResult.Failure -> {
                            val error = response.error
                            when {
                                error.kind == AuthFailureKind.NotConfigured -> {
                                    // The repository guarantees that no HTTP request was issued.
                                    try { store.write(saved) } catch (_: Exception) {
                                        clearLocked(storageFailed = true, failure = failure("SESSION_STORAGE_FAILED"))
                                        return@withLock rejected("SESSION_STORAGE_FAILED")
                                    }
                                    mutableState.value = SessionSnapshot(previous.generation, saved, failure = error)
                                    response
                                }
                                error.kind == AuthFailureKind.Server && error.status == 401 &&
                                    error.code in setOf("REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_REUSED") -> {
                                    clearLocked(failure = error)
                                    response
                                }
                                error.kind == AuthFailureKind.Network || error.kind == AuthFailureKind.InvalidResponse -> {
                                    clearLocked(failure = failure("REFRESH_OUTCOME_UNKNOWN"))
                                    rejected("REFRESH_OUTCOME_UNKNOWN")
                                }
                                else -> {
                                    // Until BE confirms which errors are pre-consumption, keep but quarantine.
                                    // No retry of this Refresh, including 503/429 + Retry-After: 0.
                                    val blocked = error.copy(code = "REFRESH_RETRY_UNCONFIRMED")
                                    mutableState.value = SessionSnapshot(previous.generation, saved.pendingRefresh(), failure = blocked)
                                    AuthResult.Failure(blocked)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun isSameSession(snapshot: SessionSnapshot): Boolean = state.value.generation == snapshot.generation && state.value.credentials != null

    fun isCurrent(snapshot: SessionSnapshot): Boolean = mutableState.value.generation == snapshot.generation &&
        mutableState.value.canEnterHome && (snapshot.credentials?.expiresAtMillis ?: 0) > nowMillis()

    /** An invalid OLD Access token must not clear an already rotated pair. */
    suspend fun rejectAccess(observed: SessionSnapshot) = withContext(NonCancellable) {
        mutex.withLock {
            if (isSameSession(observed) && state.value.credentials === observed.credentials)
                clearLocked(failure = failure("AUTH_TOKEN_INVALID"))
        }
    }

    private fun credentials(login: AuthLogin, requestStartedAt: Long): SessionCredentials {
        val expires = login.expiresAtMillis ?: run {
            require(login.refresh == null)
            Math.addExact(requestStartedAt, Math.multiplyExact(login.expiresInSeconds, 1000L))
        }
        require(expires > nowMillis())
        require(login.refresh == null || login.refresh.expiresAtMillis > nowMillis())
        return SessionCredentials(login.token, login.userId, login.email, expires,
            login.profileCompleted, login.refresh, login.authProvider, login.hasPassword).also { it.validate() }
    }

    private fun failure(code: String) = AuthFailure(AuthFailureKind.Server, code = code)
    private fun rejected(code: String) = AuthResult.Failure(failure(code))

    private suspend fun clearLocked(storageFailed: Boolean = false, failure: AuthFailure? = null) = withContext(NonCancellable) {
        val nextGeneration = mutableState.value.generation + 1
        // Revoke in-memory eligibility BEFORE touching disk or awaiting a network operation.
        mutableState.value = SessionSnapshot(nextGeneration, storageFailed = storageFailed, failure = failure)
        var failed = storageFailed
        try { store.clear() } catch (_: Exception) { failed = true }
        mutableState.value = SessionSnapshot(nextGeneration, storageFailed = failed, failure = failure)
    }
}
