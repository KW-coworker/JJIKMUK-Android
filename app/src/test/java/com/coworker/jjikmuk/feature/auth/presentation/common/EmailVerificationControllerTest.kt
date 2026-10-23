package com.coworker.jjikmuk.feature.auth.presentation.common

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationControllerTest {
    @Test fun `proof is bound to normalized email purpose and expires independently of OTP`() = runTest {
        for (purpose in EmailPurpose.entries) {
            val repository = TestAuthRepository()
            val controller = EmailVerificationController(repository, purpose, backgroundScope) { testScheduler.currentTime }
            controller.reset(" User@Example.com ")
            assertTrue(controller.send())
            controller.updateCode("0427")
            assertTrue(controller.verify())
            assertEquals("0427", repository.lastCode)
            assertEquals(purpose, repository.lastPurpose)
            assertEquals("user@example.com", controller.currentProof()!!.email)
            assertFalse(controller.verify()) // Server already consumed the OTP.
            advanceTimeBy(purpose.proofLifetimeSeconds * 1000L)
            assertNull(controller.currentProof())
            assertFalse(controller.state.value.verified)
        }
    }

    @Test fun `resend waits 60 seconds and only success invalidates proof`() = runTest {
        val repo = TestAuthRepository()
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com"); c.send(); c.updateCode("1234"); c.verify()
        assertFalse(c.send()); assertEquals(1, repo.sends)
        advanceTimeBy(60_000)
        repo.sendResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "VALIDATION_FAILED"))
        assertFalse(c.send()); assertNotNull(c.currentProof())
        repo.sendResult = null
        assertTrue(c.send()); assertNull(c.currentProof())
        assertEquals(300, c.state.value.remainingSeconds)
    }

    @Test fun `lost resend response requires recovery instead of reusing an uncertain proof`() = runTest {
        val repo = TestAuthRepository()
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com"); c.send(); c.updateCode("1234"); c.verify()
        advanceTimeBy(60_000)
        repo.sendResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        assertFalse(c.send()); assertNull(c.currentProof()); assertTrue(c.state.value.outcomeUnknown)
        assertFalse(c.verify()); assertEquals(2, repo.sends)
    }

    @Test fun `changing email discards late response even when upstream ignores cancellation`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = object : TestAuthRepository() {
            override suspend fun sendEmail(email: String, purpose: EmailPurpose): AuthResult<EmailDispatch> {
                withContext(NonCancellable) { gate.await() }
                return super.sendEmail(email, purpose)
            }
        }
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("first@example.com")
        val request = async { c.send() }; runCurrent()
        c.reset("second@example.com")
        gate.complete(Unit)
        assertFalse(request.await())
        assertFalse(c.state.value.sent); assertEquals("second@example.com", c.state.value.email)
    }

    @Test fun `double tap has only one request and cancellation is not swallowed`() = runTest {
        val repo = object : TestAuthRepository() {
            override suspend fun sendEmail(email: String, purpose: EmailPurpose): AuthResult<EmailDispatch> {
                sends++
                awaitCancellation()
            }
        }
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com")
        val request = launch { c.send() }; runCurrent()
        assertFalse(c.send()); assertEquals(1, repo.sends)
        request.cancelAndJoin()
        assertFalse(c.state.value.busy); assertTrue(c.state.value.outcomeUnknown)
    }

    @Test fun `returning to the email screen does not bypass resend cooldown`() = runTest {
        val repo = TestAuthRepository()
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com"); c.send()
        c.reset("USER@example.com")
        assertFalse(c.send()); assertEquals(1, repo.sends)
        advanceTimeBy(60_000)
        assertTrue(c.send())
    }

    @Test fun `OTP expiration and attempts limit prevent further verification`() = runTest {
        val repo = TestAuthRepository()
        val c = EmailVerificationController(repo, EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com"); c.send(); c.updateCode("1234")
        advanceTimeBy(300_000)
        assertFalse(c.verify()); assertEquals(0, repo.verifies)
        c.send(); c.updateCode("1234")
        repo.verifyResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 429, "OTP_RATE_LIMITED"))
        assertFalse(c.verify()); assertFalse(c.verify()); assertEquals(1, repo.verifies)
    }

    @Test fun `format validation preserves ASCII leading zero and excludes unicode digits`() = runTest {
        val c = EmailVerificationController(TestAuthRepository(), EmailPurpose.SignUp, backgroundScope) { testScheduler.currentTime }
        c.reset("user@example.com"); c.send()
        c.updateCode("０0a4-27")
        assertEquals("0427", c.state.value.code)
        c.updateCode("12"); assertFalse(c.verify())
    }
}
