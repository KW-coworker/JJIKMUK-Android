package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import com.coworker.jjikmuk.feature.auth.presentation.common.AuthPasswordValidationError
import com.coworker.jjikmuk.feature.auth.presentation.common.authPasswordValidationError
import com.coworker.jjikmuk.feature.auth.presentation.common.isAuthPasswordValid

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

    val newPasswordError: AuthPasswordValidationError?
        get() = authPasswordValidationError(newPassword, newPasswordConfirm)

    val isNewPasswordValid: Boolean
        get() = isAuthPasswordValid(newPassword, newPasswordConfirm)
}
