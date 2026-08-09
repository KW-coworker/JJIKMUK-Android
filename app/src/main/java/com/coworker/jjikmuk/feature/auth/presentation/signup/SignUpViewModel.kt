package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
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
                password = "",
                passwordConfirm = "",
                passwordError = null,
                nickname = "",
                nicknameError = null,
                hasVegetarianCondition = false,
                hasAllergyCondition = false,
                vegetarianDiets = emptySet(),
                allergies = emptySet(),
            )
        }
    }

    fun validateEmail(): Boolean {
        val email = _uiState.value.email.trim()
        val error = if (EMAIL_REGEX.matches(email)) null else INVALID_EMAIL_MESSAGE
        _uiState.update { it.copy(emailError = error) }
        return error == null
    }

    fun updateOtp(otp: String) {
        _uiState.update { it.copy(otp = otp, otpError = null) }
    }

    fun markOtpVerified() {
        _uiState.update { it.copy(isOtpVerified = true, otpError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updatePasswordConfirm(password: String) {
        _uiState.update { it.copy(passwordConfirm = password, passwordError = null) }
    }

    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, nicknameError = null) }
    }

    fun updateConditions(
        hasVegetarianCondition: Boolean,
        hasAllergyCondition: Boolean,
    ) {
        _uiState.update {
            it.copy(
                hasVegetarianCondition = hasVegetarianCondition,
                hasAllergyCondition = hasAllergyCondition,
                vegetarianDiets = if (hasVegetarianCondition) it.vegetarianDiets else emptySet(),
                allergies = if (hasAllergyCondition) it.allergies else emptySet(),
            )
        }
    }

    fun updateVegetarianDiets(diets: Set<String>) {
        _uiState.update { it.copy(vegetarianDiets = diets) }
    }

    fun updateAllergies(allergies: Set<String>) {
        _uiState.update { it.copy(allergies = allergies) }
    }

    fun restartFromEmail() {
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                isOtpVerified = false,
                password = "",
                passwordConfirm = "",
                passwordError = null,
            )
        }
    }

    fun reset() {
        _uiState.value = SignUpUiState()
    }
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private const val INVALID_EMAIL_MESSAGE = "올바른 이메일 주소 형식이 아니에요"
