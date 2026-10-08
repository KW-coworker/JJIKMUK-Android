package com.coworker.jjikmuk.feature.scanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import com.coworker.jjikmuk.feature.scanner.domain.PreparedScannerPhoto
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ScannerImagePreparer @Inject constructor() {
    /** Caller owns input/output files and calls from an IO dispatcher. Never alters the source. */
    suspend fun prepare(source: File, destination: File): PreparedScannerPhoto {
        require(source != destination)
        require(source.length() in 1..ScannerPhotoSession.MAX_IMPORT_BYTES) { "Invalid source size" }
        currentCoroutineContext().ensureActive()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image" }
        require(bounds.outMimeType in SUPPORTED_TYPES) { "Unsupported image type" }
        requireStaticImage(source, bounds.outMimeType)

        // Sample before allocating. Four bytes per pixel, at most ~16MiB per working bitmap.
        var sample = 1
        while ((bounds.outWidth.toLong() + sample - 1) / sample > MAX_EDGE ||
            (bounds.outHeight.toLong() + sample - 1) / sample > MAX_EDGE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = requireNotNull(BitmapFactory.decodeFile(source.absolutePath, options))
        var oriented: Bitmap? = null
        var opaque: Bitmap? = null
        try {
            currentCoroutineContext().ensureActive()
            val exif = ExifInterface(source)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = orientationMatrix(orientation)
            oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            opaque = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(oriented, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            }
            for (quality in listOf(92, 82, 72, 60)) {
                currentCoroutineContext().ensureActive()
                destination.outputStream().use { require(opaque.compress(Bitmap.CompressFormat.JPEG, quality, it)) }
                if (destination.length() <= PreparedScannerPhoto.MAX_BYTES) break
            }
            // Fresh JPEG strips source EXIF, including orientation/GPS, so rotation is never applied twice.
            val result = PreparedScannerPhoto(destination, opaque.width, opaque.height)
            require(result.isUploadable()) { "Prepared image exceeds upload limits" }
            val verify = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, verify)
            require(verify.outMimeType == "image/jpeg" && verify.outWidth == result.width && verify.outHeight == result.height)
            currentCoroutineContext().ensureActive()
            return result
        } finally {
            opaque?.recycle()
            if (oriented !== decoded) oriented?.recycle()
            decoded.recycle()
        }
    }

    internal fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }

    private fun requireStaticImage(file: File, mime: String) {
        RandomAccessFile(file, "r").use { input ->
            when (mime) {
                "image/png" -> {
                    input.seek(8)
                    while (input.filePointer + 12 <= input.length()) {
                        val length = input.readInt().toLong() and 0xffffffffL
                        val type = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
                        require(type != "acTL") { "Animated PNG is not supported" }
                        val next = input.filePointer + length + 4
                        require(next <= input.length()) { "Invalid PNG chunk" }
                        if (type == "IEND") break
                        input.seek(next)
                    }
                }
                "image/webp" -> {
                    input.seek(12)
                    while (input.filePointer + 8 <= input.length()) {
                        val type = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
                        val length = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                        val start = input.filePointer
                        require(type != "ANIM" && type != "ANMF") { "Animated WebP is not supported" }
                        if (type == "VP8X" && length > 0) require(input.readUnsignedByte() and 2 == 0)
                        val next = start + length + (length and 1)
                        require(next <= input.length()) { "Invalid WebP chunk" }
                        input.seek(next)
                    }
                }
                "image/heif", "image/heic" -> {
                    val size = input.readInt().toLong() and 0xffffffffL
                    val type = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
                    require(type == "ftyp" && size in 16..4096 && size <= input.length())
                    val brands = ByteArray((size - 8).toInt()).also(input::readFully)
                    brands.toList().chunked(4).forEachIndexed { index, bytes ->
                        if (index != 1) require(bytes.toByteArray().toString(Charsets.US_ASCII) !in SEQUENCE_BRANDS)
                    }
                }
            }
        }
    }

    companion object {
        const val MAX_EDGE = 2048
        private val SUPPORTED_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/heif", "image/heic")
        private val SEQUENCE_BRANDS = setOf("msf1", "hevc", "hevx", "avis")
    }
}
