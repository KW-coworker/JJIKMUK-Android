package com.coworker.jjikmuk.feature.auth.session

import kotlinx.coroutines.Dispatchers
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
    override suspend fun read(): SessionCredentials? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            require(file.length() in 30..MAX_FILE_BYTES)
            DataInputStream(file.inputStream()).use { input ->
                require(input.readInt() == FORMAT_VERSION)
                val iv = ByteArray(12).also(input::readFully)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keys.decryptionKey(), GCMParameterSpec(128, iv))
                cipher.updateAAD(AAD)
                val plaintext = cipher.doFinal(input.readBytes())
                try {
                    DataInputStream(ByteArrayInputStream(plaintext)).use { data ->
                        val session = SessionCredentials(data.readUTF(), data.readLong(), data.readUTF(),
                            data.readLong(), data.readBoolean())
                        require(data.available() == 0 && session.accessToken.isNotBlank() &&
                            session.userId > 0 && session.email.isNotBlank() && session.expiresAtMillis > 0)
                        session
                    }
                } finally { plaintext.fill(0) }
            }
        } catch (failure: GeneralSecurityException) {
            throw IOException("Session could not be decrypted", failure)
        } catch (failure: IllegalArgumentException) {
            throw IOException("Invalid stored session", failure)
        }
    }

    override suspend fun write(credentials: SessionCredentials) = withContext(Dispatchers.IO) {
        check(file.parentFile?.isDirectory == true || file.parentFile?.mkdirs() == true)
        val plaintext = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { data ->
                data.writeUTF(credentials.accessToken)
                data.writeLong(credentials.userId)
                data.writeUTF(credentials.email)
                data.writeLong(credentials.expiresAtMillis)
                data.writeBoolean(credentials.profileCompleted)
            }
        }.toByteArray()
        val pending = File(file.parentFile, "${file.name}.pending")
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keys.encryptionKey()) // Provider generates a fresh IV.
            require(cipher.iv.size == 12)
            cipher.updateAAD(AAD)
            val encrypted = cipher.doFinal(plaintext)
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
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        // Deleting the key also prevents reuse of ciphertext if file deletion fails.
        try { keys.deleteKey() } finally {
            File(file.parentFile, "${file.name}.pending").delete()
            if (file.exists() && !file.delete()) throw IOException("Session deletion failed")
        }
    }

    companion object {
        private const val FORMAT_VERSION = 1
        private const val MAX_FILE_BYTES = 128 * 1024L
        private val AAD = "jjikmuk-auth-session-v1".toByteArray(Charsets.UTF_8)
    }
}
