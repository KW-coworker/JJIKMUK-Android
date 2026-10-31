package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.SavedStateHandle
import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountActionViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { 0L }
    private var calls = 0
    private var response: AuthResult<Unit> = AuthResult.Success(Unit)
    private var gate: CompletableDeferred<Unit>? = null
    private val repository = object : TestAuthRepository() {
        override suspend fun changePassword(accessToken: String, userId: Long, currentPassword: String, newPassword: String): AuthResult<Unit> {
            assertEquals("old", currentPassword) // No new-password rule applied to the old credential.
            assertEquals("Password123", newPassword)
            calls++; gate?.await(); return response
        }
        override suspend fun deleteAccount(accessToken: String, userId: Long): AuthResult<Unit> {
            calls++; gate?.await(); return response
        }
    }
    private suspend fun ready(hasPassword: Boolean = true, id: Long = 1) {
        val attempt = sessions.beginLogin()
        sessions.acceptLogin(attempt, AuthLogin("token-$id", id, "$id@example.com", "unused", 900, true, expiresAtMillis = 900_000), 0)
        sessions.acceptProfile(sessions.state.value.generation, AuthProfile(id, "$id@example.com", "사용자", null, null,
            null, null, "USER", if (hasPassword) AuthProvider.LOCAL else AuthProvider.GOOGLE, true, hasPassword, null))
    }
    private fun model(saved: SavedStateHandle = SavedStateHandle()) = AccountActionViewModel(
        AuthAccountActions(repository, AuthenticatedRequests(sessions, repository), sessions), sessions, saved)
    private fun AccountActionViewModel.password() {
        open(AccountAction.Password); editCurrent("old"); editNew("Password123"); editConfirmation("Password123")
    }

    @Test fun `password success clears credentials and secrets and rejects double tap`() = runTest {
        ready(); val vm = model(); vm.password(); gate = CompletableDeferred()
        vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, calls); assertTrue(vm.state.value.busy)
        vm.editNew("Ignored123"); assertEquals("Password123", vm.state.value.newPassword)
        gate!!.complete(Unit); runCurrent()
        assertTrue(vm.state.value.complete); assertNull(sessions.state.value.credentials)
        assertNull(store.saved); assertEquals("", vm.state.value.currentPassword); assertEquals("", vm.state.value.newPassword)
    }

    @Test fun `withdrawal requires consent and only confirmed success completes`() = runTest {
        ready(); val vm = model(); vm.open(AccountAction.Withdrawal)
        vm.submit(); runCurrent(); assertEquals(0, calls)
        vm.consent(true); response = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED"))
        vm.submit(); runCurrent()
        assertFalse(vm.state.value.complete); assertNotNull(sessions.state.value.credentials); assertTrue(vm.state.value.consented)
        response = AuthResult.Success(Unit); vm.submit(); runCurrent()
        assertTrue(vm.state.value.complete); assertEquals(2, calls); assertNull(sessions.state.value.credentials)
    }

    @Test fun `definite password error preserves inputs and field mapping`() = runTest {
        ready(); val vm = model(); vm.password()
        for ((code, field) in listOf("LOGIN_FAILED" to "currentPassword", "PASSWORD_INVALID" to "password")) {
            response = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, code, field))
            vm.submit(); runCurrent()
            assertEquals(field, vm.state.value.errorField); assertEquals("Password123", vm.state.value.newPassword)
            assertFalse(vm.state.value.unknown); assertTrue(vm.state.value.canSubmit); assertNotNull(store.saved)
        }
    }

    @Test fun `unknown response is not replayed after back navigation or state restoration`() = runTest {
        ready(); val saved = SavedStateHandle(); val vm = model(saved); vm.password()
        response = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        vm.submit(); runCurrent(); assertNotNull(sessions.state.value.credentials)
        assertTrue(vm.state.value.unknown); vm.leave(); vm.open(AccountAction.Password)
        vm.submit(); runCurrent(); assertEquals(1, calls)
        val restored = model(saved); restored.open(AccountAction.Withdrawal)
        restored.consent(true); restored.submit(); runCurrent()
        assertTrue(restored.state.value.unknown); assertEquals(1, calls)
        assertEquals("", restored.state.value.newPassword)
        assertFalse(saved.keys().any { it.contains("password", ignoreCase = true) })
        restored.reauthenticate(); assertNull(sessions.state.value.credentials)
    }

    @Test fun `pending request restoration never sends another delete`() = runTest {
        ready(); val saved = SavedStateHandle(); val vm = model(saved); vm.open(AccountAction.Withdrawal)
        gate = CompletableDeferred(); vm.consent(true); vm.submit(); runCurrent()
        val restored = model(saved); restored.open(AccountAction.Withdrawal)
        restored.submit(); runCurrent(); assertTrue(restored.state.value.unknown); assertEquals(1, calls)
        gate!!.complete(Unit); runCurrent()
    }

    @Test fun `Google only account and developer entry cannot change password`() = runTest {
        ready(hasPassword = false); val vm = model(); vm.password(); vm.submit(); runCurrent()
        assertFalse(vm.state.value.hasPassword); assertEquals(0, calls)
        sessions.clear(); vm.open(AccountAction.Withdrawal); vm.consent(true); vm.submit(); runCurrent()
        assertFalse(vm.state.value.available); assertEquals(0, calls)
    }

    @Test fun `late old account success neither signs out new account nor shows completion`() = runTest {
        ready(); val vm = model(); vm.open(AccountAction.Withdrawal)
        gate = CompletableDeferred(); vm.consent(true); vm.submit(); runCurrent()
        ready(id = 2); gate!!.complete(Unit); runCurrent()
        assertFalse(vm.state.value.complete); assertEquals(2L, sessions.state.value.credentials!!.userId)
        assertFalse(vm.state.value.available)
    }

    @Test fun `validation requires matching new password but never restricts current password to new rules`() = runTest {
        ready(); val vm = model(); vm.open(AccountAction.Password)
        vm.editCurrent("old"); vm.editNew("short1"); vm.editConfirmation("short1")
        vm.submit(); runCurrent(); assertEquals(0, calls)
        vm.editNew("Password123"); vm.editConfirmation("Mismatch123")
        assertFalse(vm.state.value.canSubmit)
        vm.editConfirmation("Password123"); assertTrue(vm.state.value.canSubmit)
        assertFalse(vm.state.value.toString().contains("Password123"))
    }
    @Test fun `server success with local storage cleanup failure still clears memory and shows warning`() = runTest {
        val failingStore = object : SessionStore by store {
            override suspend fun clear() { throw java.io.IOException("unavailable") }
        }
        val manager = AuthSessionManager(failingStore) { 0L }
        manager.acceptLogin(manager.beginLogin(), AuthLogin("token", 1, "1@example.com", "unused", 900, true, expiresAtMillis = 900_000), 0)
        manager.acceptProfile(manager.state.value.generation,
            AuthProfile(1, "1@example.com", "사용자", null, null, null, null, "USER", AuthProvider.LOCAL, true, true, null))
        val vm = AccountActionViewModel(AuthAccountActions(repository, AuthenticatedRequests(manager, repository), manager), manager, SavedStateHandle())
        vm.open(AccountAction.Withdrawal); vm.consent(true); vm.submit(); runCurrent()
        assertTrue(vm.state.value.complete); assertTrue(vm.state.value.storageWarning)
        assertNull(manager.state.value.credentials); assertFalse(manager.state.value.canEnterHome)
    }

}
