package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

data class PasswordResetUiState(
    val email: String = "",
    val emailError: String? = null,
    val otp: String = "",
    val otpError: String? = null,
    val isOtpVerified: Boolean = false,
    val remainingOtpSeconds: Int = 0,
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
) {
    val isEmailNotEmpty: Boolean
        get() = email.isNotEmpty()

    val isOtpComplete: Boolean
        get() = otp.length == 4

    val newPasswordError: PasswordValidationError?
        get() = when {
            newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH ->
                PasswordValidationError.TooShort
            newPassword.length >= MIN_PASSWORD_LENGTH &&
                newPasswordConfirm.isNotEmpty() &&
                newPassword != newPasswordConfirm -> PasswordValidationError.Mismatch
            else -> null
        }

    val isNewPasswordValid: Boolean
        get() = newPassword.length >= MIN_PASSWORD_LENGTH &&
            newPasswordConfirm.isNotEmpty() &&
            newPassword == newPasswordConfirm
}

enum class PasswordValidationError {
    TooShort,
    Mismatch,
}

private const val MIN_PASSWORD_LENGTH = 6
