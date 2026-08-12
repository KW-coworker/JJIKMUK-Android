package com.coworker.jjikmuk.domain.model

data class ProductSearchResult(
    val barcode: String?,
    val productName: String,
    val brandName: String,
    val imageUrl: String?,
    val allergyLabels: List<String>,
)
