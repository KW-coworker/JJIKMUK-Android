package com.coworker.jjikmuk.feature.auth.presentation.profile

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.signup.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCompletionViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val sessions = AuthSessionManager(MemorySessionStore()) { 0L }
    private fun profile(id: Long = 1, completed: Boolean = false, allergies: String? = null, diet: String? = null) =
        AuthProfile(id, "$id@example.com", "사용자", allergies, "existing-disease", diet, "existing-dislike",
            "USER", AuthProvider.GOOGLE, completed, false, null)
    private var server = profile()
    private var completion: AuthResult<AuthProfile>? = null
    private var readFailure: AuthFailure? = null
    private var submitted: ProfileSubmission? = null
    private var calls = 0
    private var nicknameCalls = 0
    private var readCalls = 0
    private var gate: CompletableDeferred<Unit>? = null
    private var nicknameGate: CompletableDeferred<Unit>? = null
    private val repo = object : TestAuthRepository() {
        override suspend fun me(accessToken: String): AuthResult<AuthProfile> {
            readCalls++
            return readFailure?.let { AuthResult.Failure(it) } ?: AuthResult.Success(server)
        }
        override suspend fun nicknameAvailable(nickname: String): AuthResult<Boolean> {
            nicknameCalls++; nicknameGate?.await(); return AuthResult.Success(true)
        }
        override suspend fun completeProfile(accessToken: String, input: ProfileSubmission): AuthResult<AuthProfile> {
            calls++; submitted = input; gate?.await()
            return completion ?: AuthResult.Success(profile(completed = true, allergies = input.allergies, diet = input.specialDiet))
        }
    }
    private val profiles = AuthProfiles(repo, sessions, AuthenticatedRequests(sessions, repo))
    private fun model() = ProfileCompletionViewModel(repo, profiles, sessions)
    private suspend fun ready(profile: AuthProfile = server) {
        sessions.acceptLogin(sessions.beginLogin(), AuthLogin("token-${profile.id}", profile.id, profile.email, "unused", 900,
            profile.profileCompleted, expiresAtMillis = 900_000), 0)
        sessions.acceptProfile(sessions.state.value.generation, profile)
    }

    @Test fun `optional empty conditions complete without signup or OTP and preserve unedited fields`() = runTest {
        ready(); val vm = model(); vm.open(); vm.nextNickname(); runCurrent()
        assertEquals(0, nicknameCalls); assertEquals(ProfilePage.Conditions, vm.state.value.page)
        vm.nextConditions(); vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, calls); assertNull(submitted!!.allergies); assertNull(submitted!!.specialDiet)
        assertEquals("existing-disease", submitted!!.diseases); assertEquals("existing-dislike", submitted!!.dislikedIngredients)
        assertTrue(sessions.state.value.canEnterHome); assertTrue(sessions.state.value.profile!!.profileCompleted)
    }

    @Test fun `server conditions decode and user removals send canonical values`() = runTest {
        server = profile(allergies = "wheat,egg", diet = "lactoOvoVegetarian,highProtein")
        ready(); val vm = model(); vm.open()
        assertEquals(setOf("wheat", "egg"), vm.state.value.form.allergies)
        assertEquals(VegetarianDiet.LactoOvo, vm.state.value.form.vegetarianDiet)
        vm.condition(SignUpCondition.Allergy); vm.condition(SignUpCondition.Vegetarian)
        vm.condition(SignUpCondition.LowSugar); vm.review(); vm.submit(); runCurrent()
        assertNull(submitted!!.allergies); assertEquals("lowSugar,highProtein", submitted!!.specialDiet)
    }

    @Test fun `unknown catalogue and multiple vegetarian types block full replacement`() = runTest {
        for (profile in listOf(profile(allergies = "unknown"), profile(diet = "vegan,pescatarian"))) {
            ready(profile); val vm = model(); vm.open(); vm.review(); vm.submit(); runCurrent()
            assertFalse(vm.state.value.loaded); assertNotNull(vm.state.value.loadError)
        }
        assertEquals(0, calls)
    }

    @Test fun `late availability cannot approve edited nickname`() = runTest {
        ready(); val vm = model(); vm.open(); vm.nickname("새이름"); nicknameGate = CompletableDeferred()
        vm.nextNickname(); runCurrent(); vm.nickname("다른이름")
        nicknameGate!!.complete(Unit); runCurrent()
        assertFalse(vm.state.value.form.nicknameAvailable); assertEquals(ProfilePage.Nickname, vm.state.value.page)
        vm.nextNickname(); runCurrent(); assertTrue(vm.state.value.form.nicknameAvailable)
        vm.nickname("또다른이름"); assertFalse(vm.state.value.form.nicknameAvailable)
    }

    @Test fun `final nickname collision keeps selected conditions and session`() = runTest {
        ready(); val vm = model(); vm.open(); vm.condition(SignUpCondition.HighProtein); vm.review()
        completion = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "NICKNAME_ALREADY_EXISTS", "nickname"))
        vm.submit(); runCurrent()
        assertNotNull(vm.state.value.form.nicknameError); assertTrue(SignUpCondition.HighProtein in vm.state.value.form.selectedConditions)
        assertNotNull(sessions.state.value.credentials); assertFalse(sessions.state.value.canEnterHome)
        vm.nickname("수정이름"); assertNull(vm.state.value.form.nicknameError)
    }

    @Test fun `lost response locks mutation until safe read confirms completion`() = runTest {
        ready(); val vm = model(); vm.open(); vm.review()
        completion = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        vm.submit(); runCurrent(); vm.submit(); vm.nickname("수정금지"); runCurrent()
        assertEquals(1, calls); assertEquals("사용자", vm.state.value.form.nickname)
        assertTrue(vm.state.value.needsReadback); assertNotNull(sessions.state.value.credentials)
        readFailure = AuthFailure(AuthFailureKind.Network); vm.reload(); runCurrent()
        assertTrue(vm.state.value.needsReadback); assertEquals(1, calls)
        readFailure = null; server = profile(completed = true); vm.reload(); runCurrent()
        assertTrue(sessions.state.value.canEnterHome); assertEquals(1, calls); assertEquals(2, readCalls)
    }

    @Test fun `readback incomplete retains draft and permits only explicit resubmission`() = runTest {
        ready(); val vm = model(); vm.open(); vm.nickname("입력유지"); vm.condition(SignUpCondition.LowSugar); vm.review()
        completion = AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse))
        vm.submit(); runCurrent(); vm.reload(); runCurrent()
        assertEquals("입력유지", vm.state.value.form.nickname); assertFalse(vm.state.value.needsReadback)
        assertTrue(SignUpCondition.LowSugar in vm.state.value.form.selectedConditions); assertEquals(1, calls)
        completion = null; vm.submit(); runCurrent(); assertEquals(2, calls)
    }

    @Test fun `new server unknown condition during recovery cannot be silently overwritten`() = runTest {
        ready(); val vm = model(); vm.open(); vm.review()
        completion = AuthResult.Failure(AuthFailure(AuthFailureKind.Network)); vm.submit(); runCurrent()
        server = profile(diet = "futureCondition"); vm.reload(); runCurrent(); vm.submit(); runCurrent()
        assertTrue(vm.state.value.needsReadback); assertEquals(1, calls)
    }

    @Test fun `late completion and stale form never affect another account`() = runTest {
        ready(); val vm = model(); vm.open(); vm.review(); gate = CompletableDeferred()
        vm.submit(); runCurrent(); ready(profile(id = 2)); runCurrent()
        gate!!.complete(Unit); runCurrent()
        assertEquals(2L, sessions.state.value.credentials!!.userId); assertFalse(sessions.state.value.canEnterHome)
        assertFalse(vm.state.value.loaded); assertEquals("", vm.state.value.form.nickname)
        vm.open(); assertEquals("사용자", vm.state.value.form.nickname)
    }

    @Test fun `recreated model resumes from server and same running model retains draft`() = runTest {
        ready(); val vm = model(); vm.open(); vm.nickname("미저장입력"); vm.open()
        assertEquals("미저장입력", vm.state.value.form.nickname)
        val recreated = model(); recreated.open(); assertEquals("사용자", recreated.state.value.form.nickname)
        assertFalse(sessions.state.value.canEnterHome)
    }
}
