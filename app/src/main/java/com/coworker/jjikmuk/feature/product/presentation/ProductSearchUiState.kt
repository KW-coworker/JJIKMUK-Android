package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult

data class ProductSearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val products: List<ProductSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val categoryId: String? = null,
    val categoryLabel: String? = null,
    val totalCount: Int? = null,
    val categoryAllergyFilterApplied: Boolean = false,
    val page: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextPageError: String? = null,
) {
    val isEmptyResult: Boolean
        get() = hasSearched && !isLoading && errorMessage == null && products.isEmpty()
}
