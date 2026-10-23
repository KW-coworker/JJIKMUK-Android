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
class LoginViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store)
    private var result: AuthResult<AuthLogin> = AuthResult.Success(
        AuthLogin("token", 1, "user@example.com", "unused", 3600, true))
    private var calls = 0
    private val repository = object : TestAuthRepository() {
        override suspend fun login(email: String, password: String): AuthResult<AuthLogin> {
            calls++
            assertEquals("old", password) // Existing login must not apply new password restrictions.
            return result
        }
    }
    private fun model(repo: AuthRepository = repository) = LoginViewModel(repo, sessions).apply {
        updateEmail("user@example.com"); updatePassword("old")
    }

    @Test fun `real response saves session before Home and clears password`() = runTest {
        val vm = model()
        assertTrue(vm.login())
        assertTrue(sessions.state.value.canEnterHome); assertNotNull(store.saved)
        assertEquals("", vm.uiState.value.password)
        sessions.clear()
    }
    @Test fun `failure keeps input and three failed logins never bypass authentication`() = runTest {
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "LOGIN_FAILED"))
        val vm = model()
        repeat(3) { assertFalse(vm.login()) }
        assertEquals(3, calls); assertNull(store.saved)
        assertEquals("old", vm.uiState.value.password); assertNotNull(vm.uiState.value.loginError)
        vm.updateEmail("new@example.com"); assertNull(vm.uiState.value.loginError)
    }
    @Test fun `profile incomplete and Google placeholder cannot enter Home`() = runTest {
        result = AuthResult.Success(AuthLogin("token", 1, "user@example.com", "unused", 3600, false))
        val vm = model()
        assertFalse(vm.login()); assertFalse(sessions.state.value.canEnterHome)
        assertNotNull(vm.uiState.value.loginError)
        vm.googleUnavailable(); assertFalse(sessions.state.value.canEnterHome)
        sessions.clear()
    }
    @Test fun `double tap is suppressed and editing input cancels stale success`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = object : TestAuthRepository() {
            override suspend fun login(email: String, password: String): AuthResult<AuthLogin> {
                calls++
                withContext(NonCancellable) { gate.await() }
                return result
            }
        }
        val vm = model(repo)
        val request = async { vm.login() }; runCurrent()
        assertFalse(vm.login()); assertEquals(1, calls)
        vm.updateEmail("other@example.com"); gate.complete(Unit)
        request.join()
        assertTrue(request.isCancelled); assertNull(store.saved)
        assertFalse(sessions.state.value.canEnterHome)
    }
    @Test fun `storage failure does not navigate to Home`() = runTest {
        store.failWrite = true
        val vm = model()
        assertFalse(vm.login()); assertFalse(sessions.state.value.canEnterHome)
        assertNotNull(vm.uiState.value.loginError)
    }
}
