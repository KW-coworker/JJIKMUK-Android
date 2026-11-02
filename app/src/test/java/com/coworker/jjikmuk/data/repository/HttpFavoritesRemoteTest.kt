package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.di.NetworkModule
import com.coworker.jjikmuk.domain.repository.FavoriteAccount
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HttpFavoritesRemoteTest {
    private suspend fun remote(server: MockWebServer): Pair<HttpFavoritesRemote, FavoriteAccount> {
        val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
        val attempt = sessions.beginLogin()
        check(sessions.acceptLogin(attempt, AuthLogin("account-token", 7, "7@example.com", "unused", 900, true), 1000))
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(NetworkModule.provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create()).build().create(FavoritesApi::class.java)
        return HttpFavoritesRemote(api, sessions, AuthenticatedRequests(sessions, TestAuthRepository())) to FavoriteAccount(7, sessions.state.value.generation)
    }

    @Test fun `list uses nested items explicit bearer and requests pages by product id`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val (remote, account) = remote(server)
            server.enqueue(MockResponse.Builder().body("""{"data":{"items":[{"productId":123,"barcode":"880","product":{"productId":123,"barcode":"880","productName":"상품"}}],"page":0,"size":50,"hasNext":false}}""").build())
            val result = remote.list(account, 0) as AuthResult.Success
            assertEquals(123L, result.value.items!!.single()!!.productId)
            val request = server.takeRequest()
            assertEquals("/api/users/me/favorites", request.url.encodedPath)
            assertEquals("50", request.url.queryParameter("size"))
            assertEquals("Bearer account-token", request.headers["Authorization"])
        }
    }

    @Test fun `put and delete specify desired state without barcode or client owner body`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val (remote, account) = remote(server)
            server.enqueue(MockResponse.Builder().body("""{"data":{"productId":123,"isFavorite":true}}""").build())
            assertTrue(remote.set(account, 123, true) is AuthResult.Success)
            val put = server.takeRequest()
            assertEquals("PUT", put.method)
            assertEquals("/api/users/me/favorites/123", put.url.encodedPath)
            assertEquals("Bearer account-token", put.headers["Authorization"])
            assertTrue(put.body == null || put.body!!.size == 0)
            server.enqueue(MockResponse.Builder().body("""{"data":{"productId":123,"isFavorite":false}}""").build())
            assertTrue(remote.set(account, 123, false) is AuthResult.Success)
            assertEquals("DELETE", server.takeRequest().method)
        }
    }

    @Test fun `mutation with retry after zero is not automatically repeated`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val (remote, account) = remote(server)
            server.enqueue(MockResponse.Builder().code(503).addHeader("Retry-After", "0").body("{}").build())
            server.enqueue(MockResponse.Builder().body("""{"data":{"productId":123,"isFavorite":true}}""").build())
            assertTrue(remote.set(account, 123, true) is AuthResult.Failure)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun `old account reference never sends network request`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val (remote, account) = remote(server)
            assertTrue(remote.list(FavoriteAccount(99, account.generation), 0) is AuthResult.Failure)
            assertEquals(0, server.requestCount)
        }
    }
}
