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
        val normalizedEmail = email.trim()
        _uiState.update {
            it.copy(
                email = email,
                emailError = when {
                    normalizedEmail.isEmpty() -> null
                    !EMAIL_REGEX.matches(normalizedEmail) -> INVALID_EMAIL_FORMAT_MESSAGE
                    else -> null
                },
                otp = "",
                otpError = null,
                isOtpVerified = false,
                newPassword = "",
                newPasswordConfirm = "",
                passwordError = null,
            )
        }
    }

    fun sendResetCode(): Boolean {
        val email = _uiState.value.email.trim()
        val error = when {
            !EMAIL_REGEX.matches(email) -> INVALID_EMAIL_FORMAT_MESSAGE
            email !in FakePasswordResetAccounts.registeredEmails -> EMAIL_NOT_FOUND_MESSAGE
            else -> null
        }

        _uiState.update { it.copy(emailError = error) }
        return error == null
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

private object FakePasswordResetAccounts {
    val registeredEmails = setOf(
        "coworker@kw.ac.kr",
        "email@example.com",
    )
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private const val INVALID_EMAIL_FORMAT_MESSAGE = "올바른 이메일 주소 형식이 아닙니다"
private const val EMAIL_NOT_FOUND_MESSAGE = "이메일 주소를 다시 확인해 주세요"
