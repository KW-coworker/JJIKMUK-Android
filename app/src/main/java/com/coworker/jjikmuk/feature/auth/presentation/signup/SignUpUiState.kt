package com.coworker.jjikmuk.feature.auth.presentation.signup

data class SignUpUiState(
    val email: String = "",
    val emailError: String? = null,
    val otp: String = "",
    val otpError: String? = null,
    val isOtpVerified: Boolean = false,
    val password: String = "",
    val passwordConfirm: String = "",
    val passwordError: String? = null,
    val nickname: String = "",
    val nicknameError: String? = null,
    val hasVegetarianCondition: Boolean = false,
    val hasAllergyCondition: Boolean = false,
    val vegetarianDiets: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
) {
    val isEmailNotEmpty: Boolean
        get() = email.isNotEmpty()
}
