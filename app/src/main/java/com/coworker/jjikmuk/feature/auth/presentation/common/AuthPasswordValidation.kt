package com.coworker.jjikmuk.feature.auth.presentation.common

enum class AuthPasswordValidationError {
    TooShort,
    Mismatch,
}

fun authPasswordValidationError(
    password: String,
    passwordConfirm: String,
): AuthPasswordValidationError? = when {
    password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH ->
        AuthPasswordValidationError.TooShort
    password.length >= MIN_PASSWORD_LENGTH &&
        passwordConfirm.isNotEmpty() &&
        password != passwordConfirm -> AuthPasswordValidationError.Mismatch
    else -> null
}

fun isAuthPasswordValid(
    password: String,
    passwordConfirm: String,
): Boolean = password.length >= MIN_PASSWORD_LENGTH &&
    passwordConfirm.isNotEmpty() &&
    password == passwordConfirm

private const val MIN_PASSWORD_LENGTH = 6
