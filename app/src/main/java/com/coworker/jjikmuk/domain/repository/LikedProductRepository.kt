package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.LikedProduct
import kotlinx.coroutines.flow.Flow

data class FavoriteAccount(val userId: Long, val generation: Long)
data class LikedProductsState(val account: FavoriteAccount? = null,
    val products: List<LikedProduct> = emptyList(), val statuses: Map<Long, Boolean> = emptyMap(),
    val pendingIds: Set<Long> = emptySet(), val isLoading: Boolean = false, val message: String? = null)

interface LikedProductRepository {
    fun observeState(): Flow<LikedProductsState>
    fun captureAccount(): FavoriteAccount?
    suspend fun refresh(account: FavoriteAccount? = captureAccount())
    suspend fun refreshStatus(productId: Long, account: FavoriteAccount? = captureAccount())
    suspend fun setFavorite(productId: Long, favorite: Boolean, account: FavoriteAccount?)
}
