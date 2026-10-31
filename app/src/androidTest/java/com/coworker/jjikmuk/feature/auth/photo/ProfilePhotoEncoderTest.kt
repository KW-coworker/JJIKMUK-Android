package com.coworker.jjikmuk.feature.auth.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePhotoEncoderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val encoder = ProfilePhotoEncoder(context)
    private fun image(width: Int, height: Int, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try { ByteArrayOutputStream().also { bitmap.compress(format, 90, it) }.toByteArray() }
        finally { bitmap.recycle() }
    }
    @Test fun largeSourceIsDownsampledAndProducesBoundedJpeg() {
        val result = encoder.encodeBytes(image(4096, 2048))
        assertEquals("image/jpeg", result.contentType)
        assertTrue(result.bytes.size <= ProfilePhoto.MAX_BYTES)
        val bitmap = BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)
        assertTrue(bitmap.width <= 2048 && bitmap.height <= 2048)
        assertEquals(2, bitmap.width / bitmap.height)
        bitmap.recycle()
    }
    @Test fun rotationIsAppliedAndLocationMetadataIsRemoved() {
        val file = File.createTempFile("photo-encoder-test", ".jpg", context.cacheDir)
        try {
            file.writeBytes(image(80, 40, Bitmap.CompressFormat.JPEG))
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setLatLong(37.0, 127.0)
                saveAttributes()
            }
            val result = encoder.encodeBytes(file.readBytes())
            val bitmap = BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)
            assertEquals(40, bitmap.width); assertEquals(80, bitmap.height); bitmap.recycle()
            val exif = ByteArrayInputStream(result.bytes).use { ExifInterface(it) }
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertEquals(0, exif.rotationDegrees)
        } finally { file.delete() }
    }
    @Test fun invalidSourceIsRejected() {
        try { encoder.encodeBytes("not an image".toByteArray()); fail("Invalid image accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
