package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.SafeRecommendationApi
import com.coworker.jjikmuk.data.remote.auth.AccessTokenStore
import com.coworker.jjikmuk.data.remote.auth.BackendAuthInterceptor
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import com.coworker.jjikmuk.feature.auth.session.MemorySessionStore
import com.coworker.jjikmuk.feature.auth.domain.AuthLogin
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SafeRecommendationRepositoryTest {
    private val server = MockWebServer()
    private val sessions = AuthSessionManager(MemorySessionStore())
    private val tokens = AccessTokenStore(sessions)
    private lateinit var repo: SafeRecommendationRepository
    @Before fun setup() {
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(BackendAuthInterceptor(tokens)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(SafeRecommendationApi::class.java)
        repo = SafeRecommendationRepository(api)
    }
    @After fun close() { server.close() }
    private fun reply(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json").build())
    }
    @Test fun `opening recommendations reads the server profile without overwriting saved conditions`() = runBlocking {
        reply("""{"data":{"id":7,"nickname":"내닉네임","allergies":"peanut","specialDiet":"lowSugar"}}""")
        assertEquals("내닉네임", repo.owner().userName)
        val get = server.takeRequest()
        assertEquals("/api/users/me", get.url.encodedPath)
        assertEquals("GET", get.method)
        assertEquals(1, server.requestCount)
    }
    @Test fun `same endpoint gains bearer token and respects server cursor and ordering`() = runBlocking {
        assertTrue(sessions.acceptLogin(sessions.beginLogin(),
            AuthLogin("test-access-token", 7, "test@example.com", "unused", 3600, true), System.currentTimeMillis()))
        reply("""{"data":{"userId":7,"returnedBarcodes":["b","a"],"hasNext":true,"nextCursor":"next+cursor","categories":[{"items":[{"product":{"barcode":"a","productName":"상품A"}},{"product":{"barcode":"b","productName":"상품B"}}]},{"items":[{"product":{"barcode":"a","productName":"상품A"}}]}]}}""")
        val page = repo.page("previous", listOf("seen"))
        assertEquals(listOf("b", "a"), page.products.map { it.barcode })
        assertEquals("next+cursor", page.nextCursor)
        val request = server.takeRequest()
        assertEquals("Bearer test-access-token", request.headers["Authorization"])
        assertEquals("/api/products/safe-recommendations", request.url.encodedPath)
        assertEquals("previous", request.url.queryParameter("cursor"))
        assertEquals("seen", request.url.queryParameter("excludeBarcodes"))
        assertNull(request.url.queryParameter("userId"))
        sessions.clear()
        reply("""{"data":{"userId":7,"hasNext":false}}""")
        assertNull(repo.page().nextCursor)
        assertNull(server.takeRequest().headers["Authorization"])
    }
    @Test fun `unauthorized and malformed data never become sample recommendations`() = runBlocking {
        reply("{}", 401)
        try { repo.page(); fail("Must report auth failure") } catch (e: HttpException) {
            assertEquals(401, e.code())
            assertTrue(e.recommendationMessage().contains("로그인"))
        }
        reply("""{"data":{"hasNext":true}}""")
        try { repo.page(); fail("Must reject missing cursor") } catch (_: IllegalStateException) { }
    }
}
