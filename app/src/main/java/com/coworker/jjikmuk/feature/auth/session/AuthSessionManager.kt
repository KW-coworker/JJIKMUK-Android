package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
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
    private val mutableState = MutableStateFlow(SessionSnapshot())
    val state = mutableState.asStateFlow()
    private var initialized = false

    suspend fun restore() = mutex.withLock {
        if (initialized) return@withLock
        initialized = true
        try {
            val saved = store.read()
            if (saved != null && saved.expiresAtMillis <= nowMillis()) clearLocked()
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
                    val expires = Math.addExact(requestStartedAt, Math.multiplyExact(login.expiresInSeconds, 1000L))
                    require(expires > nowMillis())
                    val credentials = SessionCredentials(login.token, login.userId, login.email, expires, login.profileCompleted)
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

    suspend fun activeSnapshot(): SessionSnapshot? = mutex.withLock {
        val snapshot = mutableState.value
        if (!snapshot.canEnterHome) return@withLock null
        if (snapshot.credentials!!.expiresAtMillis <= nowMillis()) {
            clearLocked()
            return@withLock null
        }
        snapshot
    }

    fun isCurrent(snapshot: SessionSnapshot): Boolean = mutableState.value.generation == snapshot.generation &&
        mutableState.value.canEnterHome && (snapshot.credentials?.expiresAtMillis ?: 0) > nowMillis()

    private suspend fun clearLocked(storageFailed: Boolean = false) = withContext(NonCancellable) {
        val nextGeneration = mutableState.value.generation + 1
        // Revoke in-memory eligibility BEFORE touching disk or awaiting a network operation.
        mutableState.value = SessionSnapshot(nextGeneration, storageFailed = storageFailed)
        var failed = storageFailed
        try { store.clear() } catch (_: Exception) { failed = true }
        mutableState.value = SessionSnapshot(nextGeneration, storageFailed = failed)
    }
}
