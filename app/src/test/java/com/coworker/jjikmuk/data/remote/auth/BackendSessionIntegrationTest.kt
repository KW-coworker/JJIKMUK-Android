package com.coworker.jjikmuk.data.remote.auth

import com.coworker.jjikmuk.feature.auth.domain.AuthLogin
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import com.coworker.jjikmuk.feature.auth.session.MemorySessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test

class BackendSessionIntegrationTest {
    @Test fun `backend sees current account and immediately loses expired or cleared credentials`() = runBlocking {
        var now = 0L
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store) { now }
        val tokens = AccessTokenStore(sessions)
        assertNull(tokens.currentToken())
        suspend fun login(token: String, id: Long, completed: Boolean = true) {
            assertTrue(sessions.acceptLogin(sessions.beginLogin(),
                AuthLogin(token, id, "user@example.com", "unused", 3600, completed), now))
        }
        login("first-access", 1)
        assertEquals("first-access", tokens.currentToken())
        assertEquals("first-access", tokens.token.first())
        val restored = AuthSessionManager(store) { now }
        restored.restore()
        assertNull(AccessTokenStore(restored).currentToken()) // GET me must validate restored sessions.
        login("second-access", 2)
        assertEquals("second-access", tokens.currentToken())
        now = 3_600_001
        assertNull(tokens.currentToken())
        login("incomplete-access", 2, completed = false)
        assertNull(tokens.currentToken())
        login("third-access", 3)
        sessions.clear()
        assertNull(tokens.currentToken())
        assertNull(tokens.token.first())
    }

    @Test fun `backend client preserves explicit chat token and never attaches session to public auth`() = runBlocking {
        val sessions = AuthSessionManager(MemorySessionStore())
        assertTrue(sessions.acceptLogin(sessions.beginLogin(),
            AuthLogin("current-access", 1, "user@example.com", "unused", 3600, true), System.currentTimeMillis()))
        MockWebServer().use { server ->
            server.start()
            val client = OkHttpClient.Builder().addInterceptor(BackendAuthInterceptor(AccessTokenStore(sessions))).build()
            for ((path, explicit, expected) in listOf(
                Triple("/api/products/search", null, "Bearer current-access"),
                Triple("/api/chat", "Bearer captured-access", "Bearer captured-access"),
                Triple("/api/auth/login", null, null),
            )) {
                server.enqueue(MockResponse.Builder().code(200).body("{}").build())
                val request = Request.Builder().url(server.url(path)).apply {
                    explicit?.let { header("Authorization", it) }
                }.build()
                client.newCall(request).execute().use { assertEquals(200, it.code) }
                assertEquals(expected, server.takeRequest().headers["Authorization"])
            }
        }
    }
    @Test fun `backend incomplete response remains readable while tokens are retained`() = runBlocking {
        val store = MemorySessionStore()
        val sessions = AuthSessionManager(store)
        sessions.acceptLogin(sessions.beginLogin(), AuthLogin("access", 1, "user@example.com", "unused", 900, true), System.currentTimeMillis())
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(403).body("""{"code":"PROFILE_INCOMPLETE"}""").build())
            val client = OkHttpClient.Builder().addInterceptor(BackendAuthInterceptor(AccessTokenStore(sessions))).build()
            client.newCall(Request.Builder().url(server.url("/api/products/search")).build()).execute().use {
                assertEquals(403, it.code)
                assertTrue(it.body.string().contains("PROFILE_INCOMPLETE"))
            }
            assertNotNull(store.saved)
            assertFalse(sessions.state.value.canEnterHome)
            assertEquals(1, server.requestCount)
        }
    }
}
