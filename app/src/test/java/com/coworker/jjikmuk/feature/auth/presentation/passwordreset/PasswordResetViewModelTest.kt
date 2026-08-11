package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import com.coworker.jjikmuk.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PasswordResetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel by lazy { PasswordResetViewModel() }

    @Test
    fun `등록된 이메일에는 재설정 코드를 전송한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")

        assertTrue(viewModel.sendResetCode())
        assertEquals(180, viewModel.uiState.value.remainingOtpSeconds)
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `등록되지 않은 이메일에는 오류를 표시한다`() {
        viewModel.updateEmail("unknown@example.com")

        assertFalse(viewModel.sendResetCode())
        assertEquals("이메일 주소를 다시 확인해 주세요", viewModel.uiState.value.emailError)
    }

    @Test
    fun `최초 OTP와 재전송 OTP를 각각 검증한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")
        viewModel.sendResetCode()
        viewModel.updateOtp("1133")
        assertTrue(viewModel.verifyOtp())

        viewModel.resendOtp()
        viewModel.updateOtp("1133")
        assertFalse(viewModel.verifyOtp())
        viewModel.updateOtp("2468")
        assertTrue(viewModel.verifyOtp())
    }

    @Test
    fun `이메일 단계로 돌아가면 OTP와 새 비밀번호를 초기화한다`() {
        viewModel.updateEmail("coworker@kw.ac.kr")
        viewModel.sendResetCode()
        viewModel.updateOtp("1133")
        viewModel.updateNewPassword("password123")
        viewModel.updateNewPasswordConfirm("password123")

        viewModel.restartFromEmail()

        val state = viewModel.uiState.value
        assertEquals("", state.otp)
        assertFalse(state.isOtpVerified)
        assertEquals(0, state.remainingOtpSeconds)
        assertEquals("", state.newPassword)
        assertEquals("", state.newPasswordConfirm)
    }
}
