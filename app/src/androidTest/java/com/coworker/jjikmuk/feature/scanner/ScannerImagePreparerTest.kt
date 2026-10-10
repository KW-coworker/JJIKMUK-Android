package com.coworker.jjikmuk.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.scanner.data.ScannerImagePreparer
import java.io.File
import java.util.UUID
import java.util.zip.CRC32
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerImagePreparerTest {
    private lateinit var directory: File
    private val preparer = ScannerImagePreparer()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "image_preparer_test_${UUID.randomUUID()}").apply { mkdirs() }
    }
    @After fun tearDown() { directory.deleteRecursively() }

    @Test fun allExifOrientationsAreBakedIntoPixelsOnceAndMetadataIsRemoved() = runBlocking {
        withContext(Dispatchers.IO) {
            val expectedTopLeft = listOf(Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE,
                Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN)
            for (orientation in 1..8) {
                val source = pattern("source-$orientation.jpg", Bitmap.CompressFormat.JPEG)
                ExifInterface(source).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    setLatLong(37.0, 127.0)
                    saveAttributes()
                }
                val original = source.readBytes()
                val output = File(directory, "result-$orientation.jpg")
                val result = preparer.prepare(source, output)
                assertEquals(if (orientation >= 5) 80 else 120, result.width)
                assertEquals(if (orientation >= 5) 120 else 80, result.height)
                val decoded = BitmapFactory.decodeFile(output.absolutePath)
                val pixel = decoded.getPixel(decoded.width / 4, decoded.height / 4)
                val expected = expectedTopLeft[orientation - 1]
                assertTrue(abs(Color.red(pixel) - Color.red(expected)) < 25)
                assertTrue(abs(Color.green(pixel) - Color.green(expected)) < 25)
                assertTrue(abs(Color.blue(pixel) - Color.blue(expected)) < 25)
                decoded.recycle()
                val exif = ExifInterface(output)
                assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
                // Fresh JPEG may expose the library's default UNDEFINED (0), also meaning no transform.
                val outputOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                assertTrue(outputOrientation in setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL))
                assertArrayEquals(original, source.readBytes())
            }
        }
    }

    @Test fun largePngIsSampledBeforeDecodeAndEncodedAsBoundedJpeg() = runBlocking {
        withContext(Dispatchers.IO) {
            val source = File(directory, "large.png")
            val bitmap = Bitmap.createBitmap(5000, 4100, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.GREEN)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val result = preparer.prepare(source, File(directory, "bounded.jpg"))
            assertTrue(result.isUploadable())
            assertTrue(result.width <= ScannerImagePreparer.MAX_EDGE)
            assertTrue(result.height <= ScannerImagePreparer.MAX_EDGE)
            assertTrue(abs(result.width.toDouble() / result.height - 5000.0 / 4100) < 0.01)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(result.file.absolutePath, bounds)
            assertEquals("image/jpeg", bounds.outMimeType)
        }
    }

    @Test fun transparentPngGetsWhiteBackingInsteadOfBlack() = runBlocking {
        withContext(Dispatchers.IO) {
            val source = File(directory, "transparent.png")
            val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val result = preparer.prepare(source, File(directory, "white.jpg"))
            val decoded = BitmapFactory.decodeFile(result.file.absolutePath)
            assertTrue(Color.red(decoded.getPixel(40, 40)) > 245)
            assertTrue(Color.green(decoded.getPixel(40, 40)) > 245)
            assertTrue(Color.blue(decoded.getPixel(40, 40)) > 245)
            decoded.recycle()
        }
    }

    @Suppress("DEPRECATION")
    @Test fun webpBytesAreActuallyConvertedNotJustRenamed() = runBlocking {
        withContext(Dispatchers.IO) {
            val source = pattern("input.jpg", Bitmap.CompressFormat.WEBP)
            val result = preparer.prepare(source, File(directory, "output.jpg"))
            assertEquals("RIFF", source.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII))
            assertEquals(0xff, result.file.readBytes()[0].toInt() and 0xff)
            assertEquals(0xd8, result.file.readBytes()[1].toInt() and 0xff)
            assertTrue(result.isUploadable())
        }
    }

    @Test fun animatedPngHeaderIsRejectedRatherThanSendingFirstFrame() = runBlocking {
        withContext(Dispatchers.IO) {
            val source = pattern("animated.png", Bitmap.CompressFormat.PNG)
            val png = source.readBytes()
            val payload = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)
            val data = "acTL".toByteArray() + payload
            val crc = CRC32().apply { update(data) }.value
            val crcBytes = byteArrayOf((crc shr 24).toByte(), (crc shr 16).toByte(), (crc shr 8).toByte(), crc.toByte())
            source.writeBytes(png.take(33).toByteArray() + byteArrayOf(0, 0, 0, 8) + data + crcBytes + png.drop(33).toByteArray())
            try {
                preparer.prepare(source, File(directory, "rejected.jpg"))
                fail("animated input accepted")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Animated PNG"))
            }
        }
    }

    @Test fun corruptImageFailsWithoutChangingOriginal() = runBlocking {
        withContext(Dispatchers.IO) {
            val source = File(directory, "corrupt.jpg").apply { writeText("not an image") }
            try {
                preparer.prepare(source, File(directory, "invalid.jpg"))
                fail("corrupt input accepted")
            } catch (_: IllegalArgumentException) {
                assertEquals("not an image", source.readText())
            }
        }
    }

    private fun pattern(name: String, format: Bitmap.CompressFormat): File {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW).forEachIndexed { i, color ->
            paint.color = color
            val left = (i % 2 * 60).toFloat()
            val top = (i / 2 * 40).toFloat()
            canvas.drawRect(left, top, left + 60, top + 40, paint)
        }
        val file = File(directory, name)
        file.outputStream().use { bitmap.compress(format, 100, it) }
        bitmap.recycle()
        return file
    }
}
