package com.coworker.jjikmuk.feature.auth

import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.data.remote.BackendServer
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Opt-in production-adapter checks: no email dispatch, account creation, or valid credentials. */
class InstitutionAuthContractTest {
    private val repository = HttpAuthRepository.create(BackendServer.BASE_URL.toHttpUrl())

    @Before fun optIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveBackend") == "true")
    }

    private fun failure(result: AuthResult<*>, status: Int, code: String): AuthFailure {
        assertTrue("Expected a structured failure, received ${result.javaClass.simpleName}", result is AuthResult.Failure)
        val error = (result as AuthResult.Failure).error
        assertEquals(AuthFailureKind.Server, error.kind)
        assertEquals(status, error.status)
        assertEquals(code, error.code)
        return error
    }

    @Test fun nicknameAvailabilityDecodesThroughAppRepository() = runBlocking {
        val result = repository.nicknameAvailable("AuthProbe20261031")
        assertTrue("Availability must decode as Boolean without reserving a nickname", result is AuthResult.Success)
    }

    @Test fun malformedNicknameReturnsFieldValidation() = runBlocking {
        val error = failure(repository.nicknameAvailable("!"), 400, "NICKNAME_INVALID")
        assertEquals("nickname", error.field)
    }

    @Test fun invalidAccessCannotLoadProfileOrPhoto() = runBlocking {
        failure(repository.me("invalid-contract-probe"), 401, "AUTH_TOKEN_INVALID")
        failure(repository.photo("invalid-contract-probe"), 401, "AUTH_TOKEN_INVALID")
        Unit
    }

    @Test fun invalidRefreshIsRejectedWithoutCreatingASession() = runBlocking {
        failure(repository.refresh("invalid-contract-probe"), 401, "REFRESH_TOKEN_INVALID")
        Unit
    }
}
