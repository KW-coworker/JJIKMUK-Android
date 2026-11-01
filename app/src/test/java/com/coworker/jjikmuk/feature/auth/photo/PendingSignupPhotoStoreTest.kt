package com.coworker.jjikmuk.feature.auth.photo

import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingSignupPhotoStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun photo(value: Byte = 1) = ProfilePhoto(byteArrayOf(value, 2, 3), "image/jpeg")

    @Test fun `draft survives process recreation and is isolated by normalized account email`() = runBlocking {
        val directory = temporary.newFolder(); val store = FilePendingSignupPhotoStore(directory)
        val id = store.stage("User@Example.com ", photo())
        val restarted = FilePendingSignupPhotoStore(directory)
        assertEquals(id, restarted.read("user@example.com")!!.id)
        assertNull(restarted.read("other@example.com"))
        assertFalse(directory.listFiles()!!.any { it.name.contains("example") })
    }
    @Test fun `upload marker and owner survive restart and old cleanup cannot delete replacement`() = runBlocking {
        val directory = temporary.newFolder(); val store = FilePendingSignupPhotoStore(directory)
        val id = store.stage("user@example.com", photo())
        store.update("user@example.com", PendingSignupPhoto(id, 12, true, photo()))
        val restored = FilePendingSignupPhotoStore(directory).read("user@example.com")!!
        assertEquals(12L, restored.userId); assertTrue(restored.sending)
        val replacement = store.stage("user@example.com", photo(4))
        store.remove("user@example.com", id)
        assertEquals(replacement, store.read("user@example.com")!!.id)
        store.remove("user@example.com", replacement); assertNull(store.read("user@example.com"))
    }
    @Test fun `oversized and unsupported files are never persisted`() = runBlocking {
        val directory = temporary.newFolder(); val store = FilePendingSignupPhotoStore(directory)
        assertTrue(runCatching { store.stage("a@example.com", ProfilePhoto(ByteArray(ProfilePhoto.MAX_BYTES + 1), "image/jpeg")) }.isFailure)
        assertTrue(runCatching { store.stage("a@example.com", ProfilePhoto(byteArrayOf(1), "text/plain")) }.isFailure)
        assertEquals(0, directory.listFiles()!!.size)
    }
    @Test fun `corrupt draft is not silently interpreted as no photo`() = runBlocking {
        val directory = temporary.newFolder(); val store = FilePendingSignupPhotoStore(directory)
        store.stage("a@example.com", photo()); directory.listFiles()!!.single().writeBytes(byteArrayOf(0))
        assertTrue(runCatching { store.read("a@example.com") }.isFailure)
    }
}
