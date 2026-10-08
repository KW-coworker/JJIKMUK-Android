package com.coworker.jjikmuk.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.coworker.jjikmuk.R
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Opaque backing also prevents comparison-card borders showing through transparent images. */
@Composable
fun ScannerProductImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    @DrawableRes localImageRes: Int? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Box(modifier.background(Color.White), contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrBlank()) {
            val url = imageUrl.toHttpUrlOrNull()?.takeIf { it.username.isEmpty() && it.password.isEmpty() }
            if (url == null) {
                ProductImageUnavailable()
            } else {
                SubcomposeAsyncImage(
                    model = url.toString(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } },
                    error = { ProductImageUnavailable() },
                )
            }
        } else if (localImageRes != null) {
            Image(painterResource(localImageRes), contentDescription, Modifier.fillMaxSize(), contentScale = contentScale)
        } else {
            ProductImageUnavailable()
        }
    }
}

@Composable
private fun ProductImageUnavailable() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_product_image_upload), "상품 이미지 없음",
            modifier = Modifier.size(28.dp), tint = Color(0xFF98A2B3))
    }
}
