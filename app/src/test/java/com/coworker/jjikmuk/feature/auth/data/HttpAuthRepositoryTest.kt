package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class HttpAuthRepositoryTest {
    private val server = MockWebServer()
    @Before fun start() { server.start() }
    @After fun stop() { server.close() }
    private fun enqueue(body: String, status: Int = 200) {
        server.enqueue(MockResponse.Builder().code(status).body(body).build())
    }
    @Test fun `public OTP requests have no bearer and verify carries a string code`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue("""{"data":{"email":"user@example.com","purpose":"signup","expiresInSeconds":300}}""")
        assertTrue(repo.sendEmail(" User@Example.com ", EmailPurpose.SignUp) is AuthResult.Success)
        val send = server.takeRequest()
        assertEquals("/api/auth/email/send", send.url.encodedPath)
        assertNull(send.headers["Authorization"])
        assertEquals("signup", JsonParser.parseString(send.body!!.utf8()).asJsonObject["purpose"].asString)
        enqueue("""{"data":{"email":"user@example.com","purpose":"signup","verificationToken":"proof","expiresInSeconds":1800}}""")
        val proof = (repo.verifyEmail("user@example.com", EmailPurpose.SignUp, "0427") as AuthResult.Success).value
        assertEquals(1800, proof.expiresInSeconds)
        val verify = server.takeRequest()
        val body = JsonParser.parseString(verify.body!!.utf8()).asJsonObject
        assertEquals("0427", body["code"].asString); assertTrue(body["code"].asJsonPrimitive.isString)
        assertNull(verify.headers["Authorization"])
    }
    @Test fun `nickname duplicate uses false in 200 while login has top level fields`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue("""{"data":{"available":false}}""")
        assertEquals(AuthResult.Success(false), repo.nicknameAvailable("test"))
        enqueue("""{"token":"jwt","tokenType":"Bearer","userId":7,"email":"user@example.com","expiresAt":"2026-10-22T00:00:00Z","expiresInSeconds":86400,"profileCompleted":false}""")
        val login = (repo.login("user@example.com", "legacy") as AuthResult.Success).value
        assertEquals(7, login.userId); assertFalse(login.profileCompleted)
    }
    @Test fun `missing proof mismatched identity and malformed JSON never become success`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        for (body in listOf("{}", "not-json", """{"data":{"email":"other@example.com","purpose":"signup","verificationToken":"proof","expiresInSeconds":1800}}""")) {
            enqueue(body)
            assertTrue(repo.verifyEmail("user@example.com", EmailPurpose.SignUp, "1234") is AuthResult.Failure)
        }
    }
    @Test fun `reset sends proof not OTP and logout alone sends service bearer`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue("""{"message":"ok"}""")
        assertTrue(repo.resetPassword("user@example.com", "proof", "password123") is AuthResult.Success)
        val reset = server.takeRequest()
        val body = JsonParser.parseString(reset.body!!.utf8()).asJsonObject
        assertEquals("proof", body["verificationToken"].asString); assertFalse(body.has("code"))
        assertNull(reset.headers["Authorization"])
        enqueue("""{"data":{"userId":7,"serverTokenRevoked":false}}""")
        assertTrue(repo.logout("jwt") is AuthResult.Success)
        assertEquals("Bearer jwt", server.takeRequest().headers["Authorization"])
    }
    @Test fun `redirects do not forward credentials and errors retain code field without backend text`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        server.enqueue(MockResponse.Builder().code(307).addHeader("Location", server.url("/redirect")).build())
        assertTrue(repo.login("user@example.com", "password") is AuthResult.Failure)
        assertEquals(1, server.requestCount)
        enqueue("""{"code":"OTP_INVALID","field":"code","message":"never log me"}""", 400)
        val failure = (repo.verifyEmail("user@example.com", EmailPurpose.SignUp, "1234") as AuthResult.Failure).error
        assertEquals("OTP_INVALID", failure.code); assertEquals("code", failure.field)
        assertFalse(failure.toString().contains("never log me"))
    }
    @Test fun `signup is 201 with nested user ID and CSV conditions`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue("""{"data":{"userId":8}}""", 201)
        assertEquals(AuthResult.Success(8L), repo.signUp(SignUpSubmission("user@example.com", "abcd1234", "name", "proof", "egg,wheat", "lowSugar")))
        val body = JsonParser.parseString(server.takeRequest().body!!.utf8()).asJsonObject
        assertEquals("egg,wheat", body["allergies"].asString)
        assertNull(body["passwordConfirm"])
    }
    @Test fun `consuming POST requests never replay on 503 retry-after zero or 408`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        for (status in listOf(503, 408, 429)) {
            server.enqueue(MockResponse.Builder().code(status).addHeader("Retry-After", "0").body("{}").build())
            enqueue("""{"message":"ok"}""")
            val before = server.requestCount
            assertTrue(repo.resetPassword("user@example.com", "proof", "password123") is AuthResult.Failure)
            assertEquals(before + 1, server.requestCount)
            assertTrue(repo.resetPassword("user@example.com", "proof", "password123") is AuthResult.Success)
        }
    }
    @Test fun `new login contract is decoded through HTTP without sending bearer or normalizing password`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue(AuthContractModelsTest.loginJson)
        val result = (repo.login(" Ｕｓｅｒ＠Ｅｘａｍｐｌｅ．ｃｏｍ ", " KeepCase1! ") as AuthResult.Success).value
        assertEquals("refresh-secret", result.refresh!!.token)
        val request = server.takeRequest()
        assertNull(request.headers["Authorization"])
        val body = JsonParser.parseString(request.body!!.utf8()).asJsonObject
        assertEquals("user@example.com", body["email"].asString)
        assertEquals(" KeepCase1! ", body["password"].asString)
    }
    @Test fun `HTTP OTP metadata includes offsets and structured limits while service 503 remains server failure`() = runBlocking {
        val repo = HttpAuthRepository.create(server.url("/"))
        enqueue("""{"data":{"email":"user@example.com","purpose":"signup","expiresInSeconds":300,
            "expiresAt":"2026-10-23T21:05:00+09:00","resendAvailableAt":"2026-10-23T21:01:00+09:00"}}""")
        val dispatch = (repo.sendEmail("user@example.com", EmailPurpose.SignUp) as AuthResult.Success).value
        assertEquals(AuthApiTime.parseMillis("2026-10-23T12:05:00Z"), dispatch.expiresAtMillis)
        server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "43")
            .body("""{"code":"OTP_RATE_LIMITED","retryAfterSeconds":42,"remainingAttempts":0}""").build())
        val failure = (repo.verifyEmail("user@example.com", EmailPurpose.SignUp, "1234") as AuthResult.Failure).error
        assertEquals(43L, failure.retryDelaySeconds); assertEquals(0, failure.remainingAttempts)
        enqueue("""{"code":"AUTH_SERVICE_UNAVAILABLE"}""", 503)
        val unavailable = (repo.login("user@example.com", "legacy") as AuthResult.Failure).error
        assertEquals(AuthFailureKind.Server, unavailable.kind)
        assertEquals("AUTH_SERVICE_UNAVAILABLE", unavailable.code)
        assertEquals(3, server.requestCount)
    }
    @Test fun `unconfigured service never pretends an OTP was sent`() = runBlocking {
        assertEquals(AuthFailureKind.NotConfigured,
            (HttpAuthRepository.unconfigured().sendEmail("user@example.com", EmailPurpose.SignUp) as AuthResult.Failure).error.kind)
    }
}
