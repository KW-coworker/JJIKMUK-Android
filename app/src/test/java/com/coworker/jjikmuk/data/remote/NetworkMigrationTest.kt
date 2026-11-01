package com.coworker.jjikmuk.data.remote

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.di.NetworkModule
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkMigrationTest {
    @Test fun `chat uses backend path Bearer and explicit false consent without body logging`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"answer":"확인"}""").build())
            val client = NetworkModule.provideOkHttpClient()
            val api = Retrofit.Builder().baseUrl(server.url("/")).client(client).addConverterFactory(GsonConverterFactory.create())
                .build().create(ChatApi::class.java)
            api.sendMessage("Bearer test-only", ChatApiRequest("질문"))
            val request = server.takeRequest(); val body = request.body!!.utf8()
            assertEquals("/api/chat", request.url.encodedPath)
            assertEquals("Bearer test-only", request.headers["Authorization"])
            assertTrue(body.contains("\"externalAiConsent\":false")); assertTrue(body.contains("\"chatHistory\":[]"))
            assertFalse(body.contains("chat_history"))
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            assertEquals(setOf("message", "chatHistory", "externalAiConsent"), json.keySet())
            assertFalse(json.has("profiles"))
            assertFalse(json.has("product"))
            assertFalse(client.retryOnConnectionFailure); assertFalse(client.followRedirects)
            assertFalse(client.interceptors.any { it.javaClass.name.contains("HttpLoggingInterceptor") })
            assertEquals("https://43.203.42.36/", BackendServer.BASE_URL)
        }
    }
    @Test fun `selected product sends barcode without unsupported product or profiles`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"answer":"확인"}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .client(NetworkModule.provideOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create()).build().create(ChatApi::class.java)
            api.sendMessage("Bearer test-only", ChatApiRequest("먹어도 돼?", barcode = "8801019309830"))
            val json = com.google.gson.JsonParser.parseString(server.takeRequest().body!!.utf8()).asJsonObject
            assertEquals("8801019309830", json.get("barcode").asString)
            assertEquals(setOf("message", "barcode", "chatHistory", "externalAiConsent"), json.keySet())
        }
    }
    @Test fun `shared POST never replays 503 retry after or follows redirect`() = runBlocking {
        for (status in listOf(401, 503, 408, 307)) MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(status).addHeader("Retry-After", "0")
                .addHeader("Location", server.url("/other")).body("{}").build())
            server.enqueue(MockResponse.Builder().body("""{"answer":"must not reach"}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/")).client(NetworkModule.provideOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create()).build().create(ChatApi::class.java)
            assertTrue(runCatching { api.sendMessage("Bearer test-only", ChatApiRequest("질문")) }.isFailure)
            assertEquals(1, server.requestCount)
        }
    }
}
