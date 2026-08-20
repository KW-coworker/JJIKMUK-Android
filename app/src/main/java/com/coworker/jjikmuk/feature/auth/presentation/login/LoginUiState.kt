package com.coworker.jjikmuk.feature.auth.presentation.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loginError: String? = null,
)
