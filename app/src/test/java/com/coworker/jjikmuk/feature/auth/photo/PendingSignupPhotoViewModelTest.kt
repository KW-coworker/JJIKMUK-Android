package com.coworker.jjikmuk.feature.auth.photo

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingSignupPhotoViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val sessions = AuthSessionManager(MemorySessionStore()) { 0L }
    private val photo = ProfilePhoto(byteArrayOf(1, 2, 3), "image/jpeg")
    private var pending: PendingSignupPhoto? = PendingSignupPhoto("draft", 1, false, photo)
    private var removeFails = false
    private val store = object : PendingSignupPhotoStore {
        override suspend fun read(email: String) = pending.takeIf { email == "1@example.com" }
        override suspend fun stage(email: String, photo: ProfilePhoto): String = error("unused")
        override suspend fun update(email: String, record: PendingSignupPhoto) { assertEquals("1@example.com", email); pending = record }
        override suspend fun remove(email: String, id: String) { if (removeFails) error("disk"); if (pending?.id == id) pending = null }
    }
    private var serverPhoto: ProfilePhoto? = null
    private var uploadFailure: AuthFailure? = null
    private var uploads = 0
    private var reads = 0
    private var readGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    private var readFailure: AuthFailure? = null
    private val repo = object : TestAuthRepository() {
        override suspend fun photo(accessToken: String): AuthResult<ProfilePhoto> {
            reads++; readGate?.await()
            readFailure?.let { return AuthResult.Failure(it) }
            return serverPhoto?.let { AuthResult.Success(it) }
                ?: AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404, "PHOTO_NOT_FOUND"))
        }
        override suspend fun uploadPhoto(accessToken: String, photo: ProfilePhoto): AuthResult<PhotoUploadReceipt> {
            assertEquals("token-1", accessToken); assertTrue(pending!!.sending)
            uploads++; serverPhoto = photo
            return uploadFailure?.let { AuthResult.Failure(it) }
                ?: AuthResult.Success(PhotoUploadReceipt("/api/users/me/photo", true))
        }
    }
    private fun model() = PendingSignupPhotoViewModel(store, AuthPhotos(repo, sessions, AuthenticatedRequests(sessions, repo)), sessions)
    private suspend fun ready(id: Long = 1) {
        sessions.acceptLogin(sessions.beginLogin(), AuthLogin("token-$id", id, "$id@example.com", "unused", 900, true,
            expiresAtMillis = 900_000), 0)
        sessions.acceptProfile(sessions.state.value.generation, AuthProfile(id, "$id@example.com", "사용자", null, null, null,
            null, "USER", AuthProvider.LOCAL, true, true, null))
    }
    @Test fun `matching confirmed account automatically uploads once and removes draft`() = runTest {
        ready(2); val vm = model(); vm.open(); runCurrent()
        assertTrue(vm.state.value.done); assertEquals(0, reads)
        ready(); runCurrent(); vm.open(); vm.open(); runCurrent()
        assertEquals(1, uploads); assertTrue(vm.state.value.done); assertNull(pending)
    }
    @Test fun `unbound draft or different user id never uploads`() = runTest {
        for (id in listOf(null, 2L)) {
            pending = PendingSignupPhoto("draft", id, false, photo)
            ready(); val vm = model(); vm.open(); runCurrent()
            assertTrue(vm.state.value.done); assertEquals(0, reads)
        }
    }
    @Test fun `server photo wins and stale local draft is cleaned without upload`() = runTest {
        serverPhoto = ProfilePhoto(byteArrayOf(4), "image/jpeg")
        ready(); val vm = model(); vm.open(); runCurrent()
        assertTrue(vm.state.value.done); assertNull(pending); assertEquals(0, uploads)
        assertArrayEquals(byteArrayOf(4), serverPhoto!!.bytes)
    }
    @Test fun `lost upload is read back after restart without replay`() = runTest {
        uploadFailure = AuthFailure(AuthFailureKind.Network)
        ready(); var vm = model(); vm.open(); runCurrent()
        assertTrue(pending!!.sending); assertNotNull(vm.state.value.message)
        vm.open(); runCurrent(); assertEquals(1, uploads)
        vm = model(); vm.open(); runCurrent()
        assertTrue(vm.state.value.done); assertNull(pending); assertEquals(1, uploads)
    }
    @Test fun `successful upload with failed cleanup retries only local deletion`() = runTest {
        removeFails = true
        ready(); val vm = model(); vm.open(); runCurrent()
        assertNotNull(vm.state.value.message); assertEquals(1, uploads)
        removeFails = false; vm.retry(); runCurrent()
        assertTrue(vm.state.value.done); assertEquals(1, uploads)
    }
    @Test fun `account change during lookup never uploads old photo to new account`() = runTest {
        readGate = kotlinx.coroutines.CompletableDeferred()
        ready(); val vm = model(); vm.open(); runCurrent()
        ready(2); runCurrent(); readGate!!.complete(Unit); runCurrent()
        assertEquals(0, uploads); assertNotNull(pending)
    }
    @Test fun `uncertain previous upload requires explicit retry after absent server photo`() = runTest {
        pending = PendingSignupPhoto("draft", 1, true, photo)
        ready(); val vm = model(); vm.open(); runCurrent()
        assertEquals(0, uploads); assertNotNull(vm.state.value.message)
        vm.retry(); vm.retry(); runCurrent()
        assertEquals(1, uploads); assertTrue(vm.state.value.done)
    }
    @Test fun `failed server lookup keeps draft and does not assume photo is absent`() = runTest {
        readFailure = AuthFailure(AuthFailureKind.Network)
        ready(); val vm = model(); vm.open(); runCurrent()
        assertNotNull(vm.state.value.message); assertEquals(0, uploads); assertNotNull(pending)
        readFailure = null; vm.retry(); runCurrent()
        assertEquals(1, uploads); assertTrue(vm.state.value.done)
    }
}
