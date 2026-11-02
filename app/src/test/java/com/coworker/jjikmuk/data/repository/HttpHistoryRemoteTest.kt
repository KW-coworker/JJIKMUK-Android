package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.local.preference.ProductViewOwner
import com.coworker.jjikmuk.data.remote.api.HistoryApi
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HttpHistoryRemoteTest {
    @Test fun `history uses account path explicit bearer and event body with no client user id`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
            val attempt = sessions.beginLogin()
            sessions.acceptLogin(attempt, AuthLogin("account-token", 7, "7@example.com", "unused", 900, true), 1000)
            val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(HistoryApi::class.java)
            val remote = HttpHistoryRemote(api, sessions, AuthenticatedRequests(sessions, TestAuthRepository()))
            val owner = ProductViewOwner(7, sessions.state.value.generation)
            server.enqueue(MockResponse.Builder().code(200).body("""{"data":[{"id":1,"actionType":"SCAN","scannedAt":"2026-11-02T00:00:00","product":{"barcode":"123","productName":"상품","manufacturer":"제조사","imageUrl":null}}]}""").build())
            val result = remote.list(owner) as AuthResult.Success
            assertEquals("123", result.value.single()!!.product!!.barcode)
            val get = server.takeRequest()
            assertEquals("/api/histories/user/7", get.url.encodedPath)
            assertEquals("Bearer account-token", get.headers["Authorization"])
            server.enqueue(MockResponse.Builder().code(200).body("{\"message\":\"ok\",\"data\":{}}").build())
            assertTrue(remote.record(owner, "123") is AuthResult.Success)
            val post = server.takeRequest()
            assertEquals("/api/histories/events", post.url.encodedPath)
            assertEquals("Bearer account-token", post.headers["Authorization"])
            assertEquals("""{"barcode":"123","actionType":"DETAIL_VIEW"}""", post.body!!.utf8())
        }
    }

    @Test fun `expired read renews token once and reads with renewed bearer`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
            val attempt = sessions.beginLogin()
            sessions.acceptLogin(attempt, AuthLogin("old", 7, "7@example.com", "unused", 900, true,
                refresh = RefreshCredentials("refresh", expiresAtMillis = 99999999L), authProvider = AuthProvider.LOCAL, hasPassword = true, expiresAtMillis = 901000L), 1000)
            val repo = object : TestAuthRepository() {
                override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> = AuthResult.Success(
                    AuthLogin("new", 7, "7@example.com", "unused", 900, true,
                        refresh = RefreshCredentials("rotated", expiresAtMillis = 99999999L), authProvider = AuthProvider.LOCAL, hasPassword = true, expiresAtMillis = 901000L))
            }
            val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(HistoryApi::class.java)
            val remote = HttpHistoryRemote(api, sessions, AuthenticatedRequests(sessions, repo))
            server.enqueue(MockResponse.Builder().code(401).body("""{"code":"AUTH_TOKEN_EXPIRED"}""").build())
            server.enqueue(MockResponse.Builder().code(200).body("""{"data":[]}""").build())
            val result = remote.list(ProductViewOwner(7, sessions.state.value.generation))
            assertTrue((result as? AuthResult.Failure)?.error.toString(), result is AuthResult.Success)
            assertEquals("Bearer old", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer new", server.takeRequest().headers["Authorization"])
            assertEquals(2, server.requestCount)
        }
    }
}
