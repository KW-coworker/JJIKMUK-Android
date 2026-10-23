package com.coworker.jjikmuk.feature.auth.presentation.signup

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.EmailVerificationController
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()
    private val verification = EmailVerificationController(repository, EmailPurpose.SignUp, viewModelScope)
    private var revision = 0L
    private var nicknameRequest = false

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
        _uiState.value = SignUpUiState(email = email)
    }

    suspend fun validateEmail(): Boolean = verification.send()
    fun updateOtp(otp: String) = verification.updateCode(otp)
    suspend fun verifyOtp(): Boolean = verification.verify()
    suspend fun resendOtp(): Boolean = verification.send()
    // The final signup adapter must obtain this proof again immediately before submission.
    fun currentVerificationProof(): EmailProof? = verification.currentProof()

    fun updatePassword(password: String) { _uiState.update { it.copy(password = password) } }
    fun updatePasswordConfirm(password: String) { _uiState.update { it.copy(passwordConfirm = password) } }
    fun updateNickname(nickname: String) {
        revision++
        _uiState.update { it.copy(nickname = nickname, nicknameError = null, nicknameAvailable = false) }
    }

    suspend fun validateNickname(): Boolean {
        if (nicknameRequest) return false
        val nickname = _uiState.value.nickname.trim()
        if (nickname.isBlank()) return false
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
        _uiState.update { it.copy(vegetarianDiet = diet) }
    }

    fun updateAllergies(allergies: Set<String>) {
        _uiState.update { it.copy(allergies = allergies) }
    }

    fun toggleAllergy(allergy: String) {
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
        _uiState.update {
            it.copy(
                profilePhotoUri = uri,
                profilePhotoBitmap = bitmap,
            )
        }
    }


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
