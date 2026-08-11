package com.coworker.jjikmuk.feature.auth.presentation.signup

import com.coworker.jjikmuk.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel by lazy { SignUpViewModel() }

    @Test
    fun `이메일과 최초 OTP를 검증한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")
        assertTrue(viewModel.validateEmail())
        viewModel.updateOtp("1133")

        assertTrue(viewModel.verifyOtp())
        assertTrue(viewModel.uiState.value.isOtpVerified)
    }

    @Test
    fun `OTP 재전송 후에는 새로운 번호만 성공한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")
        viewModel.validateEmail()
        viewModel.resendOtp()

        viewModel.updateOtp("1133")
        assertFalse(viewModel.verifyOtp())
        viewModel.updateOtp("2468")
        assertTrue(viewModel.verifyOtp())
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
