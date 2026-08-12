package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductDetail

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductDetail? = null,
    val errorMessage: String? = null,
)
