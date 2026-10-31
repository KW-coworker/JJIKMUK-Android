package com.coworker.jjikmuk.feature.auth.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Bounded decode, orientation correction and re-encoding remove source metadata before upload. */
class ProfilePhotoEncoder @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun encode(uri: Uri): ProfilePhoto = withContext(Dispatchers.IO) {
        require(uri.scheme == "content")
        val output = ByteArrayOutputStream()
        requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 20 * 1024 * 1024) { "Source image too large" }
                output.write(buffer, 0, count)
            }
        }
        encodeBytes(output.toByteArray())
    }

    internal fun encodeBytes(bytes: ByteArray): ProfilePhoto {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        var bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }))
        try {
            val exif = ByteArrayInputStream(bytes).use { ExifInterface(it) }
            val matrix = Matrix().apply {
                if (exif.isFlipped) postScale(-1f, 1f)
                postRotate(exif.rotationDegrees.toFloat())
            }
            val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (upright !== bitmap) { bitmap.recycle(); bitmap = upright }
            while (true) {
                val output = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                if (output.size() <= ProfilePhoto.MAX_BYTES) return ProfilePhoto(output.toByteArray(), "image/jpeg")
                require(bitmap.width > 1 && bitmap.height > 1)
                val smaller = Bitmap.createScaledBitmap(bitmap, maxOf(1, bitmap.width / 2), maxOf(1, bitmap.height / 2), true)
                bitmap.recycle(); bitmap = smaller
            }
        } finally { bitmap.recycle() }
    }
}
