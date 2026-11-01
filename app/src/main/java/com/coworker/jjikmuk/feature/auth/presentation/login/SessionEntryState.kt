package com.coworker.jjikmuk.feature.auth.presentation.login

import com.coworker.jjikmuk.feature.auth.domain.AuthFailure

sealed interface SessionEntryState {
    data object Checking : SessionEntryState
    data object SignedOut : SessionEntryState
    data object Ready : SessionEntryState
    data object ProfileRequired : SessionEntryState
    data class Retry(val failure: AuthFailure) : SessionEntryState
}
