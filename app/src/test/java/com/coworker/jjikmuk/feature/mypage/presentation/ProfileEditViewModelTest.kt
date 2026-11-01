package com.coworker.jjikmuk.feature.mypage.presentation

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = MemorySessionStore().apply {
        saved = SessionCredentials("saved", 1, "user@example.com", System.currentTimeMillis() + 900_000, true)
    }
    private val sessions = AuthSessionManager(store)
    private fun profile(name: String = "User") = AuthProfile(1, "user@example.com", name, "egg", null, "lowSugar",
        null, "USER", AuthProvider.LOCAL, true, true, null)
    private var patches = 0
    private var received: ProfilePatch? = null
    private var result: AuthResult<AuthProfile> = AuthResult.Success(profile("NewName"))
    private val repo = object : TestAuthRepository() {
        override suspend fun me(accessToken: String) = AuthResult.Success(profile())
        override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
            patches++; received = patch; return result
        }
        override suspend fun nicknameAvailable(nickname: String): AuthResult<Boolean> = error("Own nickname must not use public availability")
    }
    private suspend fun model(): ProfileEditViewModel {
        sessions.restore()
        return ProfileEditViewModel(AuthProfiles(repo, sessions, AuthenticatedRequests(sessions, repo)), sessions).apply { open() }
    }

    @Test fun `nickname only save keeps conditions and supports own case changes`() = runTest {
        val vm = model(); runCurrent()
        assertEquals("user@example.com", vm.state.value.email)
        vm.save(); runCurrent(); assertEquals(0, patches)
        vm.updateNickname("user"); vm.save(); vm.save(); runCurrent()
        assertEquals(1, patches)
        assertEquals("user", received!!.nickname)
        assertSame(ProfileField.Unchanged, received!!.allergies)
        assertSame(ProfileField.Unchanged, received!!.specialDiet)
        assertEquals("NewName", sessions.state.value.profile!!.nickname)
        assertEquals("egg", sessions.state.value.profile!!.allergies)
    }

    @Test fun `duplicate error keeps draft and can be corrected without losing email`() = runTest {
        val vm = model(); runCurrent()
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "NICKNAME_ALREADY_EXISTS"))
        vm.updateNickname("NewName"); vm.save(); runCurrent()
        assertEquals("NewName", vm.state.value.nickname)
        assertEquals("user@example.com", vm.state.value.email)
        assertFalse(vm.state.value.mustReload)
        assertNotNull(vm.state.value.message)
        vm.updateNickname("OtherName")
        assertNull(vm.state.value.message)
    }

    @Test fun `lost save response blocks repetition until explicit me reconciliation`() = runTest {
        val vm = model(); runCurrent()
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        vm.updateNickname("NewName"); vm.save(); runCurrent()
        assertTrue(vm.state.value.mustReload)
        vm.save(); runCurrent(); assertEquals(1, patches)
        vm.reload(); runCurrent()
        assertFalse(vm.state.value.mustReload)
        assertEquals("NewName", vm.state.value.nickname)
        result = AuthResult.Success(profile("NewName"))
        vm.save(); runCurrent(); assertEquals(2, patches)
    }

    @Test fun `invalid nickname never dispatches and reopening reloads rather than reusing draft`() = runTest {
        val vm = model(); runCurrent()
        vm.updateNickname("!"); vm.save(); runCurrent()
        assertEquals(0, patches); assertNotNull(vm.state.value.message)
        vm.close(); vm.open(); runCurrent()
        assertEquals("User", vm.state.value.nickname)
    }

    @Test fun `late save cannot affect the next account or reopened editor`() = runTest {
        sessions.restore()
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
                withContext(NonCancellable) { gate.await() }
                return AuthResult.Success(profile("Late"))
            }
        }
        val vm = ProfileEditViewModel(AuthProfiles(delayed, sessions, AuthenticatedRequests(sessions, delayed)), sessions)
        vm.open(); runCurrent(); vm.updateNickname("Late"); vm.save(); runCurrent()
        vm.close(); sessions.clear(); gate.complete(Unit); runCurrent()
        assertNull(sessions.state.value.profile)
        assertNotEquals("저장했어요", vm.state.value.message)
    }
}
