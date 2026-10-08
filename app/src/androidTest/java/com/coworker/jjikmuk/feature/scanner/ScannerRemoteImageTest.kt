package com.coworker.jjikmuk.feature.scanner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.coworker.jjikmuk.JjikmukApplication
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerRemoteImageTest {
    @Test fun appImageLoaderFetchesImagesWithoutApiCredentialsAndReports404() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<JjikmukApplication>()
        val loader = app.newImageLoader(app)
        val server = MockWebServer()
        server.start()
        try {
            val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            image.recycle()
            server.enqueue(MockResponse.Builder().addHeader("Content-Type", "image/png").body(Buffer().write(bytes)).build())
            val result = loader.execute(ImageRequest.Builder(app).data(server.url("/image.png").toString()).build())
            assertTrue(result is SuccessResult)
            val request = server.takeRequest()
            assertNull(request.headers["Authorization"])
            assertNull(request.headers["X-OCR-Test-Token"])
            server.enqueue(MockResponse.Builder().code(404).build())
            val missing = loader.execute(ImageRequest.Builder(app).data(server.url("/missing.png").toString()).build())
            assertTrue(missing is ErrorResult)
            assertEquals(2, server.requestCount)
        } finally {
            loader.shutdown()
            server.close()
        }
    }
}
