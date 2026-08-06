package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

data class PasswordResetUiState(
    val email: String = "",
    val emailError: String? = null,
    val otp: String = "",
    val otpError: String? = null,
    val isOtpVerified: Boolean = false,
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val passwordError: String? = null,
)
