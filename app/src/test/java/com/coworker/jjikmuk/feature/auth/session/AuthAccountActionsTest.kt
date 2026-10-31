package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAccountActionsTest {
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { 0L }
    private var response: AuthResult<Unit> = AuthResult.Success(Unit)
    private var calls = 0
    private val repo = object : TestAuthRepository() {
        override suspend fun changePassword(accessToken: String, userId: Long, currentPassword: String, newPassword: String): AuthResult<Unit> {
            assertEquals(1L, userId); calls++; return response
        }
        override suspend fun deleteAccount(accessToken: String, userId: Long): AuthResult<Unit> { calls++; return response }
    }
    private suspend fun ready() {
        store.saved = SessionCredentials("a", 1, "1@example.com", 900_000, true)
        sessions.restore()
        sessions.acceptProfile(sessions.state.value.generation, AuthProfile(1, "1@example.com", "사용자", null, null, null,
            null, "USER", AuthProvider.LOCAL, true, true, null))
    }
    private fun actions(repository: AuthRepository = repo) = AuthAccountActions(repository, AuthenticatedRequests(sessions, repository), sessions)
    @Test fun `success clears session but definite failure and unknown response retain it`() = runTest {
        ready()
        for (error in listOf(AuthFailure(AuthFailureKind.Server, 400, "LOGIN_FAILED"), AuthFailure(AuthFailureKind.Network))) {
            response = AuthResult.Failure(error)
            assertTrue(actions().changePassword("old", "new") is AuthResult.Failure)
            assertNotNull(sessions.state.value.credentials)
        }
        response = AuthResult.Success(Unit)
        assertTrue(actions().changePassword("old", "new") is AuthResult.Success)
        assertNull(sessions.state.value.credentials); assertNull(store.saved)
    }
    @Test fun `late deletion success cannot clear a new account and double taps are blocked`() = runTest {
        ready()
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun deleteAccount(accessToken: String, userId: Long): AuthResult<Unit> {
                calls++; gate.await(); return AuthResult.Success(Unit)
            }
        }
        val actions = actions(delayed)
        val pending = async { actions.deleteAccount() }; runCurrent()
        assertTrue(actions.deleteAccount() is AuthResult.Failure); assertEquals(1, calls)
        val attempt = sessions.beginLogin()
        sessions.acceptLogin(attempt, AuthLogin("b", 2, "2@example.com", "unused", 900, true, expiresAtMillis = 900_000), 0)
        gate.complete(Unit); assertTrue(pending.await() is AuthResult.Failure)
        assertEquals(2L, sessions.state.value.credentials!!.userId)
    }
}
