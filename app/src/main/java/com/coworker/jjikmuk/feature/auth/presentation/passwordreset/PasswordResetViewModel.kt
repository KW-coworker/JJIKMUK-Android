package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.EmailVerificationController
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessions: AuthSessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState = _uiState.asStateFlow()
    private val verification = EmailVerificationController(repository, EmailPurpose.PasswordReset, viewModelScope)
    private var revision = 0L
    private var submitting = false

    init {
        viewModelScope.launch {
            verification.state.collect { state ->
                _uiState.update { it.copy(otp = state.code, otpError = state.error,
                    emailError = state.error, isOtpVerified = state.verified,
                    remainingOtpSeconds = state.remainingSeconds, resendSeconds = state.resendSeconds,
                    isAuthRequestInFlight = state.busy) }
            }
        }
    }

    fun updateEmail(email: String) {
        revision++
        verification.reset(email)
        _uiState.value = PasswordResetUiState(email = email)
    }
    suspend fun sendResetCode(): Boolean = verification.send()
    fun updateOtp(otp: String) = verification.updateCode(otp)
    suspend fun verifyOtp(): Boolean = verification.verify()
    suspend fun resendOtp(): Boolean = verification.send()
    fun updateNewPassword(password: String) { if (!submitting) _uiState.update { it.copy(newPassword = password, submitError = null) } }
    fun updateNewPasswordConfirm(password: String) { if (!submitting) _uiState.update { it.copy(newPasswordConfirm = password, submitError = null) } }

    suspend fun submitPassword(): Boolean {
        if (submitting || !_uiState.value.isNewPasswordValid) return false
        val proof = verification.currentProof()
        if (proof == null) {
            _uiState.update { it.copy(submitError = "이메일 인증을 다시 진행해 주세요") }
            return false
        }
        val currentRevision = revision
        submitting = true
        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
        try {
            val result = repository.resetPassword(proof.email, proof.token, _uiState.value.newPassword)
            if (currentRevision != revision) return false
            return when (result) {
                is AuthResult.Success -> {
                    sessions.clearForEmail(proof.email)
                    verification.invalidateProof()
                    _uiState.update { it.copy(newPassword = "", newPasswordConfirm = "") }
                    true
                }
                is AuthResult.Failure -> {
                    if (result.error.outcomeUnknown || result.error.code in setOf(
                        "VERIFICATION_EXPIRED", "VERIFICATION_REQUIRED", "VERIFICATION_TOKEN_INVALID")) {
                        verification.invalidateProof()
                    }
                    _uiState.update { it.copy(submitError = if (result.error.outcomeUnknown)
                        "변경 결과를 확인하지 못했어요. 로그인으로 확인하거나 이메일 인증을 다시 진행해 주세요"
                        else result.error.userMessage()) }
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            if (currentRevision == revision) {
                verification.invalidateProof()
                _uiState.update { it.copy(submitError = "변경 결과를 확인하지 못했어요. 이메일 인증을 다시 진행해 주세요") }
            }
            throw cancelled
        } finally {
            submitting = false
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun restartFromEmail() {
        revision++
        verification.reset(_uiState.value.email)
        _uiState.update { it.copy(otp = "", otpError = null, isOtpVerified = false,
            remainingOtpSeconds = 0, newPassword = "", newPasswordConfirm = "", submitError = null) }
    }
    fun reset() {
        revision++
        verification.reset()
        _uiState.value = PasswordResetUiState()
    }
}
