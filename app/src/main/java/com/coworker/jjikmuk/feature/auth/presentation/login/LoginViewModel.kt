package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, loginError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, loginError = null) }
    }

    fun clearPassword() {
        _uiState.update { it.copy(password = "", passwordError = null, loginError = null) }
    }

    fun login(): Boolean {
        val state = _uiState.value
        val isValid = FakeAuthValidator.isValidLogin(
            email = state.email,
            password = state.password,
        )

        _uiState.update {
            it.copy(loginError = if (isValid) null else LOGIN_ERROR_MESSAGE)
        }
        return isValid
    }

    fun reset() {
        _uiState.value = LoginUiState()
    }
}

private object FakeAuthValidator {
    fun isValidLogin(email: String, password: String): Boolean {
        return email.trim() == MOCK_EMAIL && password == MOCK_PASSWORD
    }

    private const val MOCK_EMAIL = "email@example.com"
    private const val MOCK_PASSWORD = "password123"
}

private const val LOGIN_ERROR_MESSAGE = "이메일 또는 비밀번호를 다시 확인해 주세요"
