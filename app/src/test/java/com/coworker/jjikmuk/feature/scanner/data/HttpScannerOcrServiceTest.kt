package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit

class HttpScannerOcrServiceTest {
    @get:Rule val temporary = TemporaryFolder()
    private val server = MockWebServer()
    private lateinit var photo: PreparedScannerPhoto

    @Before fun setUp() {
        server.start()
        val file = temporary.newFile("photo.jpg")
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpg", file)
        photo = PreparedScannerPhoto(file, 2, 2)
    }
    @After fun tearDown() { server.close() }

    @Test fun `wire request has exactly one image part and one test token with no user auth`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).body("""{"data":{"status":"OK","searchKeyword":"어성초","requestId":"ocr"}}""")
            .addHeader("X-Request-ID", "relay").build())
        val result = service().recognize(photo) as OcrResult.Extracted
        assertEquals(OcrTrace("relay", "ocr"), result.trace)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/ocr", request.url.encodedPath)
        assertNull(request.url.query)
        assertEquals(listOf("unit-test-only"), request.headers.values("X-OCR-Test-Token"))
        assertNull(request.headers["Authorization"])
        val body = request.body!!.utf8()
        assertEquals(1, Regex("Content-Disposition:").findAll(body).count())
        assertTrue(body.contains("name=\"image\"; filename=\"product.jpg\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
        assertFalse(body.contains("userId"))
    }

    @Test fun `429 and 503 retry after never automatically replay paid request`() = runBlocking {
        for (status in listOf(429, 503, 408)) {
            server.enqueue(MockResponse.Builder().code(status).addHeader("Retry-After", "0").body("{}").build())
            server.enqueue(MockResponse.Builder().code(200).body("""{"data":{"status":"NO_TEXT"}}""").build())
            val before = server.requestCount
            assertTrue(service().recognize(photo) is OcrResult.Failure)
            assertEquals(before + 1, server.requestCount)
            // Consume the sentinel only by another explicit call.
            assertTrue(service().recognize(photo) is OcrResult.NoText)
        }
    }

    @Test fun `redirect is not followed and token is not forwarded`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(307).addHeader("Location", server.url("/other")).build())
        assertTrue(service().recognize(photo) is OcrResult.Failure)
        assertEquals(1, server.requestCount)
    }

    @Test fun `missing token and invalid image do not reach the server`() = runBlocking {
        assertEquals(OcrFailure.NotConfigured, (service(token = null).recognize(photo) as OcrResult.Failure).reason)
        photo.file.writeText("not a jpeg")
        assertEquals(OcrFailure.InvalidImage, (service().recognize(photo) as OcrResult.Failure).reason)
        assertEquals(0, server.requestCount)
    }

    @Test fun `read timeout is unknown outcome with a single request`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).headersDelay(1, TimeUnit.SECONDS).body("{}").build())
        val client = HttpScannerOcrService.client().newBuilder().readTimeout(100, TimeUnit.MILLISECONDS).build()
        val result = service(client).recognize(photo) as OcrResult.Failure
        assertEquals(OcrFailure.OutcomeUnknown, result.reason)
        assertEquals(1, server.requestCount)
    }

    @Test fun `OCR client has separate waiting and replay policies with no logging interceptor`() {
        val client = HttpScannerOcrService.client()
        assertTrue(client.readTimeoutMillis >= 150_000)
        assertTrue(client.callTimeoutMillis >= 150_000)
        assertFalse(client.retryOnConnectionFailure)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertTrue(client.interceptors.isEmpty())
        assertTrue(client.networkInterceptors.isEmpty())
    }

    @Test fun `concurrent request is rejected locally and cancellation never resends`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).headersDelay(2, TimeUnit.SECONDS).body("{}").build())
        val service = service()
        val running = async(Dispatchers.IO) { service.recognize(photo) }
        server.takeRequest()
        assertEquals(OcrFailure.Busy, (service.recognize(photo) as OcrResult.Failure).reason)
        running.cancelAndJoin()
        assertTrue(running.isCancelled)
        assertEquals(1, server.requestCount)
    }

    private fun service(client: OkHttpClient = HttpScannerOcrService.client(), token: String? = "unit-test-only") =
        HttpScannerOcrService(Retrofit.Builder().baseUrl(server.url("/")).client(client).build()
            .create(ScannerOcrApi::class.java)) { token }
}
