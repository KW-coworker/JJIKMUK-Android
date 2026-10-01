package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.LikedProduct
import com.coworker.jjikmuk.domain.model.ProductDetail
import kotlinx.coroutines.flow.Flow

interface LikedProductRepository {
    fun observeLikedProducts(): Flow<List<LikedProduct>>
    fun observeLikedProductBarcodes(): Flow<Set<String>>
    suspend fun toggleLikedProduct(product: ProductDetail)
}
