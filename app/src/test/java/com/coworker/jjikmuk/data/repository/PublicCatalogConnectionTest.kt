package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.di.NetworkModule
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.io.IOException

class PublicCatalogConnectionTest {
    @Test fun `public catalog does not attach account credentials and permits safe reconnection`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val retrofit=NetworkModule.publicCatalogRetrofit(server.url("/").toString(),NetworkModule.provideOkHttpClient())
            assertTrue((retrofit.callFactory() as OkHttpClient).retryOnConnectionFailure)
            server.enqueue(MockResponse.Builder().body("""{"data":{"foodCategories":[]}}""").build())
            retrofit.create(ProductCatalogApi::class.java).classifications()
            val request=server.takeRequest()
            assertNull(request.headers["Authorization"])
        }
    }
    @Test fun `dropped reused connection reconnects for a safe catalog GET`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val client=NetworkModule.provideOkHttpClient().newBuilder().callTimeout(5,java.util.concurrent.TimeUnit.SECONDS).build()
            val api=NetworkModule.publicCatalogRetrofit(server.url("/").toString(),client).create(ProductCatalogApi::class.java)
            server.enqueue(MockResponse.Builder().body("""{"data":{"foodCategories":[]}}""").build())
            api.classifications() // Establish the reusable connection first.
            server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())
            server.enqueue(MockResponse.Builder().body("""{"data":{"items":[],"page":0,"totalElements":0,"hasNext":false}}""").build())
            assertEquals(0,api.filter("seafood",null,0).data?.totalElements)
            assertEquals(3,server.requestCount)
        }
    }
    @Test fun `server errors are not retried as repeated catalogue requests`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val api=NetworkModule.publicCatalogRetrofit(server.url("/").toString(),NetworkModule.provideOkHttpClient()).create(ProductCatalogApi::class.java)
            server.enqueue(MockResponse.Builder().code(500).body("{}").build())
            try { api.filter("seafood",null,0); fail("Expected server error") } catch (error:HttpException) {
                assertTrue(error.catalogLoadMessage().contains("서버 오류"))
            }
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun `timeout and dropped connections have distinct actionable messages`() {
        assertTrue(SocketTimeoutException().catalogLoadMessage().contains("응답이 늦어지고"))
        assertTrue(IOException().catalogLoadMessage().contains("연결이 끊어졌어요"))
        assertNotEquals(SocketTimeoutException().catalogLoadMessage(),IOException().catalogLoadMessage())
    }
}
