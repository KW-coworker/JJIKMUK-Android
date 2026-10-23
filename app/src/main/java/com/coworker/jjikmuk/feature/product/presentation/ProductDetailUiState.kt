package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.domain.model.ProductDetail

data class ProductDetailUiState(
    val referenceBarcode: String? = null,
    val selectedCapacityKey: String = "",
    val capacities: List<com.coworker.jjikmuk.data.repository.ProductCapacity> = emptyList(),
    val capacitiesLoading: Boolean = false,
    val capacityError: String? = null,
    val alternatives: List<com.coworker.jjikmuk.data.repository.ProductAlternative> = emptyList(),
    val alternativesLoading: Boolean = false,
    val alternativesError: String? = null,
    val alternativesNotice: String = "",
    val isLoading: Boolean = false,
    val product: ProductDetail? = null,
    val errorMessage: String? = null,
)
