package com.coworker.jjikmuk.feature.scanner.data

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScannerPhotoSessionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `selected photo survives callback until replaced or session ends`() {
        val session = ScannerPhotoSession(temporary.root)
        val first = session.copyFrom(byteArrayOf(1, 2).inputStream())
        assertTrue(session.select(first))
        assertTrue(first.exists())
        val replacement = session.copyFrom(byteArrayOf(3, 4).inputStream())
        assertTrue(session.select(replacement))
        assertFalse(first.exists())
        assertTrue(replacement.exists())
        session.close()
        assertFalse(replacement.exists())
    }

    @Test fun `failed import keeps previous selection and deletes partial file`() {
        val session = ScannerPhotoSession(temporary.root)
        val original = session.copyFrom(byteArrayOf(1).inputStream())
        session.select(original)
        val failing = object : InputStream() {
            override fun read(): Int = throw IOException("provider disconnected")
        }
        assertThrows(IOException::class.java) { session.copyFrom(failing) }
        assertTrue(original.exists())
        assertEquals(listOf(original), original.parentFile!!.listFiles()!!.toList())
    }

    @Test fun `late capture after exit cannot resurrect or retain a photo`() {
        val session = ScannerPhotoSession(temporary.root)
        val pending = session.createFile()
        session.close()
        pending.parentFile!!.mkdirs() // Simulate a camera callback finishing after navigation away.
        pending.writeBytes(byteArrayOf(1))
        assertFalse(session.select(pending))
        assertFalse(pending.exists())
        assertFalse(pending.parentFile!!.exists())
    }

    @Test fun `one session cannot delete another session or gallery original`() {
        val first = ScannerPhotoSession(temporary.root)
        val original = temporary.newFile("gallery.png").apply { writeBytes(byteArrayOf(1)) }
        val owned = original.inputStream().use(first::copyFrom)
        first.select(owned)
        val second = ScannerPhotoSession(temporary.root)
        assertFalse(second.select(owned))
        second.discard(original)
        second.close()
        assertTrue(owned.exists())
        assertTrue(original.exists())
        first.clearSelection()
        assertFalse(owned.exists())
        assertTrue(original.exists())
    }

    @Test fun `oversized import is bounded and removes partial output`() {
        val session = ScannerPhotoSession(temporary.root)
        val oversized = ByteArray(ScannerPhotoSession.MAX_IMPORT_BYTES.toInt() + 1).inputStream()
        assertThrows(PhotoTooLargeException::class.java) { session.copyFrom(oversized) }
        assertEquals(0, temporary.root.walkTopDown().count { it.isFile })
    }

    @Test fun `empty image is not accepted`() {
        val session = ScannerPhotoSession(temporary.root)
        assertThrows(IllegalStateException::class.java) { session.copyFrom(byteArrayOf().inputStream()) }
        assertEquals(0, temporary.root.walkTopDown().count { it.isFile })
    }

    @Test fun `abandoned previous process files are reclaimed without deleting other caches`() {
        val abandoned = File(temporary.root, "scanner_photos/${UUID.randomUUID()}_${UUID.randomUUID()}")
        abandoned.mkdirs()
        File(abandoned, "photo.jpg").writeBytes(byteArrayOf(1))
        val profile = temporary.newFolder("images")
        val profilePhoto = File(profile, "profile.jpg").apply { writeBytes(byteArrayOf(1)) }
        ScannerPhotoSession(temporary.root)
        assertFalse(abandoned.exists())
        assertTrue(profilePhoto.exists())
    }
}
