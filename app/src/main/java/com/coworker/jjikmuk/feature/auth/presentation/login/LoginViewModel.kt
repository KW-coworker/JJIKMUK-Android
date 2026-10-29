package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.AuthRepository
import com.coworker.jjikmuk.feature.auth.domain.AuthResult
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessions: AuthSessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
    val sessionState = sessions.state
    private var request: Job? = null
    private var revision = 0L

    init {
        viewModelScope.launch { sessions.restore() }
        viewModelScope.launch {
            sessions.state.collectLatest { state ->
                if (state.canEnterHome) {
                    while (sessions.isCurrent(state)) {
                        delay(1_000)
                    }
                    sessions.activeSnapshot() // Suspend expired Access eligibility; keep a usable Refresh for stage 2-2.
                }
            }
        }
    }

    fun updateEmail(email: String) {
        cancelRequest()
        _uiState.update { it.copy(email = email, loginError = null) }
    }
    fun updatePassword(password: String) {
        cancelRequest()
        _uiState.update { it.copy(password = password, loginError = null) }
    }
    fun clearPassword() = updatePassword("")

    suspend fun login(): Boolean {
        if (_uiState.value.isLoading) return false
        val input = _uiState.value
        if (input.email.isBlank() || input.password.isEmpty()) {
            _uiState.update { it.copy(loginError = "이메일과 비밀번호를 입력해 주세요") }
            return false
        }
        // Login deliberately does not enforce NEW-password creation rules on legacy accounts.
        val requestRevision = ++revision
        request = currentCoroutineContext()[Job]
        _uiState.update { it.copy(isLoading = true, loginError = null) }
        var attempt: Long? = null
        try {
            val activeAttempt = sessions.beginLogin()
            attempt = activeAttempt
            val startedAt = System.currentTimeMillis()
            currentCoroutineContext().ensureActive()
            val result = repository.login(input.email, input.password)
            currentCoroutineContext().ensureActive()
            if (requestRevision != revision) return false
            return when (result) {
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(loginError = result.error.userMessage()) }
                    false
                }
                is AuthResult.Success -> {
                    val accepted = sessions.acceptLogin(activeAttempt, result.value, startedAt)
                    currentCoroutineContext().ensureActive()
                    if (requestRevision != revision) {
                        sessions.clearIfCurrent(activeAttempt)
                        false
                    } else if (!accepted) {
                        _uiState.update { it.copy(loginError = "로그인 정보를 저장하지 못했어요. 다시 시도해 주세요") }
                        false
                    } else {
                        _uiState.update { it.copy(password = "", loginError = if (result.value.profileCompleted) null
                            else "프로필 입력이 필요해요. 추가 입력 기능을 준비 중이에요") }
                        result.value.profileCompleted // Never route incomplete profiles into Home.
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            attempt?.let { sessions.clearIfCurrent(it) }
            throw cancelled
        } finally {
            if (requestRevision == revision) {
                request = null
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun googleUnavailable() {
        cancelRequest()
        _uiState.update { it.copy(loginError = "Google 로그인을 준비 중이에요. 이메일 로그인을 이용해 주세요") }
    }

    fun logout() {
        reset()
        viewModelScope.launch { sessions.logout(repository) }
    }

    fun reset() {
        cancelRequest()
        _uiState.value = LoginUiState()
    }

    private fun cancelRequest() {
        revision++
        request?.cancel()
        request = null
        _uiState.update { it.copy(isLoading = false) }
    }
}
