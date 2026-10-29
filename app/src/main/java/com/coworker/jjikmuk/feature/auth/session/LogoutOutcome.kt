package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.AuthFailure

enum class ServerLogoutStatus { Confirmed, Unconfirmed, NoSession }

/** No credentials retained for retries. A late outcome is scoped to the logged-out generation. */
data class LogoutOutcome(
    val generation: Long,
    val localStorageCleared: Boolean,
    val serverStatus: ServerLogoutStatus,
    val failure: AuthFailure? = null,
)
