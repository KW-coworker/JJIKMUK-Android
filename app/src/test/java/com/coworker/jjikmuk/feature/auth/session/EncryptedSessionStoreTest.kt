package com.coworker.jjikmuk.feature.auth.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.security.KeyStoreException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptedSessionStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private val keys = object : SessionKeyProvider {
        var key: SecretKey? = null
        override fun encryptionKey(): SecretKey = key ?: KeyGenerator.getInstance("AES").apply { init(256) }
            .generateKey().also { key = it }
        override fun decryptionKey(): SecretKey = key ?: throw KeyStoreException("Key missing")
        override fun deleteKey() { key = null }
    }
    private val credentials = SessionCredentials("secret-token", 1, "user@example.com", 999_999, true)

    @Test fun `encrypted bytes differ per save and neither token nor email is stored as plaintext`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        store.write(credentials)
        val first = file.readBytes()
        assertFalse(first.toString(Charsets.ISO_8859_1).contains("secret-token"))
        assertFalse(first.toString(Charsets.ISO_8859_1).contains("user@example.com"))
        store.write(credentials)
        assertFalse(first.contentEquals(file.readBytes()))
        val restored = EncryptedSessionStore(file, keys).read()!!
        assertEquals("secret-token", restored.accessToken)
        assertEquals(1L, restored.userId)
        assertEquals(999_999L, restored.expiresAtMillis)
        assertTrue(restored.profileCompleted)
        store.clear()
        assertNull(store.read()); assertNull(keys.key)
        assertFalse(File(temp.root, "session.pending").exists())
    }

    @Test fun `modified truncated and missing-key ciphertext cannot restore credentials`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        store.write(credentials)
        val valid = file.readBytes()
        for (bytes in listOf(valid.copyOf(10), valid.copyOf().apply { this[lastIndex] = (last().toInt() xor 1).toByte() })) {
            file.writeBytes(bytes)
            try { store.read(); fail("Corrupt data was accepted") } catch (_: IOException) { }
        }
        file.writeBytes(valid)
        keys.deleteKey()
        try { store.read(); fail("Missing key was accepted") } catch (_: IOException) { }
    }
}
