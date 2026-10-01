package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.domain.model.ProductDetail

interface ProductRepository {
    suspend fun searchProducts(
        keyword: String,
        allergies: List<String> = emptyList(),
    ): Result<List<ProductSearchResult>>

    suspend fun getSafeRecommendations(
        filters: List<String> = emptyList(),
        allergies: List<String> = emptyList(),
        limit: Int = 6,
    ): Result<List<ProductSearchResult>>

    suspend fun getProductDetail(
        barcode: String,
        allergies: List<String> = emptyList(),
    ): Result<ProductDetail>
}
