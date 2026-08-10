package com.coworker.jjikmuk.feature.auth.presentation.signup

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

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()
    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var expectedOtp = INITIAL_MOCK_OTP
    private var otpTimerJob: Job? = null

    fun updateEmail(email: String) {
        otpTimerJob?.cancel()
        val normalizedEmail = email.trim()
        _uiState.update {
            it.copy(
                email = email,
                emailError = when {
                    normalizedEmail.isEmpty() -> null
                    !EMAIL_REGEX.matches(normalizedEmail) -> INVALID_EMAIL_MESSAGE
                    else -> null
                },
                otp = "",
                otpError = null,
                isOtpVerified = false,
                remainingOtpSeconds = 0,
                password = "",
                passwordConfirm = "",
                nickname = "",
                nicknameError = null,
                selectedConditions = emptySet(),
                vegetarianDiet = null,
                allergies = emptySet(),
            )
        }
    }

    fun validateEmail(): Boolean {
        val email = _uiState.value.email.trim()
        val error = if (EMAIL_REGEX.matches(email)) null else INVALID_EMAIL_MESSAGE
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
        _events.trySend(SignUpEvent.OtpResent)
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun updatePasswordConfirm(password: String) {
        _uiState.update { it.copy(passwordConfirm = password) }
    }

    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, nicknameError = null) }
    }

    fun validateNickname(): Boolean {
        val nickname = _uiState.value.nickname.trim()
        val error = if (nickname in FakeSignUpAccounts.registeredNicknames) {
            DUPLICATE_NICKNAME_MESSAGE
        } else {
            null
        }
        _uiState.update { it.copy(nicknameError = error) }
        return nickname.isNotEmpty() && error == null
    }

    fun toggleCondition(condition: SignUpCondition) {
        _uiState.update {
            val updatedConditions = if (condition in it.selectedConditions) {
                it.selectedConditions - condition
            } else {
                it.selectedConditions + condition
            }
            it.copy(
                selectedConditions = updatedConditions,
                vegetarianDiet = if (SignUpCondition.Vegetarian in updatedConditions) {
                    it.vegetarianDiet
                } else {
                    null
                },
                allergies = if (SignUpCondition.Allergy in updatedConditions) {
                    it.allergies
                } else {
                    emptySet()
                },
            )
        }
    }

    fun selectVegetarianDiet(diet: VegetarianDiet) {
        _uiState.update { it.copy(vegetarianDiet = diet) }
    }

    fun updateAllergies(allergies: Set<String>) {
        _uiState.update { it.copy(allergies = allergies) }
    }

    fun toggleAllergy(allergy: String) {
        _uiState.update {
            val updatedAllergies = if (allergy in it.allergies) {
                it.allergies - allergy
            } else {
                it.allergies + allergy
            }
            it.copy(allergies = updatedAllergies)
        }
    }

    fun restartFromEmail() {
        otpTimerJob?.cancel()
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                isOtpVerified = false,
                remainingOtpSeconds = 0,
                password = "",
                passwordConfirm = "",
            )
        }
    }

    fun reset() {
        otpTimerJob?.cancel()
        _uiState.value = SignUpUiState()
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
                    _events.send(SignUpEvent.OtpExpired)
                }
            }
        }
    }
}

sealed interface SignUpEvent {
    data object OtpResent : SignUpEvent
    data object OtpExpired : SignUpEvent
}

private object FakeSignUpAccounts {
    val registeredNicknames = setOf("타로")
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private const val INVALID_EMAIL_MESSAGE = "올바른 이메일 주소 형식이 아니에요"
private const val INITIAL_MOCK_OTP = "1133"
private const val RESENT_MOCK_OTP = "2468"
private const val OTP_MISMATCH_MESSAGE = "인증번호를 다시 확인해 주세요"
private const val OTP_LENGTH = 4
private const val OTP_DURATION_SECONDS = 180
private const val DUPLICATE_NICKNAME_MESSAGE = "이미 사용중인 닉네임입니다"
