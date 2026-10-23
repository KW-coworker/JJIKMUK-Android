package com.coworker.jjikmuk.feature.auth.session

import java.io.IOException

class MemorySessionStore : SessionStore {
    var saved: SessionCredentials? = null
    var failWrite = false
    override suspend fun read() = saved
    override suspend fun write(credentials: SessionCredentials) {
        if (failWrite) throw IOException("Test storage failure")
        saved = credentials
    }
    override suspend fun clear() { saved = null }
}
