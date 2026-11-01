package com.coworker.jjikmuk.feature.auth.presentation.login

import com.coworker.jjikmuk.feature.auth.domain.AuthFailure

/** App-wide deadline: changing email/screens must not bypass a server IP/account cooldown. */
class LoginRetryGate(
    private val readDeadline: () -> Long = { 0L },
    private val writeDeadline: (Long) -> Unit = {},
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var deadline = readDeadline()
    fun remainingSeconds(): Int {
        val remaining = (deadline - nowMillis()).coerceAtLeast(0)
        return (remaining / 1000 + if (remaining % 1000 > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun limit(error: AuthFailure) {
        val seconds = error.retryAfterSeconds ?: error.retryAfterHeaderSeconds ?: return
        deadline = maxOf(deadline, nowMillis() + seconds.coerceIn(0, Int.MAX_VALUE.toLong()) * 1000L)
        writeDeadline(deadline)
    }
}
