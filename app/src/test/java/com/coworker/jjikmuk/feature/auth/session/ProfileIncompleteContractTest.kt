package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProfileIncompleteContractTest {
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { 0L }
    private suspend fun login(id: Long = 1) {
        assertTrue(sessions.acceptLogin(sessions.beginLogin(),
            AuthLogin("access-$id", id, "user@example.com", "unused", 900, true), 0))
    }
    @Test fun `incomplete profile keeps credentials reloads me and does not refresh`() = runBlocking {
        login()
        val failure = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 403, "PROFILE_INCOMPLETE"))
        val repo = object : TestAuthRepository() {
            override suspend fun refresh(refreshToken: String): AuthResult<AuthLogin> = error("403 must not refresh")
            override suspend fun me(accessToken: String) = AuthResult.Success(AuthProfile(
                1, "user@example.com", "사용자", null, null, null, null, "USER", AuthProvider.LOCAL, false, true, null))
        }
        assertEquals(failure, AuthenticatedRequests(sessions, repo).execute { failure })
        assertEquals("access-1", store.saved!!.accessToken)
        assertFalse(sessions.state.value.canEnterHome)
        val result = AuthProfiles(repo, sessions, AuthenticatedRequests(sessions, repo)).load()
        assertTrue(result is AuthResult.Success)
        assertFalse(sessions.state.value.profile!!.profileCompleted)
        assertEquals("access-1", store.saved!!.accessToken)
    }
    @Test fun `ordinary forbidden does not change session and external operation incomplete keeps tokens`() = runBlocking {
        login()
        val repo = TestAuthRepository()
        val denied = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED"))
        AuthenticatedRequests(sessions, repo).execute { denied }
        assertTrue(sessions.state.value.canEnterHome)
        AuthorizedOperations(sessions, repo).execute {
            AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 403, "PROFILE_INCOMPLETE"))
        }
        assertFalse(sessions.state.value.canEnterHome)
        assertNotNull(store.saved)
    }
    @Test fun `late incomplete notification cannot change a newly signed in account`() = runBlocking {
        login(); val old = sessions.state.value
        login(2)
        sessions.requireProfileCompletion(old)
        assertTrue(sessions.state.value.canEnterHome)
        assertEquals("access-2", store.saved!!.accessToken)
    }
    @Test fun `legacy access only logout still uses bearer route`() = runBlocking {
        login()
        var accessCalls = 0
        val result = sessions.logout(object : TestAuthRepository() {
            override suspend fun logout(accessToken: String): AuthResult<LogoutReceipt> {
                assertEquals("access-1", accessToken); accessCalls++
                return AuthResult.Success(LogoutReceipt(1, true))
            }
            override suspend fun logoutWithRefresh(refreshToken: String): AuthResult<LogoutReceipt> = error("No refresh exists")
        })
        assertEquals(1, accessCalls)
        assertEquals(ServerLogoutStatus.Confirmed, result.serverStatus)
    }
}
