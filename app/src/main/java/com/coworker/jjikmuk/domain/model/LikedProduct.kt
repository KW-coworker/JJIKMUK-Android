package com.coworker.jjikmuk.domain.model

data class LikedProduct(
    val barcode: String,
    val productName: String,
    val brandName: String,
    val imageUrl: String?,
    val productId: Long,
    val isAvailable: Boolean = true,
)
