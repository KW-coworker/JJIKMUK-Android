package com.coworker.jjikmuk.feature.auth.session

/** No generated toString/copy/componentN: a service token must not appear in state logs. */
class SessionCredentials(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val expiresAtMillis: Long,
    val profileCompleted: Boolean,
)

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
