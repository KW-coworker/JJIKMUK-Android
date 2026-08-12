package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.domain.model.ProductDetail

interface ProductRepository {
    suspend fun searchProducts(keyword: String): Result<List<ProductSearchResult>>
    suspend fun getProductDetail(barcode: String): Result<ProductDetail>
}
