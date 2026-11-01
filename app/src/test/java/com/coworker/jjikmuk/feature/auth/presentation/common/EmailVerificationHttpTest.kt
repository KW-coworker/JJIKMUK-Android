package com.coworker.jjikmuk.feature.auth.presentation.common

import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.EmailPurpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class EmailVerificationHttpTest {
    @Test fun `slow successful email delivery advances to OTP without restarting its five minute lifetime`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            // The deployed service returned 200 after 32.4 seconds with offsetless timestamps.
            // A real delay beyond the old 30-second read timeout catches both regressions.
            server.enqueue(MockResponse.Builder().code(200).headersDelay(31, TimeUnit.SECONDS).body(
                """{"message":"ok","data":{"email":"user@example.com","purpose":"signup",
                    "expiresInSeconds":300,"expiresAt":"2026-10-31T17:50:38.2502111",
                    "resendAvailableAt":"2026-10-31T17:46:38.2502111"}}"""
            ).build())
            val tickerScope = CoroutineScope(coroutineContext + SupervisorJob())
            try {
                val controller = EmailVerificationController(
                    HttpAuthRepository.create(server.url("/")), EmailPurpose.SignUp, tickerScope,
                )
                controller.reset("user@example.com")
                assertTrue(controller.send()) // SignUpViewModel uses this to navigate to OTP entry.
                val state = controller.state.value
                assertTrue(state.sent)
                assertFalse(state.busy)
                assertFalse(state.outcomeUnknown)
                assertNull(state.error)
                assertTrue(state.remainingSeconds in 1..269)
                assertEquals(1, server.requestCount)
                assertFalse(controller.send()) // Waiting does not permit another immediate send.
                assertEquals(1, server.requestCount)

                server.enqueue(MockResponse.Builder().code(200).body(
                    """{"data":{"email":"user@example.com","purpose":"signup","verificationToken":"proof",
                        "expiresInSeconds":1800,"expiresAt":"2026-10-31T18:16:00.123456789"}}"""
                ).build())
                controller.updateCode("0427")
                assertTrue(controller.verify())
                assertTrue(controller.state.value.verified)
                assertEquals("user@example.com", controller.currentProof()!!.email)
                assertEquals(2, server.requestCount)
            } finally {
                tickerScope.cancel()
            }
        }
    }
}
