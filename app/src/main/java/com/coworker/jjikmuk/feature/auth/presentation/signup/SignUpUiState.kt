package com.coworker.jjikmuk.feature.auth.presentation.signup

import com.coworker.jjikmuk.feature.auth.presentation.common.AuthPasswordValidationError
import com.coworker.jjikmuk.feature.auth.presentation.common.authPasswordValidationError
import com.coworker.jjikmuk.feature.auth.presentation.common.isAuthPasswordValid

data class SignUpUiState(
    val email: String = "",
    val emailError: String? = null,
    val otp: String = "",
    val otpError: String? = null,
    val isOtpVerified: Boolean = false,
    val remainingOtpSeconds: Int = 0,
    val password: String = "",
    val passwordConfirm: String = "",
    val nickname: String = "",
    val nicknameError: String? = null,
    val hasVegetarianCondition: Boolean = false,
    val hasAllergyCondition: Boolean = false,
    val vegetarianDiets: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
) {
    val isEmailNotEmpty: Boolean
        get() = email.isNotEmpty()

    val isOtpComplete: Boolean
        get() = otp.length == 4

    val passwordValidationError: AuthPasswordValidationError?
        get() = authPasswordValidationError(password, passwordConfirm)

    val isPasswordValid: Boolean
        get() = isAuthPasswordValid(password, passwordConfirm)
}
