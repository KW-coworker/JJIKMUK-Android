package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult

data class ProductRecommendationUiState(
    val products: List<ProductSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
)
