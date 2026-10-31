package com.coworker.jjikmuk.feature.auth.presentation.signup

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.EmailVerificationController
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()
    private val verification = EmailVerificationController(repository, EmailPurpose.SignUp, viewModelScope)
    private var revision = 0L
    private var nicknameRequest = false
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
        if (email == _uiState.value.email) return
        revision++
        verification.reset(email)
        _uiState.value = SignUpUiState(email = email)
    }

    suspend fun validateEmail(): Boolean = !submitting && verification.send()
    fun updateOtp(otp: String) = verification.updateCode(otp)
    suspend fun verifyOtp(): Boolean = !submitting && verification.verify()
    suspend fun resendOtp(): Boolean = !submitting && verification.send()
    // The final signup adapter must obtain this proof again immediately before submission.
    fun currentVerificationProof(): EmailProof? = verification.currentProof()

    fun updatePassword(password: String) { if (!submitting) _uiState.update { it.copy(password = password, submitError = null, recovery = SignUpRecovery.None) } }
    fun updatePasswordConfirm(password: String) { if (!submitting) _uiState.update { it.copy(passwordConfirm = password, submitError = null, recovery = SignUpRecovery.None) } }
    fun updateNickname(nickname: String) {
        if (submitting) return
        revision++
        _uiState.update { it.copy(nickname = nickname, nicknameError = null, nicknameAvailable = false, submitError = null) }
    }

    suspend fun validateNickname(): Boolean {
        if (nicknameRequest || submitting) return false
        val nickname = normalizeAuthNickname(_uiState.value.nickname)
        if (!validNickname(nickname)) {
            _uiState.update { it.copy(nicknameError = "닉네임은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요") }
            return false
        }
        nicknameRequest = true
        val currentRevision = revision
        _uiState.update { it.copy(isNicknameChecking = true, nicknameError = null) }
        try {
            val result = repository.nicknameAvailable(nickname)
            if (currentRevision != revision) return false
            val available = result is AuthResult.Success && result.value
            _uiState.update { it.copy(nicknameAvailable = available, nicknameError = when (result) {
                is AuthResult.Success -> if (available) null else "이미 사용 중인 닉네임이에요"
                is AuthResult.Failure -> result.error.userMessage()
            }) }
            return available
        } finally {
            nicknameRequest = false
            _uiState.update { it.copy(isNicknameChecking = false) }
        }
    }

    fun toggleCondition(condition: SignUpCondition) {
        if (submitting) return
        _uiState.update {
            val updatedConditions = if (condition in it.selectedConditions) {
                it.selectedConditions - condition
            } else {
                it.selectedConditions + condition
            }
            it.copy(
                selectedConditions = updatedConditions,
                vegetarianDiet = if (SignUpCondition.Vegetarian in updatedConditions) {
                    it.vegetarianDiet
                } else {
                    null
                },
                allergies = if (SignUpCondition.Allergy in updatedConditions) {
                    it.allergies
                } else {
                    emptySet()
                },
            )
        }
    }

    fun selectVegetarianDiet(diet: VegetarianDiet) {
        if (submitting) return
        _uiState.update { it.copy(vegetarianDiet = diet) }
    }

    fun updateAllergies(allergies: Set<String>) {
        if (submitting) return
        _uiState.update { it.copy(allergies = allergies) }
    }

    fun toggleAllergy(allergy: String) {
        if (submitting) return
        _uiState.update {
            val updatedAllergies = if (allergy in it.allergies) {
                it.allergies - allergy
            } else {
                it.allergies + allergy
            }
            it.copy(allergies = updatedAllergies)
        }
    }

    fun updateProfilePhoto(uri: String?, bitmap: Bitmap?) {
        if (submitting) return
        _uiState.update {
            it.copy(
                profilePhotoUri = uri,
                profilePhotoBitmap = bitmap,
            )
        }
    }


    /** A successful reservation check is not required: signup performs the final uniqueness check. */
    suspend fun submit(): Boolean {
        val input = _uiState.value
        if (submitting || nicknameRequest || input.isAuthRequestInFlight || input.registrationCompleted ||
            input.recovery != SignUpRecovery.None) return false
        val proof = verification.currentProof()
        if (proof == null || proof.email != normalizeAuthEmail(input.email) || proof.purpose != EmailPurpose.SignUp) {
            submissionError("이메일 인증이 만료됐어요. 입력한 내용을 유지하고 다시 인증해 주세요", SignUpRecovery.ReverifyEmail)
            return false
        }
        if (!input.isPasswordValid) {
            submissionError("비밀번호 입력 조건과 확인값을 다시 확인해 주세요", SignUpRecovery.EditPassword)
            return false
        }
        val nickname = normalizeAuthNickname(input.nickname)
        if (!validNickname(nickname)) {
            val message = "닉네임은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요"
            _uiState.update { it.copy(nicknameError = message, submitError = message) }
            return false
        }
        val conditions = SignUpConditionMapping.encode(input)
        if (conditions == null) {
            submissionError("선택한 일부 식이조건은 아직 등록할 수 없어요. 선택한 항목을 유지하고 나중에 다시 시도해 주세요")
            return false
        }
        if (input.profilePhotoUri != null || input.profilePhotoBitmap != null) {
            submissionError("사진 등록은 준비 중이에요. 사진 선택을 해제한 뒤 가입해 주세요")
            return false
        }
        val currentRevision = revision
        submitting = true
        _uiState.update { it.copy(isSubmitting = true, submitError = null, nicknameError = null) }
        try {
            val result = repository.signUp(SignUpSubmission(proof.email, input.password, nickname,
                proof.token, conditions.allergies, conditions.specialDiet))
            currentCoroutineContext().ensureActive()
            if (currentRevision != revision) return false
            return when (result) {
                is AuthResult.Success -> {
                    // No login token is issued by signup. Consume the proof and clear sensitive input.
                    verification.reset()
                    _uiState.value = SignUpUiState(registrationCompleted = true)
                    true
                }
                is AuthResult.Failure -> {
                    val error = result.error
                    val recovery = when {
                        error.outcomeUnknown -> SignUpRecovery.CheckOutcome
                        error.code in setOf("VERIFICATION_EXPIRED", "VERIFICATION_REQUIRED", "VERIFICATION_TOKEN_INVALID") -> SignUpRecovery.ReverifyEmail
                        error.code == "EMAIL_ALREADY_REGISTERED" -> SignUpRecovery.Login
                        error.code == "PASSWORD_INVALID" || error.field == "password" -> SignUpRecovery.EditPassword
                        else -> SignUpRecovery.None
                    }
                    if (recovery in setOf(SignUpRecovery.ReverifyEmail, SignUpRecovery.CheckOutcome, SignUpRecovery.Login))
                        verification.invalidateProof()
                    val message = if (error.outcomeUnknown)
                        "가입 결과를 확인하지 못했어요. 먼저 로그인을 확인하거나 이메일 인증을 다시 진행해 주세요"
                        else error.userMessage()
                    _uiState.update { it.copy(submitError = message, recovery = recovery,
                        nicknameError = if (error.code in setOf("NICKNAME_ALREADY_EXISTS", "NICKNAME_INVALID") || error.field == "nickname") message else it.nicknameError,
                        nicknameAvailable = if (error.code == "NICKNAME_ALREADY_EXISTS") false else it.nicknameAvailable) }
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            if (currentRevision == revision) {
                verification.invalidateProof()
                submissionError("가입 결과를 확인하지 못했어요. 로그인으로 확인하거나 다시 인증해 주세요", SignUpRecovery.CheckOutcome)
            }
            throw cancelled
        } finally {
            submitting = false
            if (currentRevision == revision) _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun prepareReverification() {
        if (submitting) return
        revision++
        verification.reset(_uiState.value.email)
        _uiState.update { it.copy(isOtpVerified = false, otp = "", submitError = null, recovery = SignUpRecovery.None) }
    }

    private fun submissionError(message: String, recovery: SignUpRecovery = SignUpRecovery.None) {
        _uiState.update { it.copy(submitError = message, recovery = recovery) }
    }

    private fun validNickname(nickname: String) = Regex("[가-힣a-zA-Z0-9_]{2,20}").matches(nickname)

    fun restartFromEmail() {
        revision++
        verification.reset(_uiState.value.email)
        _uiState.update { it.copy(otp = "", otpError = null, isOtpVerified = false,
            remainingOtpSeconds = 0, password = "", passwordConfirm = "", isAuthRequestInFlight = false) }
    }

    fun reset() {
        revision++
        verification.reset()
        _uiState.value = SignUpUiState()
    }
}
