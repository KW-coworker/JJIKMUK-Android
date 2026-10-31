package com.coworker.jjikmuk.feature.auth.presentation.signup

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.ui.catalog.FoodAllergy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpSubmissionTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private var calls = 0
    private var input: SignUpSubmission? = null
    private var response: AuthResult<Long> = AuthResult.Success(42L)
    private val repo = object : TestAuthRepository() {
        override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> {
            calls++; this@SignUpSubmissionTest.input = input; return response
        }
    }
    private suspend fun ready(repository: AuthRepository = repo): SignUpViewModel = SignUpViewModel(repository).apply {
        updateEmail(" USER@Example.com "); check(validateEmail()); updateOtp("0427"); check(verifyOtp())
        updatePassword("Password123!"); updatePasswordConfirm("Password123!"); updateNickname("사용자")
    }

    @Test fun `no-condition signup uses verified email and proof and clears secrets only on success`() = runTest {
        val vm = ready()
        assertTrue(vm.submit())
        assertEquals("user@example.com", input!!.email)
        assertEquals("test-proof", input!!.verificationToken)
        assertNull(input!!.allergies); assertNull(input!!.specialDiet)
        assertTrue(vm.uiState.value.registrationCompleted)
        assertEquals("", vm.uiState.value.password)
        assertNull(vm.currentVerificationProof())
        assertFalse(vm.submit()); assertEquals(1, calls)
        vm.reset()
    }

    @Test fun `documented keys produce deterministic CSV without altering shared catalog IDs`() = runTest {
        val vm = ready()
        vm.toggleCondition(SignUpCondition.Allergy); vm.updateAllergies(setOf("wheat", "egg", "soy"))
        vm.toggleCondition(SignUpCondition.Vegetarian); vm.selectVegetarianDiet(VegetarianDiet.LactoOvo)
        vm.toggleCondition(SignUpCondition.HighProtein); vm.toggleCondition(SignUpCondition.LowSugar)
        assertTrue(vm.submit())
        assertEquals("egg,soy,wheat", input!!.allergies)
        assertEquals("lactoOvoVegetarian,lowSugar,highProtein", input!!.specialDiet)
        vm.reset()
    }

    @Test fun `unknown selection is not dropped or guessed`() = runTest {
        val vm = ready()
        vm.toggleCondition(SignUpCondition.Allergy); vm.updateAllergies(setOf("egg", "unknown_allergen"))
        assertFalse(vm.submit()); assertEquals(0, calls)
        assertEquals(setOf("egg", "unknown_allergen"), vm.uiState.value.allergies)
        assertNotNull(vm.currentVerificationProof())
        vm.toggleCondition(SignUpCondition.Allergy)
        vm.toggleCondition(SignUpCondition.Vegetarian)
        assertFalse(vm.submit()); assertEquals(0, calls)
        vm.reset()
    }

    @Test fun `every displayed allergy is sent with the documented server ID`() = runTest {
        val expected = "egg,milk,soy,wheat,pork,chicken,shrimp,crab,squid,mackerel,shellfish,oyster,mussel,abalone,peach,tomato,peanut,walnut,buckwheat,pine_nut,sulfites,sesame,almond,mustard,celery,beef"
        assertEquals(expected.split(",").toSet(), FoodAllergy.entries.map { it.id }.toSet())
        val vm = ready()
        vm.toggleCondition(SignUpCondition.Allergy)
        vm.updateAllergies(FoodAllergy.entries.reversed().map { it.id }.toSet())
        assertTrue(vm.submit())
        assertEquals(expected, input!!.allergies)
        vm.reset()
    }

    @Test fun `all six vegetarian types and every nutrition subset encode without UI group IDs`() {
        val diets = listOf("vegan", "lactoVegetarian", "ovoVegetarian", "lactoOvoVegetarian", "pescatarian", "pollotarian")
        val nutrients = listOf(
            SignUpCondition.LowSugar to "lowSugar", SignUpCondition.LowSodium to "lowSodium",
            SignUpCondition.GlutenFree to "glutenFree", SignUpCondition.LowCalorie to "lowCalorie",
            SignUpCondition.LowFat to "lowFat", SignUpCondition.HighProtein to "highProtein",
        )
        for ((index, diet) in VegetarianDiet.entries.withIndex()) {
            for (mask in 0 until 64) {
                val selected = nutrients.filterIndexed { bit, _ -> mask and (1 shl bit) != 0 }
                val result = SignUpConditionMapping.encode(SignUpUiState(
                    selectedConditions = selected.map { it.first }.toSet() + SignUpCondition.Vegetarian,
                    vegetarianDiet = diet,
                ))
                assertEquals((listOf(diets[index]) + selected.map { it.second }).joinToString(","), result?.specialDiet)
                assertNull(result?.allergies)
            }
        }
        // Turning groups off must not submit their retained draft values.
        val cleared = SignUpConditionMapping.encode(SignUpUiState(
            allergies = setOf("milk"), vegetarianDiet = VegetarianDiet.Vegan,
        ))
        assertNotNull(cleared); assertNull(cleared?.allergies); assertNull(cleared?.specialDiet)
    }

    @Test fun `duplicate nickname preserves inputs and proof and final submit can recover without reservation`() = runTest {
        val vm = ready()
        response = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "NICKNAME_ALREADY_EXISTS"))
        assertFalse(vm.submit())
        assertEquals("Password123!", vm.uiState.value.password)
        assertNotNull(vm.uiState.value.nicknameError); assertNotNull(vm.currentVerificationProof())
        vm.updateNickname("새이름")
        assertFalse(vm.uiState.value.nicknameAvailable)
        response = AuthResult.Success(42L)
        assertTrue(vm.submit()); assertEquals("새이름", input!!.nickname)
        vm.reset()
    }

    @Test fun `expired proof leads to reverify while retaining the draft`() = runTest {
        val vm = ready()
        response = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 410, "VERIFICATION_EXPIRED"))
        assertFalse(vm.submit())
        assertNull(vm.currentVerificationProof())
        assertEquals(SignUpRecovery.ReverifyEmail, vm.uiState.value.recovery)
        vm.prepareReverification()
        assertEquals("사용자", vm.uiState.value.nickname)
        assertEquals("Password123!", vm.uiState.value.password)
        assertFalse(vm.submit()); assertEquals(1, calls)
        vm.reset()
    }

    @Test fun `lost response cannot be replayed and existing email never becomes completion`() = runTest {
        val vm = ready()
        response = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        assertFalse(vm.submit()); assertFalse(vm.submit()); assertEquals(1, calls)
        assertEquals(SignUpRecovery.CheckOutcome, vm.uiState.value.recovery)
        assertNull(vm.currentVerificationProof()); assertEquals("사용자", vm.uiState.value.nickname)
        vm.reset()
        val other = ready()
        response = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "EMAIL_ALREADY_REGISTERED"))
        assertFalse(other.submit())
        assertEquals(SignUpRecovery.Login, other.uiState.value.recovery)
        assertFalse(other.uiState.value.registrationCompleted)
        other.reset()
    }

    @Test fun `double tap and editing during submit cannot change the dispatched payload`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> {
                calls++; this@SignUpSubmissionTest.input = input; gate.await(); return response
            }
        }
        val vm = ready(delayed)
        val pending = async { vm.submit() }; runCurrent()
        assertFalse(vm.submit()); vm.updateNickname("변경"); vm.updatePassword("Changed123")
        vm.toggleCondition(SignUpCondition.LowSugar)
        assertEquals("사용자", vm.uiState.value.nickname)
        assertTrue(vm.uiState.value.selectedConditions.isEmpty())
        gate.complete(Unit); assertTrue(pending.await()); assertEquals(1, calls)
        assertEquals("Password123!", input!!.password)
        vm.reset()
    }

    @Test fun `reset discards late success without clearing the new draft`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> {
                withContext(NonCancellable) { gate.await() }; return response
            }
        }
        val vm = ready(delayed)
        val pending = async { vm.submit() }; runCurrent()
        vm.reset(); vm.updateEmail("other@example.com"); vm.updateNickname("다른사람")
        gate.complete(Unit); assertFalse(pending.await())
        assertFalse(vm.uiState.value.registrationCompleted)
        assertEquals("other@example.com", vm.uiState.value.email)
        vm.reset()
    }

    @Test fun `cancellation invalidates possibly consumed proof`() = runTest {
        val delayed = object : AuthRepository by repo {
            override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> { awaitCancellation() }
        }
        val vm = ready(delayed)
        val pending = async { vm.submit() }; runCurrent(); pending.cancelAndJoin()
        assertNull(vm.currentVerificationProof())
        assertEquals(SignUpRecovery.CheckOutcome, vm.uiState.value.recovery)
        assertFalse(vm.uiState.value.isSubmitting)
        vm.reset()
    }

    @Test fun `selected photo is not silently lost or treated as uploaded`() = runTest {
        val vm = ready()
        vm.updateProfilePhoto("content://photo", null)
        assertFalse(vm.submit()); assertEquals(0, calls)
        assertEquals("content://photo", vm.uiState.value.profilePhotoUri)
        vm.updateProfilePhoto(null, null)
        assertTrue(vm.submit())
        vm.reset()
    }

    @Test fun `missing proof and invalid fields never dispatch signup`() = runTest {
        val vm = SignUpViewModel(repo)
        assertFalse(vm.submit()); assertEquals(0, calls)
        vm.reset()
        val verified = ready()
        verified.updateNickname("!")
        assertFalse(verified.submit()); assertEquals(0, calls)
        verified.updateNickname("사용자"); verified.updatePassword("short")
        assertFalse(verified.submit()); assertEquals(SignUpRecovery.EditPassword, verified.uiState.value.recovery)
        verified.reset()
    }
}
