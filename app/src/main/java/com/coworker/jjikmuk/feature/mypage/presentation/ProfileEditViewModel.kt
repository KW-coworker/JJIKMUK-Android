package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthProfiles
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// No credentials, password, photos or family data in an edit draft.
data class ProfileEditState(
    val nickname: String = "",
    val email: String = "",
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val mustReload: Boolean = false,
    val message: String? = null,
    val savedNickname: String? = null,
) {
    /** 입력한 닉네임이 저장된 닉네임과 다를 때만 저장 버튼을 보여 준다. */
    val hasNicknameChange: Boolean
        get() = loaded && savedNickname != null && normalizeAuthNickname(nickname) != savedNickname
}

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profiles: AuthProfiles,
    private val sessions: AuthSessionManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileEditState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = -1L
    private var revision = 0L
    private var profileVersion: Long? = null

    fun open() {
        close()
        generation = sessions.state.value.generation
        profileVersion = null
        mutableState.value = ProfileEditState()
        reload()
    }

    fun close() { revision++; request?.cancel(); request = null }

    fun updateNickname(value: String) {
        if (state.value.busy) return
        mutableState.update { it.copy(nickname = value, message = null) }
    }

    fun reload() {
        if (state.value.busy || sessions.state.value.generation != generation) return
        val keepDraft = state.value.loaded
        val currentRevision = revision
        mutableState.update { it.copy(busy = true, message = null) }
        request = viewModelScope.launch {
            val result = profiles.load()
            if (!isCurrent(currentRevision)) return@launch
            when (result) {
                is AuthResult.Success -> {
                    profileVersion = result.value.version
                    mutableState.update { it.copy(nickname = if (keepDraft) it.nickname else result.value.nickname,
                        savedNickname = result.value.nickname,
                        email = result.value.email, loaded = true, busy = false, mustReload = false,
                        message = if (keepDraft) "최신 정보를 확인했어요. 입력한 닉네임을 확인한 뒤 저장해 주세요" else null) }
                }
                is AuthResult.Failure -> mutableState.update { it.copy(busy = false, message = result.error.userMessage()) }
            }
        }
    }

    fun save() {
        val input = state.value
        if (!input.loaded || input.busy || input.mustReload || sessions.state.value.generation != generation) return
        val nickname = normalizeAuthNickname(input.nickname)
        if (!Regex("[가-힣a-zA-Z0-9_]{2,20}").matches(nickname)) {
            mutableState.update { it.copy(message = "닉네임은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요") }
            return
        }
        // 같은 닉네임이면 화면에 저장 버튼이 없으므로 여기서는 조용히 무시한다.
        if (nickname == input.savedNickname) return
        val currentRevision = revision
        mutableState.update { it.copy(busy = true, message = null) }
        request = viewModelScope.launch {
            when (val result = profiles.patch(ProfilePatch(nickname = nickname, version = profileVersion))) {
                is AuthResult.Success -> if (isCurrent(currentRevision)) {
                    profileVersion = result.value.version
                    mutableState.update { it.copy(nickname = result.value.nickname, savedNickname = result.value.nickname,
                        busy = false, message = "저장했어요") }
                }
                is AuthResult.Failure -> if (isCurrent(currentRevision)) {
                    val unknown = result.error.outcomeUnknown || result.error.code in setOf("AUTH_REFRESHED_RETRY_REQUIRED", "PROFILE_VERSION_CONFLICT")
                    mutableState.update { it.copy(busy = false, mustReload = unknown,
                        message = if (result.error.code == "PROFILE_VERSION_CONFLICT") "다른 기기에서 정보가 변경됐어요. 입력은 보관했으니 최신 정보를 확인한 뒤 다시 저장해 주세요."
                            else if (unknown) "저장 결과를 확인해야 해요. 최신 정보를 확인한 뒤 다시 저장해 주세요"
                            else result.error.userMessage()) }
                }
            }
        }
    }

    private fun isCurrent(currentRevision: Long) = currentRevision == revision &&
        sessions.state.value.generation == generation && sessions.state.value.credentials != null
}
