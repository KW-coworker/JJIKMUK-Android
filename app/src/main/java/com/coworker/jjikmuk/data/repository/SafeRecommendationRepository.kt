package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.SafeRecommendationApi
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class RecommendationOwner(val userId: Long, val userName: String)

data class SafeRecommendationPage(
    val userId: Long,
    val products: List<ProductSearchResult>,
    val nextCursor: String?,
    val notice: String,
)

class SafeRecommendationRepository @Inject constructor(private val api: SafeRecommendationApi) {
    /** Opening recommendations must not overwrite the account's saved dietary conditions. */
    suspend fun owner(): RecommendationOwner {
        val me = api.getMe().data ?: error("내 프로필을 확인할 수 없어요.")
        require(me.id > 0 && me.nickname.isNotBlank()) { "내 프로필을 확인할 수 없어요." }
        return RecommendationOwner(me.id, me.nickname)
    }

    suspend fun page(cursor: String? = null, excluded: List<String> = emptyList()): SafeRecommendationPage {
        val data = api.getRecommendations(cursor = cursor, excluded = excluded.takeLast(200)).data
            ?: error("추천 응답을 확인할 수 없어요.")
        check(data.userId > 0) { "추천 사용자를 확인할 수 없어요." }
        val byBarcode = data.categories.flatMap { it.items }.mapNotNull { item ->
            val product = item.product ?: return@mapNotNull null
            val barcode = product.barcode?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = product.productName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            barcode to ProductSearchResult(barcode, name, product.manufacturer.orEmpty(),
                product.imageUrl, emptyList())
        }.toMap()
        check(!data.hasNext || !data.nextCursor.isNullOrBlank()) { "다음 페이지 정보가 없어요." }
        return SafeRecommendationPage(
            userId = data.userId,
            products = data.returnedBarcodes.distinct().mapNotNull(byBarcode::get),
            nextCursor = data.nextCursor?.takeIf { data.hasNext },
            notice = when (data.recommendationMode) {
                "MIXED", "PREFERENCE_RELAXED" -> "알레르기 조건을 유지하고 일부 선호 조건을 완화한 상품이 포함되어 있어요. " + data.notice
                else -> data.notice
            },
        )
    }
}

internal fun Throwable.recommendationMessage(): String {
    if (this is CancellationException) throw this
    return when ((this as? HttpException)?.code()) {
        401 -> "로그인 상태를 확인한 뒤 다시 시도해 주세요."
        403 -> "내 프로필에 접근할 수 없어요. 로그인 계정을 확인해 주세요."
        else -> "추천을 불러오지 못했어요. 서버 연결과 내 식이 조건을 확인한 뒤 다시 시도해 주세요."
    }
}
