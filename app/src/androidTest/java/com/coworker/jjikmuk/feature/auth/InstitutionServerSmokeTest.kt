package com.coworker.jjikmuk.feature.auth

import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.data.remote.BackendServer
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Opt-in, credential-free deployment probe. Never creates accounts or invokes paid OCR/AI processing. */
class InstitutionServerSmokeTest {
    @Test fun publicHttpsAndAuthenticationGates() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveBackend") == "true")
        fun request(path: String, method: String = "GET", body: String? = null): Int {
            val connection = URL(BackendServer.BASE_URL + path).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 10_000; connection.readTimeout = 10_000
                connection.instanceFollowRedirects = false; connection.requestMethod = method
                if (body != null) {
                    connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toByteArray()) }
                }
                return connection.responseCode // Platform trust + hostname verification left untouched.
            } finally { connection.disconnect() }
        }
        assertEquals(200, request("api/products/classifications"))
        assertEquals(401, request("api/users/me"))
        assertEquals(400, request("api/auth/login", "POST", "{}"))
        assertEquals(401, request("api/ocr", "POST", "{}"))
    }
}
