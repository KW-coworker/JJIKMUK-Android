package com.coworker.jjikmuk.feature.auth.presentation.login

data class LoginUiState(
    val retrySeconds: Int = 0,
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val loginError: String? = null,
)
