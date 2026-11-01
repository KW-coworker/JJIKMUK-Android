package com.coworker.jjikmuk.data.remote.auth

import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** Read-only view of the real session; never keeps a second copy of the access token. */
@Singleton
class AccessTokenStore @Inject constructor(private val sessions: AuthSessionManager) {
    val token = sessions.state.map { currentToken() }.distinctUntilChanged()

    fun currentToken(): String? {
        val snapshot = sessions.state.value
        return snapshot.credentials?.accessToken?.takeIf { sessions.isCurrent(snapshot) }
    }
}
