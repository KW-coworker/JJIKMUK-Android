package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRefreshTest {
    private var now = 0L
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { now }
    private var refreshCalls = 0
    private val deadline = 2_592_000_000L
    private fun login(token: String = "old", id: Long = 1, expiry: Long = now + 900_000,
        refreshExpiry: Long = deadline, completed: Boolean = true) =
        AuthLogin(token, id, "$id@example.com", "unused", 900, completed,
            RefreshCredentials("refresh-$token", refreshExpiry), AuthProvider.LOCAL, true, false, expiry)
    private suspend fun signIn(token: String = "old", id: Long = 1, completed: Boolean = true) {
        assertTrue(sessions.acceptLogin(sessions.beginLogin(), login(token, id, completed = completed), now))
    }
    private fun repo(block: suspend (String) -> AuthResult<AuthLogin> = { AuthResult.Success(login("new")) }) =
        object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                refreshCalls++
                assertTrue(store.saved!!.refreshPending) // No network before durable marker.
                return block(refreshToken)
            }
        }
    private fun error(code: String, status: Int = 401) = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, status, code))

    @Test fun `concurrent expiry shares one rotation and stale 401 reuses its result`() = runTest {
        signIn()
        val observed = sessions.state.value
        now = 900_000
        val gate = CompletableDeferred<Unit>()
        val repository = repo { token -> assertEquals("refresh-old", token); gate.await(); AuthResult.Success(login("new")) }
        val calls = List(8) { async { sessions.accessForRequest(repository) } }
        runCurrent()
        assertEquals(1, refreshCalls)
        assertTrue(sessions.state.value.refreshing)
        assertTrue(sessions.state.value.canEnterHome) // No intermediate navigation out of Home.
        gate.complete(Unit)
        calls.awaitAll().forEach { assertEquals("new", (it as AuthResult.Success).value.credentials!!.accessToken) }
        assertEquals(1, refreshCalls)
        assertEquals("refresh-new", store.saved!!.refresh!!.token)
        assertFalse(store.saved!!.refreshPending)
        assertEquals(deadline, store.saved!!.refresh!!.expiresAtMillis)
        assertTrue(sessions.renewAccess(observed, repository) is AuthResult.Success)
        assertEquals(1, refreshCalls)
    }

    @Test fun `process restart with pending rotation requires login without replay`() = runTest {
        signIn()
        store.saved = store.saved!!.pendingRefresh()
        val restarted = AuthSessionManager(store) { now }
        restarted.restore()
        assertNull(store.saved)
        assertEquals("REFRESH_OUTCOME_UNKNOWN", restarted.state.value.failure!!.code)
        assertTrue(restarted.accessForRequest(repo(), requireCompletedProfile = false) is AuthResult.Failure)
        assertEquals(0, refreshCalls)
    }

    @Test fun `cancelled waiter does not cancel shared consuming operation`() = runTest {
        signIn(); now = 900_000
        val gate = CompletableDeferred<Unit>()
        val repository = repo { gate.await(); AuthResult.Success(login("new")) }
        val first = async { sessions.accessForRequest(repository) }; runCurrent()
        val second = async { sessions.accessForRequest(repository) }; runCurrent()
        first.cancel(); runCurrent()
        gate.complete(Unit)
        first.join()
        assertTrue(first.isCancelled)
        assertTrue(second.await() is AuthResult.Success)
        assertEquals(1, refreshCalls)
        assertEquals("refresh-new", store.saved!!.refresh!!.token)
    }

    @Test fun `logout and account switch discard late rotation even for same user`() = runTest {
        for (nextId in listOf<Long?>(null, 1, 2)) {
            signIn(); now += 900_000
            val gate = CompletableDeferred<Unit>()
            val request = async { sessions.accessForRequest(repo { gate.await(); AuthResult.Success(login("late")) }) }
            runCurrent()
            sessions.clear()
            if (nextId != null) signIn("next", nextId)
            gate.complete(Unit)
            assertEquals("SESSION_CHANGED", (request.await() as AuthResult.Failure).error.code)
            if (nextId == null) assertNull(store.saved)
            else { assertEquals("refresh-next", store.saved!!.refresh!!.token); assertEquals(nextId, store.saved!!.userId) }
        }
    }

    @Test fun `unknown response invalid and reused refresh require login and never retry`() = runTest {
        for (response in listOf(
            AuthResult.Failure(AuthFailure(AuthFailureKind.Network)),
            AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)),
            error("REFRESH_TOKEN_INVALID"), error("REFRESH_TOKEN_REUSED"),
        )) {
            signIn(); now += 900_000
            val before = refreshCalls
            val repository = repo { response }
            assertTrue(sessions.accessForRequest(repository) is AuthResult.Failure)
            assertNull(store.saved); assertFalse(sessions.state.value.canEnterHome)
            sessions.accessForRequest(repository)
            assertEquals(before + 1, refreshCalls)
        }
    }

    @Test fun `server failure quarantines credentials without guessing retry safety`() = runTest {
        for (status in listOf(503, 429, 408, 400)) {
            signIn(); now += 900_000
            val repository = repo { error("AUTH_SERVICE_UNAVAILABLE", status) }
            val before = refreshCalls
            val result = sessions.accessForRequest(repository) as AuthResult.Failure
            assertEquals("REFRESH_RETRY_UNCONFIRMED", result.error.code)
            assertEquals(status, result.error.status)
            assertEquals("refresh-old", store.saved!!.refresh!!.token)
            assertTrue(store.saved!!.refreshPending)
            assertFalse(sessions.state.value.canEnterHome)
            sessions.accessForRequest(repository, requireCompletedProfile = false)
            assertEquals(before + 1, refreshCalls)
        }
    }

    @Test fun `unconfigured repository retains credentials without pending marker`() = runTest {
        signIn(); now = 900_000
        val result = sessions.accessForRequest(TestAuthRepository()) as AuthResult.Failure
        assertEquals(AuthFailureKind.NotConfigured, result.error.kind)
        assertEquals("refresh-old", store.saved!!.refresh!!.token)
        assertFalse(store.saved!!.refreshPending)
        assertFalse(sessions.state.value.canEnterHome)
    }

    @Test fun `failed pending save prevents network and failed replacement prevents old token reuse`() = runTest {
        signIn(); now = 900_000; store.failWrite = true
        assertTrue(sessions.accessForRequest(repo()) is AuthResult.Failure)
        assertEquals(0, refreshCalls); assertNull(store.saved)
        store.failWrite = false; signIn(); now += 900_000
        assertTrue(sessions.accessForRequest(repo {
            store.failWrite = true; AuthResult.Success(login("new"))
        }) is AuthResult.Failure)
        assertEquals(1, refreshCalls); assertNull(store.saved)
        assertTrue(sessions.state.value.storageFailed)
    }

    @Test fun `foreign identity unrotated token or extended refresh expiry is rejected`() = runTest {
        val invalid = listOf<() -> AuthLogin>(
            { login("new", id = 2) }, { login("old") },
            { login("new", refreshExpiry = deadline + 1) }, { login("new", expiry = now) },
            { AuthLogin("new", 1, "other@example.com", "unused", 900, true,
                RefreshCredentials("refresh-new", deadline), AuthProvider.LOCAL, true, false, now + 900_000) },
        )
        for (body in invalid) {
            signIn(); now += 900_000
            assertEquals("REFRESH_OUTCOME_UNKNOWN", (sessions.accessForRequest(repo { AuthResult.Success(body()) }) as AuthResult.Failure).error.code)
            assertNull(store.saved)
        }
    }

    @Test fun `restored and incomplete sessions can refresh for me without gaining Home access`() = runTest {
        signIn(completed = false)
        val restarted = AuthSessionManager(store) { now }
        restarted.restore(); now = 900_000
        val result = restarted.accessForRequest(repo { AuthResult.Success(login("new", completed = false)) },
            requireCompletedProfile = false)
        assertTrue(result is AuthResult.Success)
        assertFalse(restarted.state.value.canEnterHome)
        assertFalse(restarted.state.value.validatedThisRun)
        assertTrue(restarted.accessForRequest(repo()) is AuthResult.Failure)
    }

    @Test fun `safe read retries once while mutations never replay`() = runTest {
        for (safeRead in listOf(false, true)) {
            signIn()
            val requests = AuthenticatedRequests(sessions, repo())
            var calls = 0
            val result = requests.execute(retrySafeRead = safeRead) { token ->
                calls++
                if (token == "old") error("AUTH_TOKEN_EXPIRED") else AuthResult.Success("profile")
            }
            assertEquals(if (safeRead) 2 else 1, calls)
            if (safeRead) assertEquals(AuthResult.Success("profile"), result)
            else assertEquals("AUTH_REFRESHED_RETRY_REQUIRED", (result as AuthResult.Failure).error.code)
        }
    }

    @Test fun `repeated expired read response never loops and old invalid response cannot clear replacement`() = runTest {
        signIn()
        val old = sessions.state.value
        var calls = 0
        val result = AuthenticatedRequests(sessions, repo()).execute<Unit>(retrySafeRead = true) {
            calls++; error("AUTH_TOKEN_EXPIRED")
        }
        assertTrue(result is AuthResult.Failure)
        assertEquals(2, calls); assertEquals(1, refreshCalls)
        sessions.rejectAccess(old)
        assertEquals("refresh-new", store.saved!!.refresh!!.token)
    }

    @Test fun `ordinary request network and permission failures never refresh or clear session`() = runTest {
        signIn()
        val requests = AuthenticatedRequests(sessions, repo())
        for (failure in listOf(AuthResult.Failure(AuthFailure(AuthFailureKind.Network)), error("ACCESS_DENIED", 403),
            error("AUTH_SERVICE_UNAVAILABLE", 503), error("LOGIN_FAILED"))) {
            assertEquals(failure, requests.execute<Unit>(retrySafeRead = true) { failure })
            assertTrue(sessions.state.value.canEnterHome)
        }
        assertEquals(0, refreshCalls)
    }

    @Test fun `expired fixed refresh never reaches server`() = runTest {
        signIn(); now = deadline
        assertTrue(sessions.accessForRequest(repo()) is AuthResult.Failure)
        assertEquals(0, refreshCalls); assertNull(store.saved)
    }
}
