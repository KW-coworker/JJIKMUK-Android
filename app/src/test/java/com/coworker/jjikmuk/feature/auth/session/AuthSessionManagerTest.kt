package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionManagerTest {
    private fun login(token: String = "token", completed: Boolean = true) =
        AuthLogin(token, 1, "user@example.com", "unused", 3600, completed)

    private fun refreshLogin(id: Long = 1, accessExpiry: Long = 900_000, refreshExpiry: Long = 2_592_000_000) =
        AuthLogin("access-$id", id, "$id@example.com", "unused", 900, true,
            RefreshCredentials("refresh-$id", refreshExpiry), AuthProvider.LOCAL, true, false, accessExpiry)

    @Test fun `new login persists server absolute expiries and restart does not authorize Home`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { 10_000 }
        assertTrue(sessions.acceptLogin(sessions.beginLogin(), refreshLogin(), 5_000))
        assertEquals(900_000L, store.saved!!.expiresAtMillis) // Not request start + 15 minutes.
        assertEquals(2_592_000_000L, store.saved!!.refresh!!.expiresAtMillis)
        assertEquals(AuthProvider.LOCAL, store.saved!!.authProvider)
        assertEquals(true, store.saved!!.hasPassword)
        val restarted = AuthSessionManager(store) { 10_000 }
        restarted.restore()
        assertEquals("refresh-1", restarted.state.value.credentials!!.refresh!!.token)
        assertFalse(restarted.state.value.canEnterHome)
        assertNull(restarted.activeSnapshot())
    }

    @Test fun `expired access preserves usable refresh but cannot enter Home or send requests`() = runTest {
        val store = MemorySessionStore()
        var now = 0L
        val sessions = AuthSessionManager(store) { now }
        assertTrue(sessions.acceptLogin(sessions.beginLogin(), refreshLogin(), 0))
        val beforeExpiry = sessions.state.value
        now = 900_000
        assertFalse(sessions.isCurrent(beforeExpiry))
        assertNull(sessions.activeSnapshot())
        assertFalse(sessions.state.value.canEnterHome)
        assertEquals("refresh-1", store.saved!!.refresh!!.token)
        val restarted = AuthSessionManager(store) { now }
        restarted.restore()
        assertEquals("refresh-1", restarted.state.value.credentials!!.refresh!!.token)
        var called = false
        AuthenticatedRequests(restarted).execute { called = true; AuthResult.Success(Unit) }
        assertFalse(called)
        now = 2_592_000_000
        val fullyExpired = AuthSessionManager(store) { now }
        fullyExpired.restore()
        assertNull(fullyExpired.state.value.credentials); assertNull(store.saved)
    }

    @Test fun `new account replaces entire pair and late login cannot mix old tokens into it`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { 0 }
        val first = sessions.beginLogin()
        assertTrue(sessions.acceptLogin(first, refreshLogin(1), 0))
        val second = sessions.beginLogin()
        assertNull(store.saved)
        assertTrue(sessions.acceptLogin(second, refreshLogin(2), 0))
        assertFalse(sessions.acceptLogin(first, refreshLogin(1), 0))
        assertEquals(2L, store.saved!!.userId)
        assertEquals("access-2", store.saved!!.accessToken)
        assertEquals("refresh-2", store.saved!!.refresh!!.token)
        store.failWrite = true
        assertFalse(sessions.acceptLogin(sessions.beginLogin(), refreshLogin(3), 0))
        assertNull(store.saved); assertNull(sessions.state.value.credentials)
        assertFalse(sessions.state.value.canEnterHome)
    }

    @Test fun `invalid new login is not saved and never receives a fabricated lifetime`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { 100 }
        for (invalid in listOf(
            refreshLogin(accessExpiry = 100), refreshLogin(refreshExpiry = 100),
            AuthLogin("access", 1, "user@example.com", "unused", 900, true,
                RefreshCredentials("refresh", 999_999), AuthProvider.LOCAL, true),
            AuthLogin("access", 1, "user@example.com", "unused", 900, true,
                RefreshCredentials("refresh", 999_999), expiresAtMillis = 900_000),
        )) {
            assertFalse(sessions.acceptLogin(sessions.beginLogin(), invalid, 0))
            assertNull(store.saved)
        }
    }

    @Test fun `restored session is not trusted without server profile validation and expiry clears it`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { testScheduler.currentTime }
        val attempt = sessions.beginLogin()
        assertTrue(sessions.acceptLogin(attempt, login(), 0))
        assertTrue(sessions.state.value.canEnterHome)
        val restarted = AuthSessionManager(store) { testScheduler.currentTime }
        restarted.restore()
        assertNotNull(restarted.state.value.credentials)
        assertFalse(restarted.state.value.canEnterHome)
        assertNull(restarted.activeSnapshot())
        advanceTimeBy(3600_000)
        val expired = AuthSessionManager(store) { testScheduler.currentTime }
        expired.restore()
        assertNull(expired.state.value.credentials); assertNull(store.saved)
    }

    @Test fun `incomplete profile is persisted but cannot issue protected requests or enter Home`() = runTest {
        val sessions = AuthSessionManager(MemorySessionStore()) { 0 }
        assertTrue(sessions.acceptLogin(sessions.beginLogin(), login(completed = false), 0))
        assertFalse(sessions.state.value.canEnterHome)
        assertNull(sessions.activeSnapshot())
    }

    @Test fun `failed persistence and late login after logout cannot become authenticated`() = runTest {
        val store = MemorySessionStore().apply { failWrite = true }
        val sessions = AuthSessionManager(store) { 0 }
        assertFalse(sessions.acceptLogin(sessions.beginLogin(), login(), 0))
        assertTrue(sessions.state.value.storageFailed); assertNull(store.saved)
        store.failWrite = false
        val pending = sessions.beginLogin()
        sessions.clear()
        assertFalse(sessions.acceptLogin(pending, login(), 0))
    }

    @Test fun `logout clears locally before network and its late response cannot clear a new account session`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { 0 }
        sessions.acceptLogin(sessions.beginLogin(), login("old"), 0)
        val gate = CompletableDeferred<Unit>()
        val repository = object : TestAuthRepository() {
            override suspend fun logout(accessToken: String): AuthResult<Unit> {
                assertEquals("old", accessToken)
                assertNull(store.saved); assertFalse(sessions.state.value.canEnterHome)
                gate.await()
                return AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
            }
        }
        val request = launch { sessions.logout(repository) }; runCurrent()
        assertNull(store.saved)
        sessions.acceptLogin(sessions.beginLogin(), login("new"), 0)
        gate.complete(Unit); request.join()
        assertEquals("new", sessions.state.value.credentials!!.accessToken)
    }

    @Test fun `password reset clears only the matching account`() = runTest {
        val sessions = AuthSessionManager(MemorySessionStore()) { 0 }
        sessions.acceptLogin(sessions.beginLogin(), login(), 0)
        sessions.clearForEmail("other@example.com")
        assertTrue(sessions.state.value.canEnterHome)
        sessions.clearForEmail(" USER@EXAMPLE.COM ")
        assertFalse(sessions.state.value.canEnterHome)
    }

    @Test fun `protected request uses token and discards result from previous session even for same user`() = runTest {
        val sessions = AuthSessionManager(MemorySessionStore()) { 0 }
        sessions.acceptLogin(sessions.beginLogin(), login("old"), 0)
        val gate = CompletableDeferred<Unit>()
        val requests = AuthenticatedRequests(sessions)
        val request = async {
            requests.execute { token ->
                assertEquals("old", token)
                gate.await()
                AuthResult.Success("old private data")
            }
        }
        runCurrent()
        sessions.acceptLogin(sessions.beginLogin(), login("new"), 0)
        gate.complete(Unit)
        assertEquals("SESSION_CHANGED", (request.await() as AuthResult.Failure).error.code)
        assertEquals("new", sessions.state.value.credentials!!.accessToken)
    }

    @Test fun `only explicit session token errors sign out - not network 403 or login failures`() = runTest {
        val sessions = AuthSessionManager(MemorySessionStore()) { 0 }
        val requests = AuthenticatedRequests(sessions)
        sessions.acceptLogin(sessions.beginLogin(), login(), 0)
        for (error in listOf(AuthFailure(AuthFailureKind.Network),
            AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED"),
            AuthFailure(AuthFailureKind.Server, 401, "LOGIN_FAILED"))) {
            requests.execute<Unit> { AuthResult.Failure(error) }
            assertTrue(sessions.state.value.canEnterHome)
        }
        requests.execute<Unit> { AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_EXPIRED")) }
        assertFalse(sessions.state.value.canEnterHome)
    }
}
