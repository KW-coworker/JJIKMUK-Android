package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit

class HttpScannerListServiceTest {
    @get:Rule val temp=TemporaryFolder()
    private val server=MockWebServer()
    private val trace="f360f06f-04f5-44a8-a053-6c754dc98829"
    private lateinit var photo: PreparedScannerPhoto
    @Before fun setup() {
        server.start()
        val image=temp.newFile("photo.jpg"); ImageIO.write(BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"jpg",image)
        photo=PreparedScannerPhoto(image,20,20,"d4e760c2-977a-4f06-989e-3ee1f3716268")
    }
    @After fun stop() { server.close() }
    private fun service()=HttpScannerListService(Retrofit.Builder().baseUrl(server.url("/"))
        .client(HttpScannerListService.client()).build().create(ScannerListApi::class.java))
    @Test fun `real Python Kotlin response is accepted over multipart transport in original order`()=runBlocking {
        val body=requireNotNull(javaClass.getResource("/scan-list-wire.json")).readText()
        server.enqueue(MockResponse.Builder().code(200).addHeader("X-Request-ID",trace).body(body).build())
        val result=service().scan(photo,"fake-test-access-token") as ScanListResult.Products
        assertEquals(listOf("NO_BARCODE_ROW_2","fixture-barcode-3"),result.products.map { it.barcode })
        val request=server.takeRequest()
        assertEquals("/api/product-scans",request.url.encodedPath)
        assertEquals(photo.scanId,request.headers["Idempotency-Key"])
        assertEquals("Bearer fake-test-access-token",request.headers["Authorization"])
        assertNull(request.headers["X-Scan-Owner-ID"]);assertNull(request.headers["X-OCR-Service-Token"])
        val sent=request.body!!.utf8()
        assertEquals(1,Regex("Content-Disposition:").findAll(sent).count())
        assertFalse(sent.contains("allow_fallback"))
    }
    @Test fun `redirect and transient status never replay the photo`()=runBlocking {
        for (status in listOf(307,408,429,503,504)) {
            server.enqueue(MockResponse.Builder().code(status).addHeader("Location",server.url("/other"))
                .addHeader("Retry-After","0").body("{}").build())
            val before=server.requestCount
            assertTrue(service().scan(photo,"fake-test-access-token") is ScanListResult.Failure)
            assertEquals(before+1,server.requestCount)
        }
    }
    @Test fun `same prepared photo retains stable scan ID and local duplicate is rejected`()=runBlocking {
        server.enqueue(MockResponse.Builder().code(503).body("{}").build())
        val service=service();service.scan(photo,"fake-test-access-token")
        assertEquals(photo.scanId,photo.copy().scanId)
        val rejected=service.scan(photo,"fake-test-access-token") as ScanListResult.Failure
        assertEquals(OcrFailure.RateLimited,rejected.failure.reason)
        assertEquals(1,server.requestCount)
    }
}
