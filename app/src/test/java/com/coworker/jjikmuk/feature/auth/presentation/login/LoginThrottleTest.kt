package com.coworker.jjikmuk.feature.auth.presentation.login

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginThrottleTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun `server cooldown survives input edits screens and restoration without automatic login`() = runTest {
        var saved = 0L
        val gate = LoginRetryGate({ saved }, { saved = it }, { testScheduler.currentTime })
        var calls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun login(email: String, password: String): AuthResult<AuthLogin> {
                calls++
                return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 429, "LOGIN_RATE_LIMITED",
                    retryAfterSeconds = 3, retryAfterHeaderSeconds = 9))
            }
        }
        val sessions = AuthSessionManager(MemorySessionStore())
        val vm = LoginViewModel(repo, sessions, retryGate = gate)
        vm.updateEmail("user@example.com"); vm.updatePassword("old")
        assertFalse(vm.login()); assertEquals(3, vm.uiState.value.retrySeconds)
        vm.reset(); vm.updateEmail("other@example.com"); vm.updatePassword("old")
        assertFalse(vm.login()); assertEquals(1, calls)
        val restored = LoginRetryGate({ saved }, {}, { testScheduler.currentTime })
        assertEquals(3, restored.remainingSeconds())
        advanceTimeBy(3_000); runCurrent()
        assertEquals(0, vm.uiState.value.retrySeconds); assertEquals(0, restored.remainingSeconds())
        assertEquals(1, calls) // Expiry only enables a manual retry.
        assertFalse(vm.login()); assertEquals(2, calls)
        advanceTimeBy(3_000); runCurrent()
    }
    @Test fun `rate limit does not delete an existing session or attempt refresh`() = runTest {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store)
        sessions.acceptLogin(sessions.beginLogin(), AuthLogin("existing", 1, "user@example.com", "unused", 3600, true), System.currentTimeMillis())
        val vm = LoginViewModel(object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> = error("Must not refresh for 429")
            override suspend fun login(email: String, password: String) = AuthResult.Failure(
                AuthFailure(AuthFailureKind.Server, 429, "LOGIN_RATE_LIMITED", retryAfterSeconds = 1))
        }, sessions, retryGate = LoginRetryGate(nowMillis = { testScheduler.currentTime }))
        vm.updateEmail("user@example.com"); vm.updatePassword("old")
        assertFalse(vm.login()); assertEquals("existing", store.saved!!.accessToken)
        sessions.clear(); advanceTimeBy(1_000); runCurrent()
    }
}
