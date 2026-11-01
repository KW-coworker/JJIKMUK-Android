package com.coworker.jjikmuk.feature.auth.photo

import android.graphics.Bitmap
import android.net.Uri
import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import com.coworker.jjikmuk.feature.auth.domain.normalizeAuthEmail
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingSignupPhoto(val id: String, val userId: Long?, val sending: Boolean, val photo: ProfilePhoto)

interface PendingSignupPhotoStore {
    suspend fun read(email: String): PendingSignupPhoto?
    suspend fun stage(email: String, photo: ProfilePhoto): String
    suspend fun update(email: String, record: PendingSignupPhoto)
    suspend fun remove(email: String, id: String)
}

/** App-private no_backup directory. One bounded, atomic file per normalized email; no credentials. */
class FilePendingSignupPhotoStore(private val directory: File) : PendingSignupPhotoStore {
    private val lock = Any()
    private fun file(email: String): File {
        val normalized = normalizeAuthEmail(email)
        require(normalized.isNotBlank())
        val name = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$name.photo")
    }
    override suspend fun read(email: String): PendingSignupPhoto? = withContext(Dispatchers.IO) {
        synchronized(lock) { readFile(file(email)) }
    }
    private fun readFile(file: File): PendingSignupPhoto? {
        if (!file.exists()) return null
        require(file.length() in 1..(ProfilePhoto.MAX_BYTES + 256).toLong())
        return DataInputStream(FileInputStream(file)).use { input ->
            require(input.readInt() == 1)
            val id = input.readUTF(); require(id.length == 36)
            val userId = input.readLong().takeIf { it > 0 }
            val sending = input.readBoolean()
            val count = input.readInt(); require(count in 1..ProfilePhoto.MAX_BYTES)
            val bytes = ByteArray(count); input.readFully(bytes); require(input.read() == -1)
            PendingSignupPhoto(id, userId, sending, ProfilePhoto(bytes, "image/jpeg"))
        }
    }
    override suspend fun stage(email: String, photo: ProfilePhoto): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            require(photo.contentType == "image/jpeg" && photo.bytes.size in 1..ProfilePhoto.MAX_BYTES)
            val id = UUID.randomUUID().toString()
            write(file(email), PendingSignupPhoto(id, null, false, photo)); id
        }
    }
    override suspend fun update(email: String, record: PendingSignupPhoto) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val file = file(email)
            check(readFile(file)?.id == record.id) { "Draft changed" }
            write(file, record)
        }
    }
    override suspend fun remove(email: String, id: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val file = file(email)
            if (readFile(file)?.id == id) check(file.delete()) { "Cannot remove draft" }
        }
    }
    private fun write(target: File, record: PendingSignupPhoto) {
        check(directory.isDirectory || directory.mkdirs())
        val temporary = File.createTempFile("pending-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { stream ->
                val output = DataOutputStream(stream)
                output.writeInt(1); output.writeUTF(record.id); output.writeLong(record.userId ?: -1)
                output.writeBoolean(record.sending); output.writeInt(record.photo.bytes.size); output.write(record.photo.bytes)
                output.flush(); stream.fd.sync()
            }
            check(temporary.renameTo(target)) { "Cannot save draft" }
        } finally { temporary.delete() }
    }
}

interface SignupPhotoPreparation {
    suspend fun stage(email: String, uri: String?, bitmap: Bitmap?): String
    suspend fun confirm(email: String, id: String, userId: Long)
    suspend fun discard(email: String, id: String)
    object Unavailable : SignupPhotoPreparation {
        override suspend fun stage(email: String, uri: String?, bitmap: Bitmap?): String = error("Photo storage unavailable")
        override suspend fun confirm(email: String, id: String, userId: Long) = Unit
        override suspend fun discard(email: String, id: String) = Unit
    }
}

class AndroidSignupPhotoPreparation @Inject constructor(
    private val encoder: ProfilePhotoEncoder, private val store: PendingSignupPhotoStore,
) : SignupPhotoPreparation {
    override suspend fun stage(email: String, uri: String?, bitmap: Bitmap?): String {
        val photo = if (bitmap != null) encoder.encode(bitmap) else encoder.encode(Uri.parse(requireNotNull(uri)))
        return store.stage(email, photo)
    }
    override suspend fun confirm(email: String, id: String, userId: Long) {
        val record = store.read(email) ?: return
        if (record.id == id) store.update(email, PendingSignupPhoto(id, userId, record.sending, record.photo))
    }
    override suspend fun discard(email: String, id: String) = store.remove(email, id)
}
