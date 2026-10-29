package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthProfilesTest {
    private val store = MemorySessionStore()
    private var now = 0L
    private val sessions = AuthSessionManager(store) { now }
    private fun profile(id: Long = 1, complete: Boolean = true, nickname: String = "사용자") =
        AuthProfile(id, "$id@example.com", nickname, "egg", null, "lowSugar", null,
            "USER", AuthProvider.LOCAL, complete, true, null)
    private fun login(token: String = "old", id: Long = 1) = AuthLogin(token, id, "$id@example.com", "unused", 900, true,
        RefreshCredentials("refresh-$token", 2_592_000_000), AuthProvider.LOCAL, true, false, now + 900_000)
    private suspend fun restore() {
        store.saved = SessionCredentials("old", 1, "1@example.com", 900_000, true,
            RefreshCredentials("refresh-old", 2_592_000_000), AuthProvider.LOCAL, true)
        sessions.restore()
    }
    private fun service(repo: AuthRepository) = AuthProfiles(repo, sessions, AuthenticatedRequests(sessions, repo))
    private fun repo(result: AuthResult<AuthProfile> = AuthResult.Success(profile())) = object : TestAuthRepository() {
        override suspend fun me(accessToken: String) = result
    }

    @Test fun `restored credentials only enter home after me confirms identity and completion`() = runTest {
        restore()
        assertFalse(sessions.state.value.canEnterHome)
        assertTrue(service(repo()).load() is AuthResult.Success)
        assertTrue(sessions.state.value.canEnterHome)
        assertEquals("사용자", sessions.state.value.profile!!.nickname)
    }

    @Test fun `offline forbidden unavailable and malformed me keep credentials and allow retry`() = runTest {
        restore()
        for (error in listOf(AuthFailure(AuthFailureKind.Network), AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED"),
            AuthFailure(AuthFailureKind.Server, 503), AuthFailure(AuthFailureKind.InvalidResponse))) {
            assertTrue(service(repo(AuthResult.Failure(error))).load() is AuthResult.Failure)
            assertNotNull(store.saved)
            assertFalse(sessions.state.value.canEnterHome)
        }
        assertTrue(service(repo()).load() is AuthResult.Success)
        assertTrue(sessions.state.value.canEnterHome)
    }

    @Test fun `incomplete profile replaces stale completion without deleting the session`() = runTest {
        restore()
        service(repo(AuthResult.Success(profile(complete = false)))).load()
        assertFalse(sessions.state.value.canEnterHome)
        assertFalse(store.saved!!.profileCompleted)
        assertNotNull(sessions.state.value.profile)
    }

    @Test fun `me from a different user is rejected without authorizing home`() = runTest {
        restore()
        val result = service(repo(AuthResult.Success(profile(id = 2)))).load() as AuthResult.Failure
        assertEquals(AuthFailureKind.InvalidResponse, result.error.kind)
        assertEquals(1L, store.saved!!.userId)
        assertNull(sessions.state.value.profile)
        assertFalse(sessions.state.value.canEnterHome)
    }

    @Test fun `expired access rotates once then verifies me before home`() = runTest {
        restore(); now = 900_000
        val calls = mutableListOf<String>()
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                calls += refreshToken
                return AuthResult.Success(login("new"))
            }
            override suspend fun me(accessToken: String): AuthResult<AuthProfile> {
                calls += accessToken
                assertFalse(sessions.state.value.canEnterHome)
                return AuthResult.Success(profile())
            }
        }
        assertTrue(service(repo).load() is AuthResult.Success)
        assertEquals(listOf("refresh-old", "new"), calls)
        assertTrue(sessions.state.value.canEnterHome)
    }

    @Test fun `late me and queued patch cannot update a newer login of the same account`() = runTest {
        restore()
        val gate = CompletableDeferred<Unit>()
        var patches = 0
        val repo = object : TestAuthRepository() {
            override suspend fun me(accessToken: String): AuthResult<AuthProfile> {
                gate.await(); return AuthResult.Success(profile())
            }
            override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
                patches++; return AuthResult.Success(profile())
            }
        }
        val service = service(repo)
        val reading = async { service.load() }; runCurrent()
        val editing = async { service.patch(ProfilePatch(nickname = "new")) }; runCurrent()
        sessions.acceptLogin(sessions.beginLogin(), login("next"), now)
        gate.complete(Unit)
        assertEquals("SESSION_CHANGED", (reading.await() as AuthResult.Failure).error.code)
        assertEquals("SESSION_CHANGED", (editing.await() as AuthResult.Failure).error.code)
        assertEquals(0, patches)
        assertEquals("next", store.saved!!.accessToken)
        assertNull(sessions.state.value.profile)
    }

    @Test fun `profile arriving during rotation preserves replacement tokens and profile across later rotation`() = runTest {
        restore()
        val source = sessions.state.value
        val gate = CompletableDeferred<Unit>()
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                gate.await(); return AuthResult.Success(login(if (refreshToken == "refresh-old") "new" else "newer"))
            }
        }
        val rotation = async { sessions.renewAccess(source, repo) }; runCurrent()
        val update = async { sessions.acceptProfile(source.generation, profile(nickname = "수정")) }; runCurrent()
        assertFalse(update.isCompleted)
        gate.complete(Unit); rotation.await(); update.await()
        assertEquals("new", store.saved!!.accessToken)
        assertEquals("refresh-new", store.saved!!.refresh!!.token)
        sessions.renewAccess(sessions.state.value, repo)
        assertEquals("수정", sessions.state.value.profile!!.nickname)
        assertEquals("newer", store.saved!!.accessToken)
    }

    @Test fun `metadata update does not suppress refresh or invalid-token handling for the same token`() = runTest {
        restore()
        val observed = sessions.state.value
        sessions.acceptProfile(observed.generation, profile())
        var calls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                calls++; return AuthResult.Success(login("new"))
            }
        }
        sessions.renewAccess(observed, repo)
        assertEquals(1, calls)
        sessions.rejectAccess(observed) // Old access cannot erase rotated pair.
        assertNotNull(store.saved)
        val rotated = sessions.state.value
        sessions.acceptProfile(rotated.generation, profile(nickname = "수정"))
        sessions.rejectAccess(rotated) // Same access despite a new credentials metadata object.
        assertNull(store.saved)
    }

    @Test fun `identical access with a rotated refresh is still a new pair for stale responses`() = runTest {
        restore()
        val observed = sessions.state.value
        sessions.acceptProfile(observed.generation, profile())
        var calls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> {
                calls++
                return AuthResult.Success(AuthLogin("old", 1, "1@example.com", "unused", 900, true,
                    RefreshCredentials("rotated-refresh", 2_592_000_000), AuthProvider.LOCAL, true, false, 900_000))
            }
        }
        assertTrue(sessions.renewAccess(observed, repo) is AuthResult.Success)
        assertTrue(sessions.renewAccess(observed, repo) is AuthResult.Success)
        assertEquals(1, calls)
        sessions.rejectAccess(observed)
        assertEquals("rotated-refresh", store.saved!!.refresh!!.token)
    }

    @Test fun `patch expired response rotates but never replays the mutation`() = runTest {
        restore()
        var calls = 0
        val repo = object : TestAuthRepository() {
            override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
                calls++; return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, "AUTH_TOKEN_EXPIRED"))
            }
            override suspend fun refresh(refreshToken: String) = AuthResult.Success(login("new"))
        }
        val result = service(repo).patch(ProfilePatch(nickname = "수정")) as AuthResult.Failure
        assertEquals("AUTH_REFRESHED_RETRY_REQUIRED", result.error.code)
        assertEquals(1, calls)
        assertEquals("new", store.saved!!.accessToken)
    }

    @Test fun `profile persistence failure never grants home`() = runTest {
        restore(); store.failWrite = true
        assertTrue(service(repo()).load() is AuthResult.Failure)
        assertFalse(sessions.state.value.canEnterHome)
        assertNull(sessions.state.value.profile)
        assertTrue(sessions.state.value.storageFailed)
    }
}
