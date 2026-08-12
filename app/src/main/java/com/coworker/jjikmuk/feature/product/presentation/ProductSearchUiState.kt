package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult

data class ProductSearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val products: List<ProductSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)
