package com.coworker.jjikmuk.feature.auth.google

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GooglePhase { Idle, Choosing, SigningIn, LinkRequired, Linking, Cancelling, Finished, Exit }
enum class GoogleExit { Login, PasswordReset }
data class GoogleLoginState(
    val phase: GooglePhase = GooglePhase.Idle, val started: Boolean = false,
    val password: String = "", val message: String? = null, val remainingSeconds: Int = 0,
    val remainingAttempts: Int? = null, val linked: Boolean = false, val exit: GoogleExit = GoogleExit.Login,
) {
    val busy get() = phase in setOf(GooglePhase.Choosing, GooglePhase.SigningIn, GooglePhase.Linking, GooglePhase.Cancelling)
    override fun toString() = "GoogleLoginState(phase=$phase)"
}

@HiltViewModel
class GoogleLoginViewModel(
    private val repository: AuthRepository, private val sessions: AuthSessionManager,
    private val wallClock: () -> Long, private val elapsedClock: () -> Long,
) : ViewModel() {
    @Inject constructor(repository: AuthRepository, sessions: AuthSessionManager) :
        this(repository, sessions, System::currentTimeMillis, SystemClock::elapsedRealtime)

    private class Proof(val token: String, val deadline: Long)
    private var proof: Proof? = null
    private var revision = 0L
    private var generation: Long? = null
    private val mutable = MutableStateFlow(GoogleLoginState())
    val state = mutable.asStateFlow()
    val session = sessions.state

    /** Called only by the login button, not when a screen is recreated. */
    fun prepareEntry() {
        if (state.value.phase in setOf(GooglePhase.Exit, GooglePhase.Finished)) {
            revision++; proof = null; generation = null; mutable.value = GoogleLoginState()
        }
    }

    fun beginPicking(): Long? {
        if (state.value.busy || proof != null || sessions.state.value.credentials != null) return null
        generation = sessions.state.value.generation
        mutable.value = GoogleLoginState(phase = GooglePhase.Choosing, started = true)
        return ++revision
    }

    fun pickerCancelled(ticket: Long) {
        if (revision == ticket && state.value.phase == GooglePhase.Choosing)
            mutable.value = GoogleLoginState(started = true, message = "Google 로그인을 취소했어요. 다시 시도할 수 있어요.")
    }

    fun receiveCredential(ticket: Long, result: GoogleCredentialResult) {
        if (revision != ticket || state.value.phase != GooglePhase.Choosing) return
        if (!current()) { restart("로그인 상태가 변경되었어요. 다시 시작해 주세요."); return }
        when (result) {
            GoogleCredentialResult.Cancelled -> pickerCancelled(ticket)
            is GoogleCredentialResult.Failure -> mutable.value = GoogleLoginState(started = true, message = result.message)
            is GoogleCredentialResult.Token -> {
                if (result.value.isBlank()) { restart("Google 로그인 정보를 확인하지 못했어요."); return }
                mutable.update { it.copy(phase = GooglePhase.SigningIn) }
                viewModelScope.launch {
                    val startedAt = wallClock()
                    try {
                        if (revision != ticket || !current()) return@launch
                        val attempt = sessions.beginLoginIfCurrent(generation ?: return@launch)
                        if (attempt == null) { if (revision == ticket) restart("로그인 상태가 변경되었어요."); return@launch }
                        if (revision != ticket) { sessions.clearIfCurrent(attempt); return@launch }
                        generation = attempt
                        val response = repository.google(result.value)
                        if (revision != ticket || !current()) {
                            ((response as? AuthResult.Success)?.value as? GoogleAuthResult.LinkRequired)?.let { discardProof(it.linkToken) }
                            if (revision == ticket) restart("로그인 상태가 변경되었어요. 다시 시작해 주세요.")
                            return@launch
                        }
                        when (response) {
                            is AuthResult.Success -> when (val google = response.value) {
                                is GoogleAuthResult.SignedIn -> accept(google.login, startedAt, ticket, linked = false)
                                is GoogleAuthResult.LinkRequired -> {
                                    val ttlDeadline = runCatching { Math.addExact(startedAt, Math.multiplyExact(google.expiresInSeconds, 1000L)) }.getOrNull()
                                    val remaining = ttlDeadline?.let { minOf(it, google.expiresAtMillis) - wallClock() } ?: 0
                                    if (remaining <= 0 || remaining > Long.MAX_VALUE - elapsedClock()) {
                                        discardProof(google.linkToken); restart("계정 연결 시간이 만료됐어요. Google 로그인을 다시 진행해 주세요.")
                                    } else {
                                        proof = Proof(google.linkToken, elapsedClock() + remaining)
                                        mutable.value = GoogleLoginState(phase = GooglePhase.LinkRequired, started = true)
                                        tick()
                                    }
                                }
                            }
                            is AuthResult.Failure -> restart(when {
                                response.error.code == "GOOGLE_LOGIN_UNAVAILABLE" -> response.error.userMessage()
                                response.error.outcomeUnknown -> "로그인 결과를 확인하지 못했어요. Google 계정을 다시 선택해 로그인해 주세요."
                                else -> response.error.userMessage()
                            })
                        }
                    } catch (cancelled: CancellationException) {
                        if (revision == ticket) restart("로그인 결과를 확인하지 못했어요. Google 로그인을 다시 진행해 주세요.")
                        throw cancelled
                    }
                }
            }
        }
    }

    fun password(value: String) {
        if (state.value.phase == GooglePhase.LinkRequired && current()) mutable.update { it.copy(password = value, message = null) }
    }

    fun tick() {
        val pending = proof ?: return
        if (state.value.phase != GooglePhase.LinkRequired) return
        if (!current()) { discardProof(pending.token); restart("로그인 상태가 변경되었어요. 다시 시작해 주세요."); return }
        val millis = (pending.deadline - elapsedClock()).coerceAtLeast(0)
        if (millis == 0L) {
            discardProof(pending.token); restart("계정 연결 시간이 만료됐어요. Google 로그인을 다시 진행해 주세요.")
        } else mutable.update { it.copy(remainingSeconds = (millis / 1000 + if (millis % 1000 > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
    }

    fun link() {
        tick()
        val pending = proof ?: return
        val input = state.value
        if (input.phase != GooglePhase.LinkRequired || input.password.isEmpty() || !current()) return
        val ticket = revision
        mutable.update { it.copy(phase = GooglePhase.Linking, message = null) }
        viewModelScope.launch {
            val startedAt = wallClock()
            try {
                val response = repository.linkGoogle(pending.token, input.password)
                if (revision != ticket || !current()) {
                    if (revision == ticket) { discardProof(pending.token); restart("로그인 상태가 변경되었어요. 다시 시작해 주세요.") }
                    return@launch
                }
                when (response) {
                    is AuthResult.Success -> accept(response.value, startedAt, ticket, linked = true)
                    is AuthResult.Failure -> {
                        val error = response.error
                        if (error.code == "GOOGLE_LINK_PASSWORD_INVALID" && error.status == 400 && error.remainingAttempts != 0) {
                            mutable.update { it.copy(phase = GooglePhase.LinkRequired, message = "기존 계정의 비밀번호가 올바르지 않습니다.",
                                remainingAttempts = error.remainingAttempts) }; tick()
                        } else {
                            discardProof(pending.token)
                            restart(when {
                                error.outcomeUnknown -> "연결 결과를 확인하지 못했어요. 같은 비밀번호 요청을 반복하지 말고 Google 로그인을 새로 진행해 주세요."
                                error.code == "GOOGLE_LINK_EXPIRED" || error.remainingAttempts == 0 -> "계정 연결이 만료되었거나 시도 횟수를 초과했어요. Google 로그인을 다시 진행해 주세요."
                                error.code == "GOOGLE_ACCOUNT_ALREADY_LINKED" -> "이미 다른 Google 계정이 연결되어 있어요. 기존 로그인 방식을 이용해 주세요."
                                else -> error.userMessage()
                            })
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (revision == ticket) { discardProof(pending.token); restart("연결 결과를 확인하지 못했어요. Google 로그인을 새로 진행해 주세요.") }
                throw cancelled
            }
        }
    }

    /** Stop locally first. Cancellation is a single best-effort API call, never a login. */
    fun exit(destination: GoogleExit = GoogleExit.Login) {
        if (state.value.phase in setOf(GooglePhase.Cancelling, GooglePhase.Exit, GooglePhase.Finished)) return
        val token = proof?.token
        proof = null
        val ticket = ++revision
        val oldGeneration = generation
        mutable.value = GoogleLoginState(phase = GooglePhase.Cancelling, started = true, exit = destination)
        viewModelScope.launch {
            // Invalidate any late authentication response, without clearing a newer account.
            oldGeneration?.let { sessions.clearIfCurrent(it) }
            val response = token?.let { repository.cancelGoogleLink(it) }
            if (revision == ticket) mutable.value = GoogleLoginState(phase = GooglePhase.Exit, started = true, exit = destination,
                message = if (response is AuthResult.Failure) "기기에서 연결을 중단했어요. 서버의 연결 증명 취소 여부는 확인하지 못했어요." else null)
        }
    }

    private suspend fun accept(login: AuthLogin, startedAt: Long, ticket: Long, linked: Boolean) {
        val attempt = generation ?: return
        if (revision != ticket || !current()) return
        val accepted = sessions.acceptLogin(attempt, login, startedAt)
        if (revision != ticket) { sessions.clearIfCurrent(attempt); return }
        proof = null
        mutable.value = if (accepted) GoogleLoginState(phase = GooglePhase.Finished, started = true, linked = linked)
        else GoogleLoginState(started = true, message = "로그인 정보를 저장하지 못했어요. Google 로그인을 다시 진행해 주세요.")
    }

    private fun current() = generation == sessions.state.value.generation
    private fun restart(message: String) { proof = null; mutable.value = GoogleLoginState(started = true, message = message) }
    private fun discardProof(token: String) { viewModelScope.launch { repository.cancelGoogleLink(token) } }
}
