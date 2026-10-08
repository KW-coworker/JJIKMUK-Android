package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

internal interface ScannerOcrApi {
    @POST("api/ocr")
    @Streaming
    suspend fun recognize(@Header("X-OCR-Test-Token") token: String, @Body image: MultipartBody): Response<ResponseBody>
}

/** Not bound in production DI yet. No token constant, logging, automatic retries or user JWT. */
class HttpScannerOcrService internal constructor(
    private val api: ScannerOcrApi,
    private val tokenProvider: () -> String?,
) : ScannerOcrService {
    override val available = true
    private val inFlight = AtomicBoolean(false)

    override suspend fun recognize(photo: PreparedScannerPhoto): OcrResult {
        if (!inFlight.compareAndSet(false, true)) return OcrResult.Failure(OcrFailure.Busy)
        try {
            val token = tokenProvider()?.takeIf { it.isNotBlank() && it.all { char -> char.code in 33..126 } }
                ?: return OcrResult.Failure(OcrFailure.NotConfigured)
            return withContext(Dispatchers.IO) {
                if (!photo.isUploadable() || !photo.hasJpegSignature()) {
                    return@withContext OcrResult.Failure(OcrFailure.InvalidImage)
                }
                val binary = object : RequestBody() {
                    override fun contentType() = photo.mimeType.toMediaType()
                    override fun contentLength() = photo.file.length()
                    // Also prevents follow-up replay for 408/503/redirect/auth cases in OkHttp.
                    override fun isOneShot() = true
                    override fun writeTo(sink: BufferedSink) {
                        photo.file.source().use { sink.writeAll(it) }
                    }
                }
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("image", "product.jpg", binary).build()
                if (body.contentLength() > PreparedScannerPhoto.MAX_BYTES + 64 * 1024) {
                    return@withContext OcrResult.Failure(OcrFailure.InvalidImage)
                }
                val response = api.recognize(token, body)
                val responseBody = response.body() ?: response.errorBody()
                // Bound malformed proxy/server responses rather than reading unlimited data.
                val raw = responseBody?.use { value ->
                    val source = value.source()
                    source.request(MAX_RESPONSE_BYTES + 1)
                    if (source.buffer.size > MAX_RESPONSE_BYTES) null else source.readUtf8()
                }
                OcrResponseParser.parse(response.code(), raw,
                    response.headers()["X-Request-ID"], response.headers()["Retry-After"])
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // The server may have received the image. Never silently resend.
            return OcrResult.Failure(OcrFailure.OutcomeUnknown)
        } finally {
            inFlight.set(false)
        }
    }

    private fun PreparedScannerPhoto.hasJpegSignature(): Boolean = file.inputStream().use {
        it.read() == 0xff && it.read() == 0xd8 && it.read() == 0xff
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 1024L * 1024

        fun client(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(150, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
            .build()

        fun create(baseUrl: HttpUrl, tokenProvider: () -> String?): HttpScannerOcrService {
            require(baseUrl.isHttps && baseUrl.encodedPath == "/" && baseUrl.query == null)
            require(baseUrl.username.isEmpty() && baseUrl.password.isEmpty() && baseUrl.fragment == null)
            val api = Retrofit.Builder().baseUrl(baseUrl).client(client()).build().create(ScannerOcrApi::class.java)
            return HttpScannerOcrService(api, tokenProvider)
        }
    }
}
