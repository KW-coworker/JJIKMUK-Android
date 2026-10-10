package com.coworker.jjikmuk.feature.scanner.data

import java.io.File
import java.io.InputStream
import java.util.UUID

/** Owns only this scanner session's files, never the gallery original or profile images. */
internal class ScannerPhotoSession(cacheDirectory: File) : AutoCloseable {
    private val root = File(cacheDirectory, "scanner_photos")
    private val directory = File(root, "${PROCESS_ID}_${UUID.randomUUID()}")
    private var closed = false
    private var selected: File? = null

    init {
        // A killed process cannot run onCleared. Reclaim its files on the next scanner entry.
        // Sessions in this process may still be active and must be left alone.
        root.listFiles()?.filter {
            it.isDirectory && SESSION_NAME.matches(it.name) && !it.name.startsWith("${PROCESS_ID}_")
        }?.forEach { it.deleteRecursively() }
    }

    @Synchronized
    fun createFile(): File {
        check(!closed)
        check(directory.isDirectory || directory.mkdirs())
        return File.createTempFile("photo_", ".jpg", directory)
    }

    // The suffix is not a format conversion; decoding uses the file contents.
    fun copyFrom(input: InputStream): File {
        val file = createFile()
        try {
            file.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_IMPORT_BYTES) throw PhotoTooLargeException()
                    output.write(buffer, 0, read)
                }
            }
            check(file.length() > 0)
            return file
        } catch (error: Exception) {
            discard(file)
            throw error
        }
    }

    @Synchronized
    fun select(file: File): Boolean {
        if (closed || file.parentFile != directory || !file.isFile || file.length() == 0L) {
            discard(file)
            return false
        }
        if (selected != file) selected?.delete()
        selected = file
        return true
    }

    @Synchronized
    fun discard(file: File) {
        if (file.parentFile == directory) file.delete()
        if (selected == file) selected = null
        if (closed) directory.delete()
    }

    @Synchronized
    fun clearSelection() {
        selected?.delete()
        selected = null
    }

    @Synchronized
    override fun close() {
        closed = true
        selected = null
        directory.deleteRecursively()
    }

    companion object {
        const val MAX_IMPORT_BYTES = 50L * 1024 * 1024
        private val PROCESS_ID = UUID.randomUUID().toString()
        private val SESSION_NAME = Regex("[0-9a-f-]{36}_[0-9a-f-]{36}")
    }
}

internal class PhotoTooLargeException : Exception()
