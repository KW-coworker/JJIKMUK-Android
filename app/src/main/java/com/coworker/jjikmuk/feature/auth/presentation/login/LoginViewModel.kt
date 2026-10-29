package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.AuthRepository
import com.coworker.jjikmuk.feature.auth.domain.AuthResult
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import com.coworker.jjikmuk.feature.auth.session.ServerLogoutStatus
import com.coworker.jjikmuk.feature.auth.session.AuthProfiles
import com.coworker.jjikmuk.feature.auth.session.AuthenticatedRequests
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
    private val profiles: AuthProfiles = AuthProfiles(repository, sessions, AuthenticatedRequests(sessions, repository)),
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
    val sessionState = sessions.state
    private var entryRequest: Job? = null
    private var entryGeneration: Long? = null
    private val _entryState = MutableStateFlow<SessionEntryState>(SessionEntryState.Checking)
    val entryState = _entryState.asStateFlow()
    private var request: Job? = null
    private var revision = 0L
    private var logoutRequest: Job? = null
    private var logoutSourceGeneration: Long? = null
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notices = _notices.asSharedFlow()

    init {
        viewModelScope.launch { sessions.restore() }
        viewModelScope.launch {
            sessions.state.collectLatest { state ->
                state.failure?.let { error -> _uiState.update { it.copy(loginError = error.userMessage()) } }
                if (state.canEnterHome && !state.refreshing) {
                    while (sessions.isCurrent(state)) {
                        delay(1_000)
                    }
                    if (sessions.isSameSession(state)) sessions.accessForRequest(repository)
                }
            }
        }
    }

    suspend fun hasRestoredSession(): Boolean {
        sessions.restore()
        return sessions.state.value.credentials != null
    }

    fun checkSession() {
        val generation = sessions.state.value.generation
        if (entryRequest?.isActive == true && entryGeneration == generation) return
        entryRequest?.cancel()
        entryGeneration = generation
        _entryState.value = SessionEntryState.Checking
        entryRequest = viewModelScope.launch {
            _entryState.value = SessionEntryState.Checking
            sessions.restore()
            val observed = sessions.state.value
            if (observed.credentials == null) {
                _entryState.value = SessionEntryState.SignedOut
                return@launch
            }
            val result = profiles.load()
            if (sessions.state.value.generation != observed.generation) {
                // Logout/invalid token may clear this session, but a newer account owns its own routing.
                if (sessions.state.value.credentials == null) _entryState.value = SessionEntryState.SignedOut
                return@launch
            }
            _entryState.value = when (result) {
                is AuthResult.Success -> if (result.value.profileCompleted) SessionEntryState.Ready
                    else SessionEntryState.ProfileRequired
                is AuthResult.Failure -> SessionEntryState.Retry(result.error)
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
                        _uiState.update { it.copy(password = "", loginError = null) }
                        true // The entry gate checks me before choosing Home or incomplete profile.
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
        if (logoutRequest?.isActive == true && (sessions.state.value.credentials == null ||
                sessions.state.value.generation == logoutSourceGeneration)) return
        reset()
        logoutSourceGeneration = sessions.state.value.generation
        val logoutRevision = revision
        logoutRequest = viewModelScope.launch {
            val result = sessions.logout(repository)
            // A previous account's delayed logout must not alter the new login form or show its notice.
            if (revision != logoutRevision || sessions.state.value.generation != result.generation ||
                sessions.state.value.credentials != null) return@launch
            val message = when {
                !result.localStorageCleared -> "기기 로그인 정보를 완전히 지우지 못했어요. 다시 시도해 주세요"
                result.serverStatus == ServerLogoutStatus.Confirmed -> "로그아웃했어요"
                result.serverStatus == ServerLogoutStatus.Unconfirmed -> "기기에서는 로그아웃했어요. 서버 로그아웃 완료는 확인하지 못했어요"
                else -> null
            }
            message?.let(_notices::tryEmit)
        }
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
