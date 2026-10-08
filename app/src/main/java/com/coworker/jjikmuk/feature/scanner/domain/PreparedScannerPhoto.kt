package com.coworker.jjikmuk.feature.scanner.domain

import java.io.File

/** A normalized, static JPEG. Only the image preparer should produce production instances. */
@ConsistentCopyVisibility
data class PreparedScannerPhoto internal constructor(
    val file: File,
    val width: Int,
    val height: Int,
) {
    val mimeType: String get() = "image/jpeg"
    val id: String get() = file.absolutePath

    fun isUploadable(): Boolean = file.isFile && file.length() in 1..MAX_BYTES &&
        width > 0 && height > 0 && width.toLong() * height <= MAX_PIXELS

    companion object {
        const val MAX_BYTES = 10L * 1024 * 1024
        const val MAX_PIXELS = 20_000_000L
    }
}
