package com.coworker.jjikmuk.feature.auth.presentation.signup

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.TestAuthRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TestAuthRepository()
    private val viewModel by lazy { SignUpViewModel(repository) }

    @Test
    fun `server OTP proof is kept and changing email invalidates it`() = runTest {
        viewModel.updateEmail("coworker@kw.ac.kr")
        assertTrue(viewModel.validateEmail())
        viewModel.updateOtp("0427")
        assertTrue(viewModel.verifyOtp())
        runCurrent()
        assertTrue(viewModel.uiState.value.isOtpVerified)
        assertEquals("0427", repository.lastCode)
        viewModel.updateEmail("other@example.com")
        assertNull(viewModel.currentVerificationProof())
        viewModel.reset()
    }

    @Test
    fun `editing nickname invalidates availability without clearing other fields`() = runTest {
        viewModel.updateNickname("first")
        assertTrue(viewModel.validateNickname())
        assertTrue(viewModel.uiState.value.nicknameAvailable)
        viewModel.updatePassword("password123")
        viewModel.updateNickname("second")
        assertFalse(viewModel.uiState.value.nicknameAvailable)
        assertEquals("password123", viewModel.uiState.value.password)
        viewModel.reset()
    }

    @Test
    fun `채식 조건을 해제하면 식단 선택도 제거한다`() {
        viewModel.toggleCondition(SignUpCondition.Vegetarian)
        viewModel.selectVegetarianDiet(VegetarianDiet.Vegan)

        viewModel.toggleCondition(SignUpCondition.Vegetarian)

        assertNull(viewModel.uiState.value.vegetarianDiet)
    }

    @Test
    fun `알레르기 조건을 해제하면 알레르기 선택도 제거한다`() {
        viewModel.toggleCondition(SignUpCondition.Allergy)
        viewModel.toggleAllergy("milk")
        viewModel.toggleAllergy("egg")

        viewModel.toggleCondition(SignUpCondition.Allergy)

        assertTrue(viewModel.uiState.value.allergies.isEmpty())
    }

    @Test
    fun `프로필에서 수정한 값은 상태에 유지된다`() {
        viewModel.updateNickname("새 닉네임")
        viewModel.toggleCondition(SignUpCondition.LowSugar)
        viewModel.updateProfilePhoto(uri = "content://profile", bitmap = null)

        val state = viewModel.uiState.value
        assertEquals("새 닉네임", state.nickname)
        assertTrue(SignUpCondition.LowSugar in state.selectedConditions)
        assertEquals("content://profile", state.profilePhotoUri)
    }

    @Test
    fun `회원가입 상태를 초기화한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")
        viewModel.updateNickname("닉네임")
        viewModel.toggleCondition(SignUpCondition.LowSodium)

        viewModel.reset()

        assertEquals(SignUpUiState(), viewModel.uiState.value)
    }
}
