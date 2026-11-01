package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = TestAuthRepository()
    private val viewModel by lazy { PasswordResetViewModel(repository, AuthSessionManager(MemorySessionStore())) }

    @Test fun `server rejection preserves email and never navigates to OTP`() = runTest {
        repository.sendResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404, "USER_NOT_FOUND"))
        viewModel.updateEmail("unknown@example.com")
        assertFalse(viewModel.sendResetCode())
        runCurrent()
        assertNotNull(viewModel.uiState.value.emailError)
        assertEquals("unknown@example.com", viewModel.uiState.value.email)
        viewModel.reset()
    }
    @Test fun `server proof is required before resetting and cannot be reused after success`() = runTest {
        viewModel.updateEmail("user@example.com")
        assertTrue(viewModel.sendResetCode())
        viewModel.updateOtp("0427")
        assertTrue(viewModel.verifyOtp())
        assertEquals(EmailPurpose.PasswordReset, repository.lastPurpose)
        viewModel.updateNewPassword("password123")
        viewModel.updateNewPasswordConfirm("password123")
        assertTrue(viewModel.submitPassword())
        assertFalse(viewModel.submitPassword())
        assertEquals(1, repository.resets)
        assertEquals("", viewModel.uiState.value.newPassword)
        viewModel.reset()
    }
    @Test fun `known failure keeps proof and input while unknown result requires reauthentication`() = runTest {
        viewModel.updateEmail("user@example.com"); viewModel.sendResetCode()
        viewModel.updateOtp("0427"); viewModel.verifyOtp()
        viewModel.updateNewPassword("password123"); viewModel.updateNewPasswordConfirm("password123")
        repository.resetResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "PASSWORD_INVALID"))
        assertFalse(viewModel.submitPassword())
        assertEquals("password123", viewModel.uiState.value.newPassword)
        repository.resetResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        assertFalse(viewModel.submitPassword())
        assertFalse(viewModel.submitPassword())
        assertEquals(2, repository.resets)
        viewModel.reset()
    }
    @Test fun `back to email clears proof code and new passwords`() = runTest {
        viewModel.updateEmail("user@example.com"); viewModel.sendResetCode()
        viewModel.updateOtp("0427"); viewModel.verifyOtp()
        viewModel.updateNewPassword("password123")
        viewModel.restartFromEmail()
        runCurrent()
        assertFalse(viewModel.uiState.value.isOtpVerified)
        assertEquals("", viewModel.uiState.value.newPassword)
        assertFalse(viewModel.submitPassword())
        viewModel.reset()
    }
}
