package com.coworker.jjikmuk.feature.auth.presentation.common

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State contains no verification token; proofs stay in memory and are bound to email + purpose. */
data class EmailVerificationState(
    val email: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val sent: Boolean = false,
    val verified: Boolean = false,
    val remainingSeconds: Int = 0,
    val resendSeconds: Int = 0,
    val error: String? = null,
    val outcomeUnknown: Boolean = false,
)

/** Confined to the ViewModel's main dispatcher. Caller cancellation cancels the HTTP request. */
class EmailVerificationController(
    private val repository: AuthRepository,
    private val purpose: EmailPurpose,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutableState = MutableStateFlow(EmailVerificationState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var otpDeadline = 0L
    private var proofDeadline = 0L
    private var proof: EmailProof? = null
    private val cooldowns = mutableMapOf<String, Long>()
    private var ticker: Job? = null

    fun reset(email: String = "") {
        revision++
        ticker?.cancel()
        otpDeadline = 0
        proofDeadline = 0
        proof = null
        mutableState.value = EmailVerificationState(email = normalizeAuthEmail(email))
        tick()
        startTicker()
    }

    fun updateCode(code: String) {
        if (state.value.busy || state.value.verified) return
        mutableState.update { it.copy(code = code.filter { c -> c in '0'..'9' }.take(4), error = null) }
    }

    fun currentProof(): EmailProof? {
        tick()
        return proof?.takeIf { it.email == state.value.email && it.purpose == purpose && proofDeadline > nowMillis() }
    }

    suspend fun send(): Boolean {
        tick()
        val snapshot = state.value
        if (snapshot.busy) return false
        if (!EMAIL_PATTERN.matches(snapshot.email)) return fail("올바른 이메일 주소를 입력해 주세요")
        if (snapshot.resendSeconds > 0) return fail("${snapshot.resendSeconds}초 후 다시 요청해 주세요")
        val requestRevision = revision
        val startedAt = nowMillis()
        mutableState.update { it.copy(busy = true, error = null) }
        try {
            val result = repository.sendEmail(snapshot.email, purpose)
            if (requestRevision != revision) return false
            return when (result) {
                is AuthResult.Success -> {
                    proof = null
                    proofDeadline = 0
                    otpDeadline = startedAt + result.value.expiresInSeconds * 1000L
                    // The spec guarantees at least 60 seconds; server remains authoritative for 429.
                    cooldowns[snapshot.email] = nowMillis() + 60_000
                    mutableState.update { it.copy(code = "", sent = true, verified = false, outcomeUnknown = false) }
                    tick()
                    startTicker()
                    true
                }
                is AuthResult.Failure -> {
                    if (result.error.outcomeUnknown) markUnknown()
                    else {
                        if (result.error.status == 429) {
                            // Optional server delay, not a guessed limit parsed from message text.
                            val seconds = result.error.retryDelaySeconds ?: 60L
                            cooldowns[snapshot.email] = nowMillis() + seconds.coerceAtMost(Int.MAX_VALUE.toLong()) * 1000L
                        }
                        mutableState.update { it.copy(error = result.error.userMessage()) }
                    }
                    tick()
                    startTicker()
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            if (requestRevision == revision) { markUnknown(); startTicker() }
            throw cancelled
        } finally {
            if (requestRevision == revision) mutableState.update { it.copy(busy = false) }
        }
    }

    suspend fun verify(): Boolean {
        tick()
        val snapshot = state.value
        if (snapshot.busy || snapshot.verified) return false
        if (!snapshot.sent || snapshot.remainingSeconds == 0 || snapshot.outcomeUnknown) {
            return fail("인증번호를 다시 받아 주세요")
        }
        if (snapshot.code.length != 4) return fail("인증번호 4자리를 입력해 주세요")
        val requestRevision = revision
        val startedAt = nowMillis()
        mutableState.update { it.copy(busy = true, error = null) }
        try {
            val result = repository.verifyEmail(snapshot.email, purpose, snapshot.code)
            if (requestRevision != revision) return false
            return when (result) {
                is AuthResult.Success -> {
                    proof = result.value
                    proofDeadline = startedAt + result.value.expiresInSeconds * 1000L
                    otpDeadline = 0 // Server consumed this OTP.
                    mutableState.update { it.copy(code = "", verified = true, sent = false, remainingSeconds = 0) }
                    tick()
                    startTicker()
                    currentProof() != null
                }
                is AuthResult.Failure -> {
                    if (result.error.outcomeUnknown) markUnknown()
                    else {
                        if (result.error.code in setOf("OTP_EXPIRED", "OTP_NOT_REQUESTED", "OTP_RATE_LIMITED") ||
                            (result.error.code == "OTP_INVALID" && result.error.remainingAttempts == 0)) {
                            otpDeadline = 0
                            mutableState.update { it.copy(sent = false) }
                        }
                        mutableState.update { it.copy(error = result.error.userMessage()) }
                    }
                    tick()
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            if (requestRevision == revision) markUnknown()
            throw cancelled
        } finally {
            if (requestRevision == revision) mutableState.update { it.copy(busy = false) }
        }
    }

    fun invalidateProof() {
        proof = null
        proofDeadline = 0
        mutableState.update { it.copy(verified = false) }
    }

    private fun markUnknown() {
        invalidateProof()
        otpDeadline = 0
        cooldowns[state.value.email] = nowMillis() + 60_000
        mutableState.update { it.copy(sent = false, remainingSeconds = 0, outcomeUnknown = true,
            error = "처리 결과를 확인하지 못했어요. 잠시 후 인증번호를 다시 받아 주세요") }
    }

    private fun fail(message: String): Boolean {
        mutableState.update { it.copy(error = message) }
        return false
    }

    private fun secondsUntil(deadline: Long): Int = ((deadline - nowMillis()).coerceAtLeast(0) + 999).div(1000).toInt()
    private fun tick() {
        val expired = proof != null && proofDeadline <= nowMillis()
        if (expired) { proof = null; proofDeadline = 0 }
        mutableState.update { it.copy(
            remainingSeconds = secondsUntil(otpDeadline),
            resendSeconds = secondsUntil(cooldowns[it.email] ?: 0),
            verified = proof != null,
            error = if (expired) "이메일 인증이 만료됐어요. 다시 인증해 주세요" else it.error,
        ) }
    }
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            do {
                tick()
                delay(250)
            } while (state.value.remainingSeconds > 0 || state.value.resendSeconds > 0 || proof != null)
        }
    }
    companion object {
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
