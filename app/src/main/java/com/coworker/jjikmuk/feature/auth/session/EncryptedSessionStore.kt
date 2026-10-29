package com.coworker.jjikmuk.feature.auth.session

import com.coworker.jjikmuk.feature.auth.domain.AuthProvider
import com.coworker.jjikmuk.feature.auth.domain.RefreshCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionKeyProvider {
    fun encryptionKey(): SecretKey
    fun decryptionKey(): SecretKey
    fun deleteKey()
}

/** Small, versioned binary envelope with authenticated encryption; file lives in noBackupFilesDir. */
class EncryptedSessionStore(
    private val file: File,
    private val keys: SessionKeyProvider,
) : SessionStore {
    private val mutex = Mutex()

    override suspend fun read(): SessionCredentials? = withContext(Dispatchers.IO) { mutex.withLock {
        if (!file.exists()) return@withLock null
        try {
            require(file.length() in 30..MAX_FILE_BYTES)
            DataInputStream(file.inputStream()).use { input ->
                val version = input.readInt()
                require(version == 1 || version == FORMAT_VERSION)
                val iv = ByteArray(12).also(input::readFully)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keys.decryptionKey(), GCMParameterSpec(128, iv))
                cipher.updateAAD(aad(version))
                val plaintext = cipher.doFinal(input.readBytes())
                try {
                    DataInputStream(ByteArrayInputStream(plaintext)).use { data ->
                        val token = data.readUTF()
                        val userId = data.readLong()
                        val email = data.readUTF()
                        val expiresAt = data.readLong()
                        val completed = data.readBoolean()
                        val hasRefresh = version >= 2 && data.readBoolean()
                        val refresh = if (hasRefresh) RefreshCredentials(data.readUTF(), data.readLong()) else null
                        val provider = if (hasRefresh) AuthProvider.valueOf(data.readUTF()) else null
                        val hasPassword = if (hasRefresh) data.readBoolean() else null
                        val session = SessionCredentials(token, userId, email, expiresAt, completed,
                            refresh, provider, hasPassword)
                        require(data.available() == 0)
                        session.validate()
                        session
                    }
                } finally { plaintext.fill(0) }
            }
        } catch (failure: GeneralSecurityException) {
            throw IOException("Session could not be decrypted", failure)
        } catch (failure: IllegalArgumentException) {
            throw IOException("Invalid stored session", failure)
        }
    } }

    override suspend fun write(credentials: SessionCredentials) = withContext(Dispatchers.IO) { mutex.withLock {
        credentials.validate()
        check(file.parentFile?.isDirectory == true || file.parentFile?.mkdirs() == true)
        val plaintext = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { data ->
                data.writeUTF(credentials.accessToken)
                data.writeLong(credentials.userId)
                data.writeUTF(credentials.email)
                data.writeLong(credentials.expiresAtMillis)
                data.writeBoolean(credentials.profileCompleted)
                data.writeBoolean(credentials.refresh != null)
                credentials.refresh?.let { refresh ->
                    data.writeUTF(refresh.token)
                    data.writeLong(refresh.expiresAtMillis)
                    data.writeUTF(requireNotNull(credentials.authProvider).name)
                    data.writeBoolean(requireNotNull(credentials.hasPassword))
                }
            }
        }.toByteArray()
        val pending = File(file.parentFile, "${file.name}.pending")
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keys.encryptionKey()) // Provider generates a fresh IV.
            require(cipher.iv.size == 12)
            cipher.updateAAD(aad(FORMAT_VERSION))
            val encrypted = cipher.doFinal(plaintext)
            require(encrypted.size + 16L <= MAX_FILE_BYTES)
            FileOutputStream(pending).use { output ->
                val data = DataOutputStream(output)
                data.writeInt(FORMAT_VERSION)
                data.write(cipher.iv)
                data.write(encrypted)
                data.flush()
                output.fd.sync()
            }
            if (!pending.renameTo(file)) throw IOException("Session replacement failed")
        } finally {
            plaintext.fill(0)
            pending.delete()
        }
    } }

    override suspend fun clear() = withContext(Dispatchers.IO) { mutex.withLock {
        // Deleting the key also prevents reuse of ciphertext if file deletion fails.
        try { keys.deleteKey() } finally {
            File(file.parentFile, "${file.name}.pending").delete()
            if (file.exists() && !file.delete()) throw IOException("Session deletion failed")
        }
    } }

    companion object {
        private const val FORMAT_VERSION = 2
        private const val MAX_FILE_BYTES = 128 * 1024L
        private fun aad(version: Int) = "jjikmuk-auth-session-v$version".toByteArray(Charsets.UTF_8)
    }
}
