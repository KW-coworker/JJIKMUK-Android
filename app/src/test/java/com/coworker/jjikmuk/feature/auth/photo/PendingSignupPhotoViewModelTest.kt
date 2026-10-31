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
    private val repo = object : TestAuthRepository() {
        override suspend fun photo(accessToken: String): AuthResult<ProfilePhoto> {
            reads++; return serverPhoto?.let { AuthResult.Success(it) }
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
    @Test fun `only matching account sees draft and upload requires explicit confirmation`() = runTest {
        ready(2); val vm = model(); vm.open(); runCurrent()
        assertTrue(vm.state.value.done); assertNull(vm.state.value.pending); assertEquals(0, reads)
        ready(); runCurrent(); vm.open(); runCurrent()
        assertTrue(vm.state.value.canUpload); assertEquals(0, uploads); assertNotNull(pending)
        vm.upload(); vm.upload(); runCurrent()
        assertEquals(1, uploads); assertTrue(vm.state.value.done); assertNull(pending)
    }
    @Test fun `same email with different confirmed user id never gets old photo`() = runTest {
        pending = PendingSignupPhoto("draft", 2, false, photo)
        ready(); val vm = model(); vm.open(); runCurrent()
        assertTrue(vm.state.value.done); assertNull(vm.state.value.pending); assertEquals(0, reads)
    }
    @Test fun `server photo is shown without silently overwriting another device change`() = runTest {
        serverPhoto = ProfilePhoto(byteArrayOf(4), "image/jpeg")
        ready(); val vm = model(); vm.open(); runCurrent()
        assertArrayEquals(byteArrayOf(4), vm.state.value.server!!.bytes); assertEquals(0, uploads)
        vm.discard(); runCurrent(); assertNull(pending); assertEquals(0, uploads)
        assertArrayEquals(byteArrayOf(4), serverPhoto!!.bytes)
    }
    @Test fun `lost upload remains durable and restart reads back without replay`() = runTest {
        ready(); var vm = model(); vm.open(); runCurrent()
        uploadFailure = AuthFailure(AuthFailureKind.Network); vm.upload(); runCurrent()
        assertTrue(pending!!.sending); assertTrue(vm.state.value.mustReload)
        vm.upload(); runCurrent(); assertEquals(1, uploads)
        vm = model(); vm.open(); runCurrent()
        assertNotNull(vm.state.value.server); assertFalse(pending!!.sending); assertEquals(1, uploads)
        vm.discard(); runCurrent(); assertNull(pending); assertEquals(1, uploads)
    }
    @Test fun `successful upload with failed cleanup retries only local deletion`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); removeFails = true
        vm.upload(); runCurrent(); assertTrue(vm.state.value.uploaded); assertFalse(vm.state.value.done)
        vm.upload(); runCurrent(); assertEquals(1, uploads)
        removeFails = false; vm.discard(); runCurrent(); assertTrue(vm.state.value.done); assertEquals(1, uploads)
    }
    @Test fun `account change before scheduled upload never sends old photo with new token`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.upload()
        ready(2); runCurrent(); assertEquals(0, uploads); assertNull(vm.state.value.pending)
    }
    @Test fun `later keeps private draft for next login without sending it`() = runTest {
        ready(); val vm = model(); vm.open(); runCurrent(); vm.later()
        assertTrue(vm.state.value.done); assertNull(vm.state.value.pending); assertNotNull(pending); assertEquals(0, uploads)
    }
}
