package com.coworker.jjikmuk.feature.auth.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** One small decoded bitmap for all private avatars; never put account photos in a global image cache. */
internal val LocalAccountPhotoPreview = staticCompositionLocalOf<ImageBitmap?> { null }
private data class Preview(val generation: Long, val source: ProfilePhoto, val bitmap: ImageBitmap)

@Composable
fun AccountPhotoPreviewProvider(viewModel: ProfilePhotoViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Readiness is independent of busy/error state: failed GETs must not trigger a retry loop.
    LaunchedEffect(state.generation, state.previewReady) { if (state.previewReady) viewModel.ensureLoaded() }
    val preview by produceState<Preview?>(null, state.generation, state.photo, state.previewReady) {
        value = null
        if (!state.previewReady) return@produceState
        val source = state.photo ?: return@produceState
        val bitmap = withContext(Dispatchers.Default) { decodeAvatarPreview(source.bytes) } ?: return@produceState
        value = Preview(state.generation, source, bitmap.asImageBitmap())
    }
    // Invalidate synchronously on logout/replacement, before the next decoding effect starts.
    val current = preview?.takeIf { it.generation == state.generation && it.source === state.photo && state.previewReady }
    CompositionLocalProvider(LocalAccountPhotoPreview provides current?.bitmap, content = content)
}

@Composable
internal fun AccountAvatarContent(isMe: Boolean, fallback: @Composable () -> Unit) {
    val preview = LocalAccountPhotoPreview.current
    if (isMe && preview != null) Image(bitmap = preview, contentDescription = "내 프로필 사진",
        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    else fallback()
}

/** Bounded background decode; maximum ARGB storage is 160 * 160 * 4 bytes. */
internal fun decodeAvatarPreview(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > 320) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }) ?: return null
    val edge = max(decoded.width, decoded.height)
    if (edge <= 160) return decoded
    val result = Bitmap.createScaledBitmap(decoded, max(1, decoded.width * 160 / edge), max(1, decoded.height * 160 / edge), true)
    if (result !== decoded) decoded.recycle()
    return result
}
