package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.isAuthPasswordValid
import com.coworker.jjikmuk.feature.auth.session.AuthAccountActions
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountAction { Password, Withdrawal }

data class AccountActionState(
    val action: AccountAction = AccountAction.Password,
    val currentPassword: String = "", val newPassword: String = "", val confirmation: String = "",
    val consented: Boolean = false, val busy: Boolean = false, val complete: Boolean = false,
    val unknown: Boolean = false, val message: String? = null, val errorField: String? = null,
    val available: Boolean = false, val hasPassword: Boolean = false,
    val storageWarning: Boolean = false, val ownerGeneration: Long? = null, val requiresLogin: Boolean = false,
) {
    val canSubmit: Boolean get() = available && !busy && !unknown && !complete && !requiresLogin &&
        if (action == AccountAction.Withdrawal) consented
        else hasPassword && currentPassword.isNotEmpty() && isAuthPasswordValid(newPassword, confirmation)
    // Passwords stay in memory only and must never enter state logs.
    override fun toString() = "AccountActionState(action=$action, busy=$busy, complete=$complete, unknown=$unknown)"
}

@HiltViewModel
class AccountActionViewModel @Inject constructor(
    private val actions: AuthAccountActions,
    private val sessions: AuthSessionManager,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(AccountActionState())
    val state = mutable.asStateFlow()
    val session = sessions.state
    private var generation: Long? = null

    fun open(action: AccountAction) {
        val snapshot = session.value
        if (generation == snapshot.generation && mutable.value.action == action) return
        if (mutable.value.busy) return
        generation = snapshot.generation
        // Only the operation kind and owner are restored, never passwords or an automatic retry.
        val unresolved = saved.get<Long>(OWNER) == snapshot.credentials?.userId && saved.get<String>(PENDING) != null
        mutable.value = AccountActionState(action = action, ownerGeneration = snapshot.generation, available = snapshot.canEnterHome && snapshot.profile != null,
            hasPassword = snapshot.profile?.hasPassword == true, unknown = unresolved,
            message = if (unresolved) UNKNOWN_MESSAGE else null)
    }

    fun editCurrent(value: String) = edit { copy(currentPassword = value) }
    fun editNew(value: String) = edit { copy(newPassword = value) }
    fun editConfirmation(value: String) = edit { copy(confirmation = value) }
    fun consent(value: Boolean) = edit { copy(consented = value) }
    private fun edit(change: AccountActionState.() -> AccountActionState) {
        if (!mutable.value.busy && !mutable.value.unknown && !mutable.value.complete)
            mutable.update { it.change().copy(message = null, errorField = null) }
    }

    fun submit() {
        val input = mutable.value
        val expected = generation ?: return
        val snapshot = session.value
        if (!input.canSubmit || snapshot.generation != expected || !snapshot.canEnterHome) return
        // Mark synchronously, before launching, to block a second tap and interrupted restoration.
        saved[OWNER] = snapshot.credentials!!.userId
        saved[PENDING] = input.action.name
        mutable.update { it.copy(busy = true, message = null, errorField = null) }
        viewModelScope.launch {
            try {
                val result = when (input.action) {
                    AccountAction.Password -> actions.changePassword(input.currentPassword, input.newPassword, expected)
                    AccountAction.Withdrawal -> actions.deleteAccount(expected)
                }
                when (result) {
                    is AuthResult.Success -> {
                        clearPending()
                        val current = session.value
                        // Success clears exactly this session; do not display an old account's result on a new one.
                        val isOwnCompletion = current.generation == expected + 1 && current.credentials == null
                        mutable.value = AccountActionState(action = input.action, complete = isOwnCompletion,
                            storageWarning = isOwnCompletion && current.storageFailed)
                    }
                    is AuthResult.Failure -> {
                        val error = result.error
                        if (!error.outcomeUnknown) clearPending()
                        val sameAccount = session.value.generation == expected && session.value.canEnterHome
                        mutable.update { it.copy(busy = false, available = sameAccount,
                            unknown = error.outcomeUnknown, requiresLogin = error.code == "USER_NOT_FOUND",
                            message = error.messageForAction(), errorField = error.field,
                            hasPassword = it.hasPassword && error.code != "AUTH_METHOD_NOT_SUPPORTED",
                            currentPassword = if (sameAccount) it.currentPassword else "",
                            newPassword = if (sameAccount) it.newPassword else "",
                            confirmation = if (sameAccount) it.confirmation else "") }
                    }
                }
            } catch (cancelled: CancellationException) {
                mutable.update { it.copy(busy = false, unknown = true, message = UNKNOWN_MESSAGE,
                    currentPassword = "", newPassword = "", confirmation = "") }
                throw cancelled
            }
        }
    }

    fun leave() {
        if (!mutable.value.busy) mutable.update { it.copy(currentPassword = "", newPassword = "", confirmation = "") }
    }

    /** Explicit user recovery only; a network failure by itself never signs out. */
    suspend fun reauthenticate() {
        generation?.let { sessions.clearIfCurrent(it) }
        clearPending()
        mutable.value = AccountActionState()
        generation = null
    }

    private fun clearPending() { saved.remove<String>(PENDING); saved.remove<Long>(OWNER) }

    private fun AuthFailure.messageForAction(): String = when {
        outcomeUnknown -> UNKNOWN_MESSAGE
        kind == AuthFailureKind.NotConfigured -> "인증 서버 설정이 필요합니다. 개발 환경을 확인해 주세요."
        code == "LOGIN_FAILED" -> "현재 비밀번호가 올바르지 않습니다. 다시 확인해 주세요."
        code == "PASSWORD_INVALID" -> "새 비밀번호 조건을 확인해 주세요. 영문·숫자를 포함해 8자 이상, 72바이트 이하로 입력해 주세요."
        code == "AUTH_METHOD_NOT_SUPPORTED" -> "Google로 로그인한 계정입니다. 비밀번호 변경을 사용할 수 없습니다."
        code == "ACCESS_DENIED" -> "이 계정으로 요청을 처리할 수 없습니다."
        code == "AUTH_REFRESHED_RETRY_REQUIRED" -> "로그인 정보가 갱신되었습니다. 내용을 확인한 뒤 다시 눌러 주세요."
        code == "VALIDATION_FAILED" -> "입력 내용을 확인해 주세요."
        code == "USER_NOT_FOUND" -> "계정 정보를 확인할 수 없습니다. 다시 로그인해 주세요."
        else -> "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
    }

    companion object {
        private const val PENDING = "account_action_pending"
        private const val OWNER = "account_action_owner"
        const val UNKNOWN_MESSAGE = "서버의 처리 결과를 확인하지 못했습니다. 같은 요청을 반복하지 말고 다시 로그인해 계정 상태를 확인해 주세요."
    }
}
