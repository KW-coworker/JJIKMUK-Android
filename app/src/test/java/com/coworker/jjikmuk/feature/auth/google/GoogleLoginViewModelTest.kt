package com.coworker.jjikmuk.feature.auth.google

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
class GoogleLoginViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private var wall = 1_000L
    private var elapsed = 200L
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { wall }
    private fun login(id: Long = 1, completed: Boolean = true) = AuthLogin("service-$id", id, "$id@example.com", "unused", 900,
        completed, RefreshCredentials("refresh-$id", wall + 2_592_000_000L), AuthProvider.LOCAL_AND_GOOGLE, true, false, wall + 900_000)
    private var googleResult: AuthResult<GoogleAuthResult> = AuthResult.Success(GoogleAuthResult.LinkRequired("link-proof", wall + 300_000, 300))
    private var linkResult: AuthResult<AuthLogin> = AuthResult.Success(login())
    private var cancelResult: AuthResult<Unit> = AuthResult.Success(Unit)
    private var googleGate: CompletableDeferred<Unit>? = null
    private var linkGate: CompletableDeferred<Unit>? = null
    private var googleCalls = 0
    private var linkCalls = 0
    private var cancelCalls = 0
    private val repo = object : TestAuthRepository() {
        override suspend fun google(idToken: String): AuthResult<GoogleAuthResult> {
            assertEquals("google-id-token", idToken); googleCalls++; googleGate?.await(); return googleResult
        }
        override suspend fun linkGoogle(linkToken: String, currentPassword: String): AuthResult<AuthLogin> {
            assertEquals("link-proof", linkToken); assertEquals("old", currentPassword)
            linkCalls++; linkGate?.await(); return linkResult
        }
        override suspend fun cancelGoogleLink(linkToken: String): AuthResult<Unit> {
            assertEquals("link-proof", linkToken); cancelCalls++; return cancelResult
        }
    }
    private fun model() = GoogleLoginViewModel(repo, sessions, { wall }, { elapsed })
    private fun GoogleLoginViewModel.choose() {
        val ticket = requireNotNull(beginPicking())
        receiveCredential(ticket, GoogleCredentialResult.Token("google-id-token"))
    }

    @Test fun `link required never creates service session and old password can explicitly link`() = runTest {
        val vm = model(); vm.choose(); runCurrent()
        assertEquals(GooglePhase.LinkRequired, vm.state.value.phase)
        assertNull(sessions.state.value.credentials); assertNull(store.saved); assertEquals(0, linkCalls)
        vm.password("old"); vm.link(); runCurrent()
        assertEquals(GooglePhase.Finished, vm.state.value.phase); assertTrue(vm.state.value.linked)
        assertEquals("service-1", store.saved!!.accessToken); assertEquals("", vm.state.value.password)
        assertNull(sessions.state.value.profile) // Entry gate still performs me.
    }

    @Test fun `new Google user stores incomplete server session for onboarding`() = runTest {
        googleResult = AuthResult.Success(GoogleAuthResult.SignedIn(login(completed = false)))
        val vm = model(); vm.choose(); runCurrent()
        assertEquals(GooglePhase.Finished, vm.state.value.phase); assertFalse(vm.state.value.linked)
        assertFalse(sessions.state.value.canEnterHome); assertNotNull(store.saved); assertEquals(0, linkCalls)
    }

    @Test fun `wrong password uses server remaining attempts and preserves proof until exhausted`() = runTest {
        val vm = model(); vm.choose(); runCurrent(); vm.password("old")
        linkResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "GOOGLE_LINK_PASSWORD_INVALID", remainingAttempts = 4))
        vm.link(); runCurrent()
        assertEquals(GooglePhase.LinkRequired, vm.state.value.phase); assertEquals(4, vm.state.value.remainingAttempts)
        assertNull(store.saved); assertEquals("old", vm.state.value.password)
        linkResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "GOOGLE_LINK_PASSWORD_INVALID", remainingAttempts = 0))
        vm.link(); runCurrent(); vm.link(); runCurrent()
        assertEquals(GooglePhase.Idle, vm.state.value.phase); assertEquals("", vm.state.value.password)
        assertEquals(2, linkCalls); assertEquals(1, cancelCalls)
    }

    @Test fun `expiry uses shorter absolute expiry and monotonic countdown despite wall clock change`() = runTest {
        googleResult = AuthResult.Success(GoogleAuthResult.LinkRequired("link-proof", wall + 2_000, 300))
        val vm = model(); vm.choose(); runCurrent(); assertEquals(2, vm.state.value.remainingSeconds)
        wall -= 100_000; elapsed += 2_001; vm.tick(); vm.password("old"); vm.link(); runCurrent()
        assertEquals(GooglePhase.Idle, vm.state.value.phase); assertEquals(0, linkCalls); assertEquals(1, cancelCalls)
    }

    @Test fun `lost link response discards proof and never retries same mutation`() = runTest {
        val vm = model(); vm.choose(); runCurrent(); vm.password("old")
        linkResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network)); vm.link(); runCurrent()
        vm.link(); runCurrent()
        assertEquals(1, linkCalls); assertNull(store.saved); assertEquals(GooglePhase.Idle, vm.state.value.phase)
        assertEquals("", vm.state.value.password)
        googleResult = AuthResult.Success(GoogleAuthResult.SignedIn(login()))
        vm.choose(); runCurrent(); assertEquals(GooglePhase.Finished, vm.state.value.phase); assertEquals(2, googleCalls)
    }

    @Test fun `cancel and password recovery revoke proof even when cancellation is unconfirmed`() = runTest {
        val vm = model(); vm.choose(); runCurrent(); vm.password("old")
        cancelResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        vm.exit(GoogleExit.PasswordReset); vm.exit(); runCurrent()
        assertEquals(1, cancelCalls); assertEquals(GooglePhase.Exit, vm.state.value.phase)
        assertEquals(GoogleExit.PasswordReset, vm.state.value.exit); assertNotNull(vm.state.value.message)
        assertNull(store.saved); assertEquals("", vm.state.value.password)
    }

    @Test fun `late sign in cannot replace another account after user leaves`() = runTest {
        googleGate = CompletableDeferred(); googleResult = AuthResult.Success(GoogleAuthResult.SignedIn(login()))
        val vm = model(); vm.choose(); runCurrent(); vm.exit(); runCurrent()
        sessions.acceptLogin(sessions.beginLogin(), login(id = 2), wall)
        googleGate!!.complete(Unit); runCurrent()
        assertEquals(2L, store.saved!!.userId); assertEquals(GooglePhase.Exit, vm.state.value.phase)
    }

    @Test fun `late link grant after cancellation is discarded on server`() = runTest {
        googleGate = CompletableDeferred(); val vm = model(); vm.choose(); runCurrent(); vm.exit(); runCurrent()
        googleGate!!.complete(Unit); runCurrent()
        assertEquals(1, cancelCalls); assertNull(store.saved); assertEquals(GooglePhase.Exit, vm.state.value.phase)
    }

    @Test fun `double taps do not duplicate picker google or link requests`() = runTest {
        val vm = model(); val ticket = vm.beginPicking()!!; assertNull(vm.beginPicking())
        vm.receiveCredential(ticket, GoogleCredentialResult.Token("google-id-token"))
        vm.receiveCredential(ticket, GoogleCredentialResult.Token("google-id-token")); runCurrent()
        linkGate = CompletableDeferred(); vm.password("old"); vm.link(); vm.link(); runCurrent()
        assertEquals(1, googleCalls); assertEquals(1, linkCalls)
        vm.password("changed"); assertEquals("old", vm.state.value.password)
        linkGate!!.complete(Unit); runCurrent()
    }

    @Test fun `missing configuration and platform cancellation never contact backend`() = runTest {
        val vm = model(); val first = vm.beginPicking()!!
        vm.receiveCredential(first, GoogleCredentialResult.Failure("설정 필요")); runCurrent()
        assertEquals("설정 필요", vm.state.value.message)
        val second = vm.beginPicking()!!; vm.receiveCredential(second, GoogleCredentialResult.Cancelled); runCurrent()
        assertEquals(0, googleCalls); assertNull(store.saved)
    }

    @Test fun `picker finishing after new login cannot start or clear service session`() = runTest {
        val vm = model(); val ticket = vm.beginPicking()!!
        sessions.acceptLogin(sessions.beginLogin(), login(id = 2), wall)
        vm.receiveCredential(ticket, GoogleCredentialResult.Token("google-id-token")); runCurrent()
        assertEquals(0, googleCalls); assertEquals(2L, store.saved!!.userId); assertEquals(GooglePhase.Idle, vm.state.value.phase)
    }

    @Test fun `storage failure never grants UI login and proof password are cleared`() = runTest {
        val vm = model(); vm.choose(); runCurrent(); vm.password("old"); store.failWrite = true
        vm.link(); runCurrent()
        assertEquals(GooglePhase.Idle, vm.state.value.phase); assertNull(sessions.state.value.credentials)
        assertEquals("", vm.state.value.password); assertFalse(vm.state.value.toString().contains("old"))
    }
}
