package com.coworker.jjikmuk.feature.auth.photo

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthPhotosTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = MemorySessionStore()
    private val sessions = AuthSessionManager(store) { 0L }
    private val old = ProfilePhoto(byteArrayOf(1), "image/jpeg")
    private val serverCopy = ProfilePhoto(byteArrayOf(2), "image/jpeg")
    private var read: AuthResult<ProfilePhoto> = AuthResult.Success(old)
    private var uploads = 0
    private var uploadResult: AuthResult<PhotoUploadReceipt> = AuthResult.Success(PhotoUploadReceipt("/api/users/me/photo", true))
    private val repo = object : TestAuthRepository() {
        override suspend fun photo(accessToken: String) = read
        override suspend fun uploadPhoto(accessToken: String, photo: ProfilePhoto): AuthResult<PhotoUploadReceipt> {
            uploads++; return uploadResult
        }
        override suspend fun deletePhoto(accessToken: String): AuthResult<Unit> = AuthResult.Success(Unit)
    }
    private suspend fun service(repository: AuthRepository = repo): AuthPhotos {
        store.saved = SessionCredentials("access", 1, "1@example.com", 900_000, true)
        sessions.restore()
        return AuthPhotos(repository, sessions, AuthenticatedRequests(sessions, repository))
    }
    @Test fun `server normalized photo replaces old bytes and delete removes only current memory`() = runTest {
        val photos = service()
        photos.load(); assertSame(old, photos.state.value.photo)
        read = AuthResult.Success(serverCopy)
        photos.upload(ProfilePhoto(byteArrayOf(3), "image/jpeg"))
        assertSame(serverCopy, photos.state.value.photo); assertEquals(1, uploads)
        photos.delete(); assertNull(photos.state.value.photo)
        assertNotNull(sessions.state.value.credentials)
    }
    @Test fun `only PHOTO_NOT_FOUND is empty while forbidden and offline preserve old photo`() = runTest {
        val photos = service(); photos.load()
        for (error in listOf(AuthFailure(AuthFailureKind.Network), AuthFailure(AuthFailureKind.Server, 403, "ACCESS_DENIED"), AuthFailure(AuthFailureKind.Server, 404, "OTHER"))) {
            read = AuthResult.Failure(error); photos.load()
            assertSame(old, photos.state.value.photo); assertNotNull(sessions.state.value.credentials)
        }
        read = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404, "PHOTO_NOT_FOUND"))
        photos.load(); assertNull(photos.state.value.photo); assertTrue(photos.state.value.loaded)
    }
    @Test fun `uncertain upload retains prior image and cannot repeat before readback`() = runTest {
        val photos = service(); photos.load()
        uploadResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        photos.upload(serverCopy); assertTrue(photos.state.value.mustReload)
        assertSame(old, photos.state.value.photo)
        photos.upload(serverCopy); photos.delete(); assertEquals(1, uploads)
        read = AuthResult.Success(serverCopy); photos.load()
        assertFalse(photos.state.value.mustReload); assertSame(serverCopy, photos.state.value.photo)
    }
    @Test fun `explicit invalid upload permits correction and preserves prior image`() = runTest {
        val photos = service(); photos.load()
        uploadResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400, "PHOTO_INVALID"))
        photos.upload(serverCopy)
        assertSame(old, photos.state.value.photo); assertFalse(photos.state.value.mustReload)
        photos.upload(serverCopy); assertEquals(2, uploads)
    }
    @Test fun `logout clears bytes and late account A response never appears for account B`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun photo(accessToken: String): AuthResult<ProfilePhoto> {
                if (accessToken == "B") return AuthResult.Success(serverCopy)
                gate.await(); return AuthResult.Success(old)
            }
        }
        val photos = service(delayed)
        val pending = async { photos.load() }; runCurrent()
        sessions.clear(); runCurrent(); assertNull(photos.state.value.photo)
        val attempt = sessions.beginLogin()
        sessions.acceptLogin(attempt, AuthLogin("B", 2, "2@example.com", "unused", 900, true, expiresAtMillis = 900_000), 0)
        photos.load() // B must not wait for A's in-flight network timeout.
        gate.complete(Unit); pending.await(); runCurrent()
        assertSame(serverCopy, photos.state.value.photo); assertEquals(2L, sessions.state.value.credentials!!.userId)
    }
    @Test fun `double taps do not queue mutations and cancellation requires readback`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val delayed = object : AuthRepository by repo {
            override suspend fun uploadPhoto(accessToken: String, photo: ProfilePhoto): AuthResult<PhotoUploadReceipt> {
                uploads++; gate.await(); return uploadResult
            }
        }
        val photos = service(delayed)
        val pending = async { photos.upload(old) }; runCurrent()
        assertTrue(photos.upload(old) is AuthResult.Failure); assertEquals(1, uploads)
        pending.cancelAndJoin(); assertTrue(photos.state.value.mustReload); assertFalse(photos.state.value.busy)
    }
}
