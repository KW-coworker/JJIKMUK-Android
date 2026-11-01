package com.coworker.jjikmuk.feature.auth.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.presentation.signup.*
import com.coworker.jjikmuk.feature.auth.session.AuthProfiles
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfilePage { Nickname, Conditions, Allergies, Vegetarian, Review }
data class ProfileCompletionState(
    val form: SignUpUiState = SignUpUiState(), val page: ProfilePage = ProfilePage.Nickname,
    val loaded: Boolean = false, val loading: Boolean = false, val needsReadback: Boolean = false,
    val loadError: String? = null,
) {
    val busy get() = loading || form.isSubmitting
    val editable get() = loaded && !busy && !needsReadback
}

/** Reuses signup presentation only: no OTP, password, signup request or synthetic session. */
@HiltViewModel
class ProfileCompletionViewModel @Inject constructor(
    private val repository: AuthRepository, private val profiles: AuthProfiles,
    private val sessions: AuthSessionManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileCompletionState())
    val state = mutable.asStateFlow()
    val session = sessions.state
    private var owner: Long? = null
    private var original: AuthProfile? = null
    private var revision = 0L

    init {
        viewModelScope.launch {
            session.collect { snapshot ->
                if (owner != null && owner != snapshot.generation) clearDraft()
            }
        }
    }

    fun open() {
        val snapshot = session.value
        if (snapshot.credentials == null) { clearDraft(); return }
        if (owner == snapshot.generation) return
        clearDraft()
        owner = snapshot.generation
        if (snapshot.profile != null) adopt(snapshot.profile) else reload()
    }

    fun clearDraft() {
        revision++; owner = null; original = null
        mutable.value = ProfileCompletionState()
    }

    private fun current(generation: Long) = owner == generation && session.value.generation == generation && session.value.credentials != null

    private fun adopt(profile: AuthProfile) {
        original = profile
        val decoded = SignUpConditionMapping.decode(profile.allergies, profile.specialDiet)
        mutable.value = if (decoded == null) ProfileCompletionState(loadError =
            "현재 앱에서 표시할 수 없는 조건이 있습니다. 정보를 지우지 않도록 제출을 중단했어요. 앱과 서버의 조건 목록을 확인해 주세요.")
        else ProfileCompletionState(form = decoded.copy(nickname = profile.nickname), loaded = true)
    }

    fun nickname(value: String) {
        if (!state.value.editable) return
        revision++
        mutable.update { it.copy(form = it.form.copy(nickname = value, nicknameAvailable = false,
            nicknameError = null, submitError = null)) }
    }

    fun nextNickname() {
        val state = state.value
        val generation = owner ?: return
        if (!state.editable || state.form.isNicknameChecking || state.page != ProfilePage.Nickname) return
        val nickname = normalizeAuthNickname(state.form.nickname)
        if (!validNickname(nickname)) {
            mutable.update { it.copy(form = it.form.copy(nicknameError = NICKNAME_RULE)) }; return
        }
        val version = revision
        mutable.update { it.copy(form = it.form.copy(isNicknameChecking = true, nicknameError = null)) }
        viewModelScope.launch {
            try {
                // Public availability excludes even the owner's current nickname. Final complete still checks uniqueness.
                val sameName = nickname.equals(normalizeAuthNickname(original?.nickname.orEmpty()), ignoreCase = true)
                val result = if (sameName) AuthResult.Success(true) else profiles.nicknameAvailable(nickname)
                if (!current(generation) || revision != version) return@launch
                when (result) {
                    is AuthResult.Success -> mutable.update { it.copy(
                        page = if (result.value) ProfilePage.Conditions else ProfilePage.Nickname,
                        form = it.form.copy(nickname = nickname, nicknameAvailable = result.value,
                            nicknameError = if (result.value) null else "이미 사용 중인 닉네임이에요")) }
                    is AuthResult.Failure -> mutable.update { it.copy(form = it.form.copy(nicknameError = result.error.userMessage())) }
                }
            } finally {
                if (current(generation)) mutable.update { it.copy(form = it.form.copy(isNicknameChecking = false)) }
            }
        }
    }

    fun condition(condition: SignUpCondition) = edit { form ->
        val selected = if (condition in form.selectedConditions) form.selectedConditions - condition else form.selectedConditions + condition
        form.copy(selectedConditions = selected,
            allergies = if (SignUpCondition.Allergy in selected) form.allergies else emptySet(),
            vegetarianDiet = if (SignUpCondition.Vegetarian in selected) form.vegetarianDiet else null)
    }
    fun allergy(id: String) = edit { it.copy(allergies = if (id in it.allergies) it.allergies - id else it.allergies + id) }
    fun vegetarian(diet: VegetarianDiet) = edit { it.copy(vegetarianDiet = diet) }
    private fun edit(change: (SignUpUiState) -> SignUpUiState) {
        if (state.value.editable) { revision++; mutable.update { it.copy(form = change(it.form).copy(submitError = null)) } }
    }

    fun nextConditions() = page(when {
        state.value.form.hasAllergyCondition -> ProfilePage.Allergies
        state.value.form.hasVegetarianCondition -> ProfilePage.Vegetarian
        else -> ProfilePage.Review
    })
    fun nextAllergies() = page(if (state.value.form.hasVegetarianCondition) ProfilePage.Vegetarian else ProfilePage.Review)
    fun review() = page(ProfilePage.Review)
    fun editConditions() = page(ProfilePage.Conditions)
    fun back() = page(when (state.value.page) {
        ProfilePage.Nickname, ProfilePage.Conditions -> ProfilePage.Nickname
        ProfilePage.Allergies -> ProfilePage.Conditions
        ProfilePage.Vegetarian -> if (state.value.form.hasAllergyCondition) ProfilePage.Allergies else ProfilePage.Conditions
        ProfilePage.Review -> ProfilePage.Conditions
    })
    private fun page(page: ProfilePage) {
        if (state.value.editable) { revision++; mutable.update { it.copy(page = page) } }
    }

    fun submit() {
        val input = state.value
        val generation = owner ?: return
        val previous = original ?: return
        if (!current(generation) || !input.editable || input.page != ProfilePage.Review) return
        val name = normalizeAuthNickname(input.form.nickname)
        if (!validNickname(name)) {
            mutable.update { it.copy(form = it.form.copy(nicknameError = NICKNAME_RULE)) }; return
        }
        val conditions = SignUpConditionMapping.encode(input.form)
        if (conditions == null) {
            mutable.update { it.copy(form = it.form.copy(submitError = "선택한 조건을 다시 확인해 주세요.")) }; return
        }
        mutable.update { it.copy(form = it.form.copy(isSubmitting = true, submitError = null, nicknameError = null)) }
        viewModelScope.launch {
            try {
                // Completion is a full replacement. Preserve the server fields this form does not edit.
                val result = profiles.complete(ProfileSubmission(name, conditions.allergies, conditions.specialDiet,
                    previous.diseases, previous.dislikedIngredients), generation)
                if (!current(generation)) return@launch
                when (result) {
                    is AuthResult.Success -> mutable.update { it.copy(form = it.form.copy(isSubmitting = false)) }
                    is AuthResult.Failure -> {
                        val error = result.error
                        val readback = error.outcomeUnknown || error.code == "AUTH_REFRESHED_RETRY_REQUIRED"
                        mutable.update { it.copy(needsReadback = readback, form = it.form.copy(isSubmitting = false,
                            nicknameAvailable = if (error.code == "NICKNAME_ALREADY_EXISTS") false else it.form.nicknameAvailable,
                            nicknameError = if (error.code in setOf("NICKNAME_ALREADY_EXISTS", "NICKNAME_INVALID"))
                                if (error.code == "NICKNAME_ALREADY_EXISTS") "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요." else NICKNAME_RULE else null,
                            submitError = if (readback) "처리 결과를 확인해야 합니다. 완료 상태를 다시 확인해 주세요."
                                else if (error.code in setOf("NICKNAME_ALREADY_EXISTS", "NICKNAME_INVALID")) null else error.userMessage())) }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (current(generation)) mutable.update { it.copy(needsReadback = true,
                    form = it.form.copy(isSubmitting = false, submitError = "처리 결과를 다시 확인해 주세요.")) }
                throw cancelled
            }
        }
    }

    /** Safe read only; a failed completion is never automatically replayed. */
    fun reload() {
        val generation = owner ?: return
        if (state.value.busy || !current(generation)) return
        val preserveDraft = state.value.loaded
        mutable.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val result = profiles.load()
                if (!current(generation)) return@launch
                when (result) {
                    is AuthResult.Success -> {
                        original = result.value
                        if (!preserveDraft) adopt(result.value)
                        else if (!result.value.profileCompleted && SignUpConditionMapping.decode(result.value.allergies, result.value.specialDiet) == null)
                            mutable.update { it.copy(loading = false, needsReadback = true, form = it.form.copy(
                                submitError = "서버 조건 목록이 변경되었습니다. 표시할 수 없는 정보를 지우지 않도록 제출을 중단했어요.")) }
                        else mutable.update { it.copy(loading = false, needsReadback = false,
                            form = it.form.copy(submitError = if (result.value.profileCompleted) null
                                else "아직 완료되지 않았어요. 입력 내용을 확인한 뒤 완료해 주세요.")) }
                    }
                    is AuthResult.Failure -> mutable.update { it.copy(loading = false,
                        loadError = result.error.userMessage(), form = it.form.copy(submitError = result.error.userMessage())) }
                }
            } finally {
                if (current(generation)) mutable.update { it.copy(loading = false) }
            }
        }
    }

    private fun validNickname(value: String) = Regex("[가-힣a-zA-Z0-9_]{2,20}").matches(value)
    companion object { private const val NICKNAME_RULE = "닉네임은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요" }
}
