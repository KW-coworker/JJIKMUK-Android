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
