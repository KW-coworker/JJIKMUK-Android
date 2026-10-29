package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.AuthProvider
import com.coworker.jjikmuk.feature.auth.domain.RefreshCredentials

/** No generated toString/copy/componentN: a service token must not appear in state logs. */
class SessionCredentials(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val expiresAtMillis: Long,
    val profileCompleted: Boolean,
    val refresh: RefreshCredentials? = null,
    val authProvider: AuthProvider? = null,
    val hasPassword: Boolean? = null,
) {
    // Legacy Access-only sessions never acquire an invented Refresh token or expiry.
    internal fun validate() {
        require(accessToken.isNotBlank() && userId > 0 && email.isNotBlank() && expiresAtMillis > 0)
        if (refresh == null) {
            require(authProvider == null && hasPassword == null)
        } else {
            require(refresh.token.isNotBlank() && refresh.expiresAtMillis > 0)
            requireNotNull(authProvider)
            require(hasPassword == (authProvider != AuthProvider.GOOGLE))
        }
    }

    fun hasUsableRefresh(nowMillis: Long): Boolean = (refresh?.expiresAtMillis ?: 0) > nowMillis
}

interface SessionStore {
    suspend fun read(): SessionCredentials?
    suspend fun write(credentials: SessionCredentials)
    suspend fun clear()
}

class SessionSnapshot(
    val generation: Long = 0,
    val credentials: SessionCredentials? = null,
    // Restored credentials await GET /users/me. Local persistence alone never authorizes Home.
    val validatedThisRun: Boolean = false,
    val storageFailed: Boolean = false,
) {
    val canEnterHome: Boolean get() = validatedThisRun && credentials?.profileCompleted == true
}
