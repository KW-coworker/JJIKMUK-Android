package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ProductApi
import com.coworker.jjikmuk.data.remote.dto.ProductDetailDataDto
import com.coworker.jjikmuk.data.remote.dto.ProductSearchItemDto
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductMacroPercents
import com.coworker.jjikmuk.domain.model.ProductNutrition
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.domain.repository.ProductRepository
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productApi: ProductApi,
) : ProductRepository {

    override suspend fun searchProducts(keyword: String): Result<List<ProductSearchResult>> =
        runCatching {
            productApi.searchProducts(keyword = keyword)
                .data
                .orEmpty()
                .mapNotNull { item -> item.toDomain() }
                .sortedBySimilarity(keyword)
        }

    override suspend fun getProductDetail(barcode: String): Result<ProductDetail> =
        runCatching {
            val detailData = productApi.getProductDetail(barcode = barcode).data
                ?: error("상품 상세 정보를 찾을 수 없어요.")

            detailData.toDomain()
        }

    private fun ProductSearchItemDto.toDomain(): ProductSearchResult? {
        val product = product ?: return null
        val productName = product.productName?.takeIf(String::isNotBlank) ?: return null

        return ProductSearchResult(
            barcode = product.barcode,
            productName = productName,
            brandName = product.manufacturer
                ?.split("/", ",", "(", "[")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: "브랜드 정보 없음",
            imageUrl = product.imageUrl,
            allergyLabels = product.allergy.toAllergyLabels(),
        )
    }

    private fun ProductDetailDataDto.toDomain(): ProductDetail {
        val product = product ?: error("상품 상세 정보를 찾을 수 없어요.")
        val productName = product.productName?.takeIf(String::isNotBlank)
            ?: product.cleanProductName?.takeIf(String::isNotBlank)
            ?: error("상품명이 없는 상품이에요.")

        return ProductDetail(
            barcode = product.barcode.orEmpty(),
            productName = productName,
            brandName = product.manufacturer
                ?.split("/", ",", "(", "[")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: "브랜드 정보 없음",
            imageUrl = product.imageUrl,
            allergyLabels = product.allergy.toAllergyLabels(),
            allergyWarning = product.allergyWarning,
            rawMaterials = product.rawMaterials,
            totalWeight = product.totalWeight,
            nutrition = ProductNutrition(
                energyKcal = product.energyKcal,
                carbsG = product.carbsG,
                proteinG = product.proteinG,
                fatG = product.fatG,
                sugarG = product.sugarG,
                sodiumMg = product.sodiumMg,
                cholesterolMg = product.cholesterolMg,
            ),
            macroPercents = ProductMacroPercents(
                carbs = nutrientPercents?.carbsMacroPercent ?: product.carbsPercent,
                protein = nutrientPercents?.proteinMacroPercent ?: product.proteinPercent,
                fat = nutrientPercents?.fatMacroPercent ?: product.fatPercent,
            ),
            analysisMessage = analysis?.message,
            isDangerous = analysis?.isDangerous == true,
        )
    }

    private fun String?.toAllergyLabels(): List<String> {
        return orEmpty()
            .split(",", "/", "|", " ")
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_ALLERGY_LABEL_COUNT)
    }

    private fun List<ProductSearchResult>.sortedBySimilarity(
        keyword: String,
    ): List<ProductSearchResult> {
        val normalizedKeyword = keyword.trim().lowercase()

        return sortedWith(
            compareBy<ProductSearchResult> { product ->
                product.productName.similarityRank(normalizedKeyword)
            }.thenBy { product ->
                product.productName.length
            }.thenBy { product ->
                product.productName
            },
        )
    }

    private fun String.similarityRank(keyword: String): Int {
        val normalizedName = lowercase()
        val index = normalizedName.indexOf(keyword)

        return when {
            normalizedName == keyword -> 0
            normalizedName.startsWith(keyword) -> 1
            index >= 0 -> 2 + index
            else -> Int.MAX_VALUE
        }
    }

    private companion object {
        const val MAX_ALLERGY_LABEL_COUNT = 3
    }
}
