package com.coworker.jjikmuk.feature.mypage.presentation

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
class SelfDietConditionViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val sessions = AuthSessionManager(MemorySessionStore()) { 0L }
    private fun profile(id: Long = 1, allergies: String? = "egg", diet: String? = "highProtein") =
        AuthProfile(id, "$id@example.com", "사용자", allergies, "disease", diet, "dislike", "USER", AuthProvider.LOCAL, true, true, null)
    private var server = profile()
    private var failure: AuthFailure? = null
    private var readFailure: AuthFailure? = null
    private var gate: CompletableDeferred<Unit>? = null
    private var calls = 0
    private var reads = 0
    private var lastPatch: ProfilePatch? = null
    private fun ProfileField.apply(previous: String?) = when (this) {
        ProfileField.Unchanged -> previous
        ProfileField.Clear -> null
        is ProfileField.Value -> value
    }
    private val repo = object : TestAuthRepository() {
        override suspend fun me(accessToken: String): AuthResult<AuthProfile> {
            reads++; return readFailure?.let { AuthResult.Failure(it) } ?: AuthResult.Success(server)
        }
        override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
            calls++; lastPatch = patch; gate?.await()
            failure?.let { return AuthResult.Failure(it) }
            server = profile(allergies = patch.allergies.apply(server.allergies), diet = patch.specialDiet.apply(server.specialDiet))
            return AuthResult.Success(server)
        }
    }
    private val profiles = AuthProfiles(repo, sessions, AuthenticatedRequests(sessions, repo))
    private fun model() = SelfDietConditionViewModel(profiles, sessions)
    private suspend fun ready(value: AuthProfile = server) {
        sessions.acceptLogin(sessions.beginLogin(), AuthLogin("token-${value.id}", value.id, value.email, "unused", 900, true,
            expiresAtMillis = 900_000), 0)
        sessions.acceptProfile(sessions.state.value.generation, value)
    }

    @Test fun `only edited allergy field is sent and unrelated fields stay omitted`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent()
        assertEquals(setOf("egg"), vm.state.value.draft.allergies)
        vm.allergy("wheat"); vm.save(); runCurrent()
        assertEquals("egg,wheat", (lastPatch!!.allergies as ProfileField.Value).value)
        assertSame(ProfileField.Unchanged, lastPatch!!.specialDiet)
        assertSame(ProfileField.Unchanged, lastPatch!!.diseases)
        assertSame(ProfileField.Unchanged, lastPatch!!.dislikedIngredients); assertNull(lastPatch!!.nickname)
        assertFalse(vm.state.value.hasChanges); assertEquals("disease", sessions.state.value.profile!!.diseases)
    }

    @Test fun `clearing every condition sends explicit null fields`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent()
        vm.allergy("egg"); vm.nutrition(SignUpCondition.HighProtein); vm.save(); runCurrent()
        assertSame(ProfileField.Clear, lastPatch!!.allergies); assertSame(ProfileField.Clear, lastPatch!!.specialDiet)
        assertNull(sessions.state.value.profile!!.allergies); assertNull(sessions.state.value.profile!!.specialDiet)
    }

    @Test fun `vegetarian and nutrition share canonical specialDiet without touching allergy`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent()
        vm.vegetarian(VegetarianDiet.LactoOvo); vm.nutrition(SignUpCondition.LowSugar); vm.save(); runCurrent()
        assertEquals("lactoOvoVegetarian,lowSugar,highProtein", (lastPatch!!.specialDiet as ProfileField.Value).value)
        assertSame(ProfileField.Unchanged, lastPatch!!.allergies)
    }

    @Test fun `unknown server codes and multiple vegetarian codes never become empty conditions`() = runTest {
        for (value in listOf(profile(allergies = "new-code"), profile(diet = "vegan,pescatarian"))) {
            server = value; ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("egg"); vm.save(); runCurrent()
            assertTrue(vm.state.value.unsupported); assertFalse(vm.state.value.canSave)
        }
        assertEquals(0, calls)
    }

    @Test fun `lost response requires GET before any further edit or patch`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat")
        failure = AuthFailure(AuthFailureKind.Network); vm.save(); runCurrent()
        assertTrue(vm.state.value.needsReadback)
        vm.allergy("milk"); vm.save(); runCurrent(); assertEquals(1, calls)
        server = profile(allergies = "wheat,egg") // Server committed despite lost response.
        vm.reload(); runCurrent()
        assertFalse(vm.state.value.needsReadback); assertFalse(vm.state.value.hasChanges)
        vm.save(); runCurrent(); assertEquals(1, calls); assertEquals(2, reads)
    }

    @Test fun `readback preserves edited allergy while adopting new untouched diet`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat")
        failure = AuthFailure(AuthFailureKind.Network); vm.save(); runCurrent()
        readFailure = AuthFailure(AuthFailureKind.Network); vm.reload(); runCurrent()
        assertTrue(vm.state.value.needsReadback); assertEquals(setOf("egg", "wheat"), vm.state.value.draft.allergies)
        readFailure = null; server = profile(diet = "lowSugar"); vm.reload(); runCurrent()
        assertEquals(setOf(SignUpCondition.LowSugar), vm.state.value.draft.nutrition)
        assertTrue(vm.state.value.canSave); failure = null; vm.save(); runCurrent()
        assertSame(ProfileField.Unchanged, lastPatch!!.specialDiet); assertEquals("lowSugar", server.specialDiet)
    }

    @Test fun `definite validation failure keeps draft and allows corrected submission`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat")
        failure = AuthFailure(AuthFailureKind.Server, 400, "VALIDATION_FAILED", "allergies")
        vm.save(); runCurrent(); assertFalse(vm.state.value.needsReadback); assertTrue(vm.state.value.canSave)
        vm.allergy("wheat"); assertFalse(vm.state.value.hasChanges); assertNotNull(sessions.state.value.credentials)
    }

    @Test fun `double submission edits and screen recreation cannot duplicate in flight patch`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat"); gate = CompletableDeferred()
        vm.save(); vm.save(); runCurrent(); vm.open(); vm.allergy("milk")
        assertEquals(1, calls); assertEquals(setOf("egg", "wheat"), vm.state.value.draft.allergies)
        gate!!.complete(Unit); runCurrent(); assertFalse(vm.state.value.busy)
    }

    @Test fun `account switch clears draft and late result cannot update newer account`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat"); gate = CompletableDeferred()
        vm.save(); runCurrent(); ready(profile(id = 2, allergies = "milk")); runCurrent()
        assertNull(vm.state.value.saved); assertEquals(emptySet<String>(), vm.state.value.draft.allergies)
        gate!!.complete(Unit); runCurrent()
        assertEquals(2L, sessions.state.value.profile!!.id); assertEquals("milk", sessions.state.value.profile!!.allergies)
    }

    @Test fun `captured owner cannot patch new account before coroutine begins`() = runTest {
        ready(); val generation = sessions.state.value.generation
        ready(profile(id = 2))
        val result = profiles.patch(ProfilePatch(allergies = ProfileField.Clear), generation)
        assertTrue(result is AuthResult.Failure); assertEquals(0, calls)
    }

    @Test fun `unsigned developer entry does not request or mutate server data`() = runTest {
        val vm = model(); vm.open(); vm.allergy("wheat"); vm.save(); vm.reload(); runCurrent()
        assertEquals(0, reads); assertEquals(0, calls); assertFalse(vm.state.value.canSave)
    }

    @Test fun `reopening after explicit leave reads latest server instead of old draft`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.allergy("wheat"); vm.open()
        assertEquals(setOf("egg", "wheat"), vm.state.value.draft.allergies)
        vm.leave(); server = profile(allergies = "milk"); vm.open(); runCurrent()
        assertEquals(setOf("milk"), vm.state.value.draft.allergies); assertEquals(2, reads)
    }
}
