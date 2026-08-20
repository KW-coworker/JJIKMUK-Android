package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PasswordResetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()
    private val _events = Channel<PasswordResetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var expectedOtp = INITIAL_MOCK_OTP
    private var otpTimerJob: Job? = null

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
        if (error == null) {
            expectedOtp = INITIAL_MOCK_OTP
            startOtpTimer()
        }
        return error == null
    }

    fun updateOtp(otp: String) {
        _uiState.update {
            it.copy(
                otp = otp.filter(Char::isDigit).take(OTP_LENGTH),
                otpError = null,
            )
        }
    }

    fun verifyOtp(): Boolean {
        val state = _uiState.value
        val isValid = state.remainingOtpSeconds > 0 && state.otp == expectedOtp

        _uiState.update {
            it.copy(
                isOtpVerified = isValid,
                otpError = if (isValid) null else OTP_MISMATCH_MESSAGE,
            )
        }
        if (isValid) otpTimerJob?.cancel()
        return isValid
    }

    fun resendOtp() {
        expectedOtp = RESENT_MOCK_OTP
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                isOtpVerified = false,
            )
        }
        startOtpTimer()
        _events.trySend(PasswordResetEvent.OtpResent)
    }

    fun updateNewPassword(password: String) {
        _uiState.update { it.copy(newPassword = password) }
    }

    fun updateNewPasswordConfirm(password: String) {
        _uiState.update { it.copy(newPasswordConfirm = password) }
    }

    fun restartFromEmail() {
        otpTimerJob?.cancel()
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                isOtpVerified = false,
                remainingOtpSeconds = 0,
                newPassword = "",
                newPasswordConfirm = "",
            )
        }
    }

    fun reset() {
        otpTimerJob?.cancel()
        _uiState.value = PasswordResetUiState()
    }

    private fun startOtpTimer() {
        otpTimerJob?.cancel()
        _uiState.update { it.copy(remainingOtpSeconds = OTP_DURATION_SECONDS) }
        otpTimerJob = viewModelScope.launch {
            while (_uiState.value.remainingOtpSeconds > 0) {
                delay(1_000L)
                val nextSeconds = (_uiState.value.remainingOtpSeconds - 1).coerceAtLeast(0)
                _uiState.update { it.copy(remainingOtpSeconds = nextSeconds) }
                if (nextSeconds == 0) {
                    _events.send(PasswordResetEvent.OtpExpired)
                }
            }
        }
    }
}

sealed interface PasswordResetEvent {
    data object OtpResent : PasswordResetEvent
    data object OtpExpired : PasswordResetEvent
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
private const val INITIAL_MOCK_OTP = "1133"
private const val RESENT_MOCK_OTP = "2468"
private const val OTP_MISMATCH_MESSAGE = "인증번호를 다시 확인해 주세요"
private const val OTP_LENGTH = 4
private const val OTP_DURATION_SECONDS = 180
