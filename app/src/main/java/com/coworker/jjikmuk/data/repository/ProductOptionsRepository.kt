package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ProductOptionsApi
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import javax.inject.Inject

data class ProductCapacity(val key: String, val label: String, val amount: Double?, val unit: String?, val product: ProductDetail)
data class ProductCapacities(val selectedKey: String, val options: List<ProductCapacity>)
data class ProductAlternative(val product: ProductSearchResult, val needsVerification: Boolean)
data class ProductAlternatives(val products: List<ProductAlternative>, val notice: String)

class ProductOptionsRepository @Inject constructor(private val api: ProductOptionsApi, private val mapper: ProductRepositoryImpl) {
    suspend fun capacities(barcode: String): ProductCapacities {
        val data = api.variants(barcode).data ?: error("중량 정보를 확인할 수 없어요.")
        return ProductCapacities(data.selectedOptionKey, data.options.mapNotNull { option ->
            val detail = option.data ?: return@mapNotNull null
            ProductCapacity(option.key, option.label, option.amount, option.unit, mapper.mapDetail(detail))
        })
    }
    suspend fun alternatives(barcode: String): ProductAlternatives {
        val data = api.alternatives(barcode).data ?: error("대체 상품 응답을 확인할 수 없어요.")
        val items = (data.items + data.fallbackItems).mapNotNull { item ->
            val p = item.product ?: return@mapNotNull null
            val code = p.barcode?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = p.productName?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (code == barcode || item.safety?.status !in listOf("PASS", "UNKNOWN")) return@mapNotNull null
            ProductAlternative(ProductSearchResult(code, name, p.manufacturer.orEmpty(), p.imageUrl, emptyList()),
                item.verificationRequired || item.safety?.status != "PASS")
        }.distinctBy { it.product.barcode }
        return ProductAlternatives(items, data.notice.orEmpty())
    }
}
