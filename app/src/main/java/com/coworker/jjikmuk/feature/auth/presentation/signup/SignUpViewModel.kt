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
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
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
