package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PasswordResetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                otp = "",
                otpError = null,
                isOtpVerified = false,
                newPassword = "",
                newPasswordConfirm = "",
                passwordError = null,
            )
        }
    }

    fun updateOtp(otp: String) {
        _uiState.update { it.copy(otp = otp, otpError = null) }
    }

    fun markOtpVerified() {
        _uiState.update { it.copy(isOtpVerified = true, otpError = null) }
    }

    fun updateNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password, passwordError = null) }
    }

    fun updateNewPasswordConfirm(password: String) {
        _uiState.update { it.copy(newPasswordConfirm = password, passwordError = null) }
    }

    fun restartFromEmail() {
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                isOtpVerified = false,
                newPassword = "",
                newPasswordConfirm = "",
                passwordError = null,
            )
        }
    }

    fun reset() {
        _uiState.value = PasswordResetUiState()
    }
}
