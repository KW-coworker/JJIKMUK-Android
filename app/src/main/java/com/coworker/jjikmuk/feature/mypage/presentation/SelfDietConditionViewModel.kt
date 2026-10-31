package com.coworker.jjikmuk.feature.mypage.presentation

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

data class SelfConditions(
    val allergies: Set<String> = emptySet(),
    val vegetarian: VegetarianDiet? = null,
    val nutrition: Set<SignUpCondition> = emptySet(),
) {
    internal fun encode() = SignUpConditionMapping.encode(SignUpUiState(
        allergies = allergies, vegetarianDiet = vegetarian,
        selectedConditions = nutrition + (if (allergies.isNotEmpty()) setOf(SignUpCondition.Allergy) else emptySet()) +
            (if (vegetarian != null) setOf(SignUpCondition.Vegetarian) else emptySet()),
    ))
    companion object {
        internal fun decode(profile: AuthProfile): SelfConditions? =
            SignUpConditionMapping.decode(profile.allergies, profile.specialDiet)?.let {
                SelfConditions(it.allergies, it.vegetarianDiet,
                    it.selectedConditions - setOf(SignUpCondition.Allergy, SignUpCondition.Vegetarian))
            }
    }
}

data class SelfDietConditionState(
    val nickname: String = "", val draft: SelfConditions = SelfConditions(),
    val saved: SelfConditions? = null, val busy: Boolean = false,
    val needsReadback: Boolean = false, val unsupported: Boolean = false, val message: String? = null,
) {
    val editable get() = saved != null && !busy && !needsReadback && !unsupported
    val hasChanges get() = saved != null && draft != saved
    val canSave get() = editable && hasChanges
}

/** Own profile only. Never reads/writes ownerless Room family data. Drafts stay in memory. */
@HiltViewModel
class SelfDietConditionViewModel @Inject constructor(
    private val profiles: AuthProfiles, private val sessions: AuthSessionManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(SelfDietConditionState())
    val state = mutable.asStateFlow()
    private var owner: Long? = null
    private var revision = 0L

    init {
        viewModelScope.launch { sessions.state.collect { if (owner != null && owner != it.generation) leave() } }
    }

    fun open() {
        val session = sessions.state.value
        if (owner == session.generation && session.credentials != null) return
        if (!session.canEnterHome) { leave(); mutable.update { it.copy(message = "로그인 상태를 확인해 주세요.") }; return }
        leave(); owner = session.generation; reload()
    }

    fun leave() { revision++; owner = null; mutable.value = SelfDietConditionState() }
    private fun current(generation: Long, ticket: Long) = owner == generation && revision == ticket &&
        sessions.state.value.generation == generation && sessions.state.value.canEnterHome

    fun allergy(id: String) = edit { it.copy(allergies = if (id in it.allergies) it.allergies - id else it.allergies + id) }
    fun vegetarian(value: VegetarianDiet?) = edit { it.copy(vegetarian = if (it.vegetarian == value) null else value) }
    fun nutrition(value: SignUpCondition) = edit {
        it.copy(nutrition = if (value in it.nutrition) it.nutrition - value else it.nutrition + value)
    }
    private fun edit(change: (SelfConditions) -> SelfConditions) {
        if (state.value.editable && owner == sessions.state.value.generation) {
            val next = change(state.value.draft)
            if (next.encode() != null) mutable.update { it.copy(draft = next, message = null) }
        }
    }

    fun reload() {
        val generation = owner ?: return
        val ticket = revision
        if (state.value.busy || !current(generation, ticket)) return
        val before = state.value
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val result = profiles.load()
                if (!current(generation, ticket)) return@launch
                when (result) {
                    is AuthResult.Success -> {
                        val server = SelfConditions.decode(result.value)
                        if (server == null) unsupported(result.value.nickname)
                        else {
                            // Preserve only edited field groups; adopt newer values for untouched fields.
                            val previous = before.saved
                            val draft = if (previous == null) server else server.copy(
                                allergies = if (before.draft.allergies != previous.allergies) before.draft.allergies else server.allergies,
                                vegetarian = if (before.draft.vegetarian != previous.vegetarian || before.draft.nutrition != previous.nutrition)
                                    before.draft.vegetarian else server.vegetarian,
                                nutrition = if (before.draft.vegetarian != previous.vegetarian || before.draft.nutrition != previous.nutrition)
                                    before.draft.nutrition else server.nutrition,
                            )
                            mutable.value = SelfDietConditionState(result.value.nickname, draft, server,
                                message = if (previous == null) null else if (draft == server) "서버에 저장된 조건을 확인했어요."
                                    else "최신 정보를 확인했어요. 선택한 조건을 확인한 뒤 저장해 주세요.")
                        }
                    }
                    is AuthResult.Failure -> mutable.update { it.copy(busy = false, message = result.error.userMessage()) }
                }
            } finally { if (current(generation, ticket)) mutable.update { it.copy(busy = false) } }
        }
    }

    fun save() {
        val generation = owner ?: return
        val ticket = revision
        val input = state.value
        if (!input.canSave || !current(generation, ticket)) return
        val encoded = input.draft.encode() ?: return
        val saved = input.saved ?: return
        fun field(changed: Boolean, value: String?): ProfileField = when {
            !changed -> ProfileField.Unchanged
            value == null -> ProfileField.Clear
            else -> ProfileField.Value(value)
        }
        val patch = ProfilePatch(
            allergies = field(input.draft.allergies != saved.allergies, encoded.allergies),
            specialDiet = field(input.draft.vegetarian != saved.vegetarian || input.draft.nutrition != saved.nutrition, encoded.specialDiet),
        )
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val result = profiles.patch(patch, generation)
                if (!current(generation, ticket)) return@launch
                when (result) {
                    is AuthResult.Success -> {
                        val server = SelfConditions.decode(result.value)
                        if (server == null) unsupported(result.value.nickname)
                        else mutable.value = SelfDietConditionState(result.value.nickname, server, server, message = "식이조건을 저장했어요.")
                    }
                    is AuthResult.Failure -> {
                        val uncertain = result.error.outcomeUnknown || result.error.code == "AUTH_REFRESHED_RETRY_REQUIRED"
                        mutable.update { it.copy(busy = false, needsReadback = uncertain,
                            message = if (uncertain) "저장 결과를 확인해야 해요. 서버 정보를 다시 확인해 주세요." else result.error.userMessage()) }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (current(generation, ticket)) mutable.update { it.copy(busy = false, needsReadback = true,
                    message = "저장 결과를 확인해야 해요. 서버 정보를 다시 확인해 주세요.") }
                throw cancelled
            }
        }
    }

    private fun unsupported(nickname: String) {
        mutable.update { it.copy(nickname = nickname, busy = false, unsupported = true, needsReadback = true,
            message = "현재 앱에서 표시할 수 없는 조건이 있어요. 기존 정보를 지우지 않도록 수정을 중단했어요. 앱과 서버의 조건 목록을 확인해 주세요.") }
    }
}
