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
    val selectedConditions: Set<SignUpCondition> = emptySet(),
    val vegetarianDiet: VegetarianDiet? = null,
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

    val isNicknameNotBlank: Boolean
        get() = nickname.isNotBlank()

    val hasVegetarianCondition: Boolean
        get() = SignUpCondition.Vegetarian in selectedConditions

    val hasAllergyCondition: Boolean
        get() = SignUpCondition.Allergy in selectedConditions

    val hasSelectedCondition: Boolean
        get() = selectedConditions.isNotEmpty()
}

enum class SignUpCondition {
    Allergy,
    Vegetarian,
    LowSugar,
    LowSodium,
    GlutenFree,
    LowCalorie,
    LowFat,
    HighProtein,
}

enum class VegetarianDiet {
    Vegan,
    Lacto,
    Ovo,
    LactoOvo,
    Pesco,
    Pollo,
}
