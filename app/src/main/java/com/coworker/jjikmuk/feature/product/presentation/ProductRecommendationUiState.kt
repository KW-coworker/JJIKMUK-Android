package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult

data class ProductRecommendationUiState(
    val userName: String = "나",
    val userId: Long? = null,
    val notice: String = "",
    val products: List<ProductSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
)
