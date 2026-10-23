package com.coworker.jjikmuk.feature.auth.presentation.common

enum class AuthPasswordValidationError { TooShort, Requirements, Mismatch }

// Only new-password creation uses this policy. Existing account login is unaffected.
fun authPasswordValidationError(password: String, passwordConfirm: String): AuthPasswordValidationError? = when {
    password.isNotEmpty() && password.length < 8 -> AuthPasswordValidationError.TooShort
    password.isNotEmpty() && !meetsNewPasswordRequirements(password) -> AuthPasswordValidationError.Requirements
    passwordConfirm.isNotEmpty() && password != passwordConfirm -> AuthPasswordValidationError.Mismatch
    else -> null
}

fun isAuthPasswordValid(password: String, passwordConfirm: String): Boolean =
    meetsNewPasswordRequirements(password) && passwordConfirm.isNotEmpty() && password == passwordConfirm

private fun meetsNewPasswordRequirements(password: String): Boolean = password.length >= 8 &&
    password.toByteArray(Charsets.UTF_8).size <= 72 &&
    password.any { it in 'a'..'z' || it in 'A'..'Z' } && password.any { it in '0'..'9' } &&
    password.none { it.isWhitespace() || Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HANGUL }
// Special characters aren't required. A stricter allowlist awaits the backend agreement.
