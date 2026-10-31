package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.scanner.data.*
import com.coworker.jjikmuk.feature.scanner.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizedOperationsTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private var now = 0L
    private val sessions = AuthSessionManager(MemorySessionStore()) { now }
    private var refreshCalls = 0
    private fun login(id: Long = 1, token: String = "access", refresh: String = "refresh") =
        AuthLogin(token, id, "$id@example.com", "unused", 900, true,
            RefreshCredentials(refresh, 2_592_000_000), AuthProvider.LOCAL, true, expiresAtMillis = now + 900_000)
    private val repo = object : TestAuthRepository() {
        override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
            refreshCalls++; return AuthResult.Success(login(token = "rotated", refresh = "rotated-refresh"))
        }
    }
    private val operations = AuthorizedOperations(sessions, repo)
    private suspend fun ready(id: Long = 1) {
        sessions.acceptLogin(sessions.beginLogin(), login(id), now)
        sessions.acceptProfile(sessions.state.value.generation, AuthProfile(id, "$id@example.com", "사용자", null, null,
            null, null, "USER", AuthProvider.LOCAL, true, true, null))
    }
    @Test fun `missing session never calls paid transport`() = runTest {
        var calls = 0
        assertTrue(operations.execute { calls++; AuthResult.Success(Unit) } is AuthResult.Failure)
        assertEquals(0, calls)
    }
    @Test fun `expired access is renewed before one upload`() = runTest {
        ready(); now = 900_001; var calls = 0
        val result = operations.execute { calls++; assertEquals("rotated", it); AuthResult.Success(Unit) }
        assertTrue(result is AuthResult.Success); assertEquals(1, calls); assertEquals(1, refreshCalls)
    }
    @Test fun `401 after dispatch never refreshes or replays and clears rejected session`() = runTest {
        ready(); var calls = 0
        val result = operations.execute { calls++; AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_EXPIRED")) }
        assertTrue(result is AuthResult.Failure); assertEquals(1, calls); assertEquals(0, refreshCalls)
        assertNull(sessions.state.value.credentials)
    }
    @Test fun `403 and network failure preserve session`() = runTest {
        ready()
        operations.execute { AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED")) }
        assertNotNull(sessions.state.value.credentials)
        operations.execute { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
        assertNotNull(sessions.state.value.credentials); assertEquals(0, refreshCalls)
    }
    @Test fun `late 401 from old account cannot log out new account`() = runTest {
        ready(); val gate = CompletableDeferred<Unit>()
        val request = async { operations.execute { gate.await(); AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401)) } }
        runCurrent(); ready(2); gate.complete(Unit); request.await()
        assertEquals(2L, sessions.state.value.credentials!!.userId)
    }
    @Test fun `late 401 does not revoke independently refreshed token pair`() = runTest {
        ready(); val observed = sessions.state.value; val gate = CompletableDeferred<Unit>()
        val request = async { operations.execute { gate.await(); AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401)) } }
        runCurrent(); sessions.renewAccess(observed, repo); gate.complete(Unit); request.await()
        assertEquals("rotated", sessions.state.value.credentials!!.accessToken)
    }
    @Test fun `OCR adapter supplies service token and invalid response cannot start search after account switch`() = runTest {
        ready(); val gate = CompletableDeferred<Unit>(); var calls = 0
        val service = AuthenticatedScannerOcrService(ScannerOcrTransport { _, token ->
            calls++; assertEquals("access", token); gate.await(); OcrResult.NoText(OcrTrace())
        }, operations)
        val request = async { service.recognize(PreparedScannerPhoto(File("unused"), 2, 2)) }
        runCurrent(); ready(2); gate.complete(Unit)
        assertEquals(OcrFailure.Unauthorized, (request.await() as OcrResult.Failure).reason)
        assertEquals(1, calls)
    }
}
