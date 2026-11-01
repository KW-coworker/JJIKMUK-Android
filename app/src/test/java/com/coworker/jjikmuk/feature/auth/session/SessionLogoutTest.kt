package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLogoutTest {
    private val store = MemorySessionStore()
    private var now = 0L
    private val sessions = AuthSessionManager(store) { now }
    private fun login(token: String = "old", userId: Long = 1) = AuthLogin(token, userId,
        "$userId@example.com", "unused", 900, true, RefreshCredentials("refresh-$token", 2_592_000_000),
        AuthProvider.LOCAL, true, false, now + 900_000)
    private suspend fun signIn(token: String = "old", userId: Long = 1) {
        assertTrue(sessions.acceptLogin(sessions.beginLogin(), login(token, userId), now))
    }

    @Test fun `local exit precedes HTTP and only matching confirmed receipt counts as server logout`() = runTest {
        for (receipt in listOf(LogoutReceipt(1, true), LogoutReceipt(2, true), LogoutReceipt(1, false))) {
            signIn()
            val result = sessions.logout(object : TestAuthRepository() {
                override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                    assertEquals("old", accessToken)
                    assertNull(store.saved); assertNull(sessions.state.value.credentials)
                    return AuthResult.Success(receipt)
                }
            })
            assertTrue(result.localStorageCleared)
            assertEquals(if (receipt == LogoutReceipt(1, true)) ServerLogoutStatus.Confirmed
                else ServerLogoutStatus.Unconfirmed, result.serverStatus)
        }
    }

    @Test fun `network errors expired access and unknown outcome never refresh or restore local session`() = runTest {
        for (failure in listOf(AuthFailure(AuthFailureKind.Network), AuthFailure(AuthFailureKind.NotConfigured),
            AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_EXPIRED"),
            AuthFailure(AuthFailureKind.Server, 503, "AUTH_SERVICE_UNAVAILABLE"))) {
            signIn(); now += 900_000
            var calls = 0
            val result = sessions.logout(object : TestAuthRepository() {
                override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> = error("Logout must not refresh")
                override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                    calls++; return AuthResult.Failure(failure)
                }
            })
            assertEquals(1, calls); assertNull(store.saved)
            assertEquals(ServerLogoutStatus.Unconfirmed, result.serverStatus)
            assertEquals(failure, result.failure)
            val restored = AuthSessionManager(store) { now }
            restored.restore(); assertNull(restored.state.value.credentials)
        }
    }

    @Test fun `duplicate manager logout makes no second network request and invalidates pending login`() = runTest {
        signIn()
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val repo = object : TestAuthRepository() {
            override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                calls++; gate.await(); return AuthResult.Success(LogoutReceipt(1, true))
            }
        }
        val first = async { sessions.logout(repo) }; runCurrent()
        assertEquals(ServerLogoutStatus.NoSession, sessions.logout(repo).serverStatus)
        assertEquals(1, calls)
        gate.complete(Unit); first.await()
        val pendingLogin = sessions.beginLogin()
        sessions.logout(repo)
        assertFalse(sessions.acceptLogin(pendingLogin, login(), now))
    }

    @Test fun `late logout success or failure cannot clear another or same newly logged in account`() = runTest {
        for (id in listOf(1L, 2L)) {
            signIn()
            val gate = CompletableDeferred<Unit>()
            val request = async { sessions.logout(object : TestAuthRepository() {
                override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                    gate.await(); return AuthResult.Success(LogoutReceipt(1, true))
                }
            }) }
            runCurrent(); signIn("new", id)
            gate.complete(Unit)
            val oldResult = request.await()
            assertNotEquals(oldResult.generation, sessions.state.value.generation)
            assertEquals("refresh-new", store.saved!!.refresh!!.token)
            assertEquals(id, store.saved!!.userId)
        }
    }

    @Test fun `logout during rotation clears both tokens immediately and late rotation cannot revive them`() = runTest {
        signIn(); now = 900_000
        val gate = CompletableDeferred<Unit>()
        var logoutCalls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                gate.await(); return AuthResult.Success(login("rotated"))
            }
            override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                logoutCalls++; assertEquals("old", accessToken)
                return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_EXPIRED"))
            }
        }
        val rotation = async { sessions.accessForRequest(repo) }; runCurrent()
        assertTrue(store.saved!!.refreshPending)
        val outcome = sessions.logout(repo)
        assertNull(store.saved); assertFalse(sessions.state.value.canEnterHome)
        assertEquals(ServerLogoutStatus.Unconfirmed, outcome.serverStatus)
        gate.complete(Unit)
        assertEquals("SESSION_CHANGED", (rotation.await() as AuthResult.Failure).error.code)
        assertNull(store.saved); assertEquals(1, logoutCalls)
    }

    @Test fun `cancelling screen does not interrupt already dispatched logout`() = runTest {
        signIn()
        val gate = CompletableDeferred<Unit>()
        var completed = false
        val request = async { sessions.logout(object : TestAuthRepository() {
            override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                gate.await(); completed = true; return AuthResult.Success(LogoutReceipt(1, true))
            }
        }) }
        runCurrent(); request.cancel(); runCurrent()
        assertNull(store.saved)
        gate.complete(Unit); request.join()
        assertTrue(completed); assertNull(sessions.state.value.credentials)
    }

    @Test fun `local storage failure is reported separately and does not retain authenticated UI`() = runTest {
        var failClear = false
        val faultyStore = object : SessionStore {
            override suspend fun read() = store.read()
            override suspend fun write(credentials: SessionCredentials) = store.write(credentials)
            override suspend fun clear() { if (failClear) throw IOException("Test failure") else store.clear() }
        }
        val manager = AuthSessionManager(faultyStore) { now }
        manager.acceptLogin(manager.beginLogin(), login(), now)
        failClear = true
        val result = manager.logout(object : TestAuthRepository() {
            override suspend fun logout(accessToken: String) = AuthResult.Success(LogoutReceipt(1, true))
        })
        assertEquals(ServerLogoutStatus.Confirmed, result.serverStatus)
        assertFalse(result.localStorageCleared)
        assertNull(manager.state.value.credentials)
        assertTrue(manager.state.value.storageFailed)
        manager.restore(); assertNull(manager.state.value.credentials)
    }
}
