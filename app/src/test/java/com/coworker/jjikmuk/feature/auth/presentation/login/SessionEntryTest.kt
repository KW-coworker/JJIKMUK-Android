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
class SessionEntryTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = MemorySessionStore().apply {
        saved = SessionCredentials("saved", 1, "user@example.com", System.currentTimeMillis() + 900_000, true)
    }
    private val sessions = AuthSessionManager(store)
    private fun profile(complete: Boolean = true) = AuthProfile(1, "user@example.com", "사용자", null, null, null,
        null, "USER", AuthProvider.LOCAL, complete, true, null)

    @Test fun `startup offline keeps session and retry proceeds only after me succeeds`() = runTest {
        var result: AuthResult<AuthProfile> = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        var calls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun me(accessToken: String): AuthResult<AuthProfile> { calls++; return result }
        }
        val vm = LoginViewModel(repo, sessions)
        vm.checkSession(); runCurrent()
        assertTrue(vm.entryState.value is SessionEntryState.Retry)
        assertNotNull(store.saved); assertFalse(sessions.state.value.canEnterHome)
        result = AuthResult.Success(profile())
        vm.checkSession(); runCurrent()
        assertEquals(SessionEntryState.Ready, vm.entryState.value)
        assertEquals(2, calls)
        sessions.clear()
    }

    @Test fun `incomplete profile is routed separately and invalid access becomes signed out`() = runTest {
        var result: AuthResult<AuthProfile> = AuthResult.Success(profile(false))
        val vm = LoginViewModel(object : TestAuthRepository() {
            override suspend fun me(accessToken: String) = result
        }, sessions)
        vm.checkSession(); runCurrent()
        assertEquals(SessionEntryState.ProfileRequired, vm.entryState.value)
        assertFalse(sessions.state.value.canEnterHome)
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_INVALID"))
        vm.checkSession(); runCurrent()
        assertEquals(SessionEntryState.SignedOut, vm.entryState.value)
        assertNull(store.saved)
    }

    @Test fun `duplicate retry and late startup after logout never open home`() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val vm = LoginViewModel(object : TestAuthRepository() {
            override suspend fun me(accessToken: String): AuthResult<AuthProfile> {
                calls++; gate.await(); return AuthResult.Success(profile())
            }
        }, sessions)
        vm.checkSession(); runCurrent(); vm.checkSession(); runCurrent()
        assertEquals(1, calls)
        sessions.clear()
        gate.complete(Unit); runCurrent()
        assertEquals(SessionEntryState.SignedOut, vm.entryState.value)
        assertFalse(sessions.state.value.canEnterHome)
    }
}
