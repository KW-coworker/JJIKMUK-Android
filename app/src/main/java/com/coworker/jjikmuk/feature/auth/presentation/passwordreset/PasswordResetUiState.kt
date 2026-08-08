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
    val passwordError: String? = null,
) {
    val isEmailNotEmpty: Boolean
        get() = email.isNotEmpty()

    val isOtpComplete: Boolean
        get() = otp.length == 4
}
