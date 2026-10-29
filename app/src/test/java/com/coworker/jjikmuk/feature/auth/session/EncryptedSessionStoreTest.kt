package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.AuthProvider
import com.coworker.jjikmuk.feature.auth.domain.RefreshCredentials
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStoreException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.Cipher

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

    @Test fun `new format restores token pair expiry and provider without exposing either token`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        for (provider in AuthProvider.entries) {
            val pair = SessionCredentials("access-secret", 7, "other@example.com", 900_000, false,
                RefreshCredentials("refresh-secret", 2_592_000_000), provider, provider != AuthProvider.GOOGLE)
            store.write(pair)
            assertEquals(2, DataInputStream(file.inputStream()).use { it.readInt() })
            val bytes = file.readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(bytes.contains("access-secret")); assertFalse(bytes.contains("refresh-secret"))
            val restored = EncryptedSessionStore(file, keys).read()!!
            assertEquals("access-secret", restored.accessToken)
            assertEquals("refresh-secret", restored.refresh!!.token)
            assertEquals(900_000L, restored.expiresAtMillis)
            assertEquals(2_592_000_000L, restored.refresh.expiresAtMillis)
            assertEquals(provider, restored.authProvider)
            assertEquals(provider != AuthProvider.GOOGLE, restored.hasPassword)
            assertFalse(restored.profileCompleted)
            assertFalse(restored.toString().contains("access-secret"))
            assertFalse(restored.refresh.toString().contains("refresh-secret"))
        }
    }

    @Test fun `legacy v1 stays access only and a new login replaces it with v2`() = runBlocking {
        val file = File(temp.root, "session")
        writeLegacy(file)
        val store = EncryptedSessionStore(file, keys)
        val restored = store.read()!!
        assertEquals("legacy-access", restored.accessToken)
        assertNull(restored.refresh); assertNull(restored.authProvider); assertNull(restored.hasPassword)
        store.write(SessionCredentials("new-access", 2, "new@example.com", 900_000, true,
            RefreshCredentials("new-refresh", 2_592_000_000), AuthProvider.LOCAL, true))
        assertEquals(2, DataInputStream(file.inputStream()).use { it.readInt() })
        assertEquals("new-refresh", store.read()!!.refresh!!.token)
        assertEquals(2L, store.read()!!.userId)
    }

    @Test fun `unsupported version and version downgrade cannot restore the pair`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        store.write(credentials)
        val bytes = file.readBytes()
        for (version in listOf(1, 3)) {
            file.writeBytes(bytes.copyOf().apply { this[3] = version.toByte() })
            try { store.read(); fail("Version change was accepted") } catch (_: IOException) { }
        }
    }

    @Test fun `failed replacement preserves previous pair and pending file is never restored`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        store.write(credentials)
        val pending = File(temp.root, "session.pending")
        pending.writeText("uncommitted-refresh")
        assertEquals("secret-token", store.read()!!.accessToken)
        assertTrue(pending.delete())
        assertTrue(pending.mkdir()) // Force FileOutputStream failure before rename.
        try { store.write(SessionCredentials("new", 2, "new@example.com", 999_999, true))
            fail("Replacement unexpectedly succeeded") } catch (_: IOException) { }
        assertEquals("secret-token", store.read()!!.accessToken)
        assertFalse(pending.exists())
        pending.writeText("stale")
        store.clear()
        assertFalse(file.exists()); assertFalse(pending.exists()); assertNull(keys.key)
    }

    @Test fun `invalid partial refresh metadata cannot replace a valid session`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        store.write(credentials)
        for (invalid in listOf(
            SessionCredentials("new", 2, "new@example.com", 999_999, true,
                RefreshCredentials("refresh", 999_999)),
            SessionCredentials("new", 2, "new@example.com", 999_999, true,
                RefreshCredentials("", 999_999), AuthProvider.LOCAL, true),
            SessionCredentials("new", 2, "new@example.com", 999_999, true,
                RefreshCredentials("refresh", 0), AuthProvider.LOCAL, true),
            SessionCredentials("new", 2, "new@example.com", 999_999, true,
                RefreshCredentials("refresh", 999_999), AuthProvider.GOOGLE, true),
        )) {
            try { store.write(invalid); fail("Invalid credentials were accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals("secret-token", store.read()!!.accessToken)
        }
    }

    @Test fun `concurrent saves never combine credentials from different accounts`() = runBlocking {
        val file = File(temp.root, "session")
        val store = EncryptedSessionStore(file, keys)
        (1..20).map { id -> async {
            store.write(SessionCredentials("access-$id", id.toLong(), "$id@example.com", 999_999, true,
                RefreshCredentials("refresh-$id", 2_592_000_000), AuthProvider.LOCAL, true))
            store.read()!!.let {
                assertEquals("access-${it.userId}", it.accessToken)
                assertEquals("refresh-${it.userId}", it.refresh!!.token)
            }
        } }.awaitAll()
        assertFalse(File(temp.root, "session.pending").exists())
    }

    private fun writeLegacy(file: File) {
        val plaintext = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use {
                it.writeUTF("legacy-access"); it.writeLong(1); it.writeUTF("user@example.com")
                it.writeLong(999_999); it.writeBoolean(true)
            }
        }.toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.encryptionKey())
        cipher.updateAAD("jjikmuk-auth-session-v1".toByteArray(Charsets.UTF_8))
        DataOutputStream(file.outputStream()).use {
            it.writeInt(1); it.write(cipher.iv); it.write(cipher.doFinal(plaintext))
        }
    }
}
