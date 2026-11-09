package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.dto.ProductSearchItemDto
import com.coworker.jjikmuk.di.PublicCatalogRetrofit
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import javax.inject.Inject
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

data class ProductCategory(val id: String, val label: String)
data class CategoryPage(val products: List<ProductSearchResult>, val page: Int, val total: Int, val hasNext: Boolean)
internal data class ClassificationsEnvelope(val data: ClassificationsData? = null)
internal data class ClassificationsData(val foodCategories: List<ProductCategory>? = null)
internal data class CategoryProductsEnvelope(val data: CategoryProductsData? = null)
internal data class CategoryProductsData(val items: List<ProductSearchItemDto>? = null, val page: Int? = null,
    val totalElements: Int? = null, val hasNext: Boolean? = null)
internal interface ProductCatalogApi {
    @GET("api/products/classifications") suspend fun classifications(): ClassificationsEnvelope
    @GET("api/products/filter") suspend fun filter(@Query("categories") category: String,
        @Query("keyword") keyword: String?, @Query("page") page: Int, @Query("size") size: Int = 20,
        @Query("analysisMode") mode: String = "none"): CategoryProductsEnvelope
}

class ProductCatalogRepository internal constructor(private val api: ProductCatalogApi, private val mapper: ProductRepositoryImpl) {
    @Inject constructor(@PublicCatalogRetrofit retrofit: Retrofit, mapper: ProductRepositoryImpl) : this(retrofit.create(ProductCatalogApi::class.java), mapper)
    suspend fun categories(): List<ProductCategory> {
        val values = api.classifications().data?.foodCategories ?: error("카테고리를 불러오지 못했어요.")
        check(values.all { it.id.isNotBlank() && it.label.isNotBlank() })
        return values.distinctBy { it.id }
    }
    suspend fun page(category: String, keyword: String, page: Int): CategoryPage {
        val data = api.filter(category, keyword.trim().takeIf { it.isNotEmpty() }, page).data
            ?: error("상품 목록을 불러오지 못했어요.")
        check(data.page == page && data.totalElements != null && data.totalElements >= 0 && data.hasNext != null)
        val items = requireNotNull(data.items).mapNotNull { mapper.mapSearchItem(it) }
        check(!data.hasNext || items.isNotEmpty()) { "다음 상품 목록을 확인할 수 없어요." }
        return CategoryPage(items, page, data.totalElements, data.hasNext)
    }
}


internal fun Throwable.catalogLoadMessage(): String = when (this) {
    is java.net.SocketTimeoutException -> "서버 응답이 늦어지고 있어요. 잠시 후 다시 시도해 주세요."
    is retrofit2.HttpException -> when (code()) {
        401, 403 -> "상품 조회 권한을 확인하지 못했어요. 다시 시도해 주세요."
        in 500..599 -> "서버 오류로 상품을 불러오지 못했어요. 잠시 후 다시 시도해 주세요."
        else -> "상품 요청을 처리하지 못했어요. 다시 시도해 주세요."
    }
    is java.io.IOException -> "서버 연결이 끊어졌어요. 연결을 확인한 뒤 다시 시도해 주세요."
    else -> "상품 응답을 확인하지 못했어요. 다시 시도해 주세요."
}
