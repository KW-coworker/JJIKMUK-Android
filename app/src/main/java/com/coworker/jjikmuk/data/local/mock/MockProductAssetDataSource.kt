package com.coworker.jjikmuk.data.local.mock

import android.content.Context
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductMacroPercents
import com.coworker.jjikmuk.domain.model.ProductNutrition
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockProductAssetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun searchProducts(keyword: String): List<ProductSearchResult>? {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return null

        return products()
            .filter { product ->
                product.matchesKeyword(normalizedKeyword)
            }
            .mapNotNull { product -> product.toSearchResult() }
            .sortedBySimilarity(normalizedKeyword)
            .ifEmpty { null }
    }

    fun getProductDetail(barcode: String): ProductDetail? {
        return products()
            .firstOrNull { product -> product.getString("barcode") == barcode }
            ?.toProductDetail()
    }

    private fun products(): List<JsonObject> {
        val root = runCatching {
            context.assets.open(PRODUCT_DUMP_MOCK).bufferedReader().use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }
        }.getOrNull() ?: return emptyList()

        return root.get("products")
            ?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?.mapNotNull { element -> element.asObjectOrNull() }
            .orEmpty()
    }

    private fun JsonObject.matchesKeyword(keyword: String): Boolean {
        return listOf(
            getString("productName"),
            getString("cleanProductName"),
            getString("manufacturer"),
            getString("searchKeywords"),
        ).any { value ->
            value?.contains(keyword, ignoreCase = true) == true
        }
    }

    private fun JsonObject.toSearchResult(): ProductSearchResult? {
        val productName = getString("productName")?.takeIf(String::isNotBlank) ?: return null

        return ProductSearchResult(
            barcode = getString("barcode"),
            productName = productName,
            brandName = getDisplayManufacturer(),
            imageUrl = getString("imageUrl"),
            allergyLabels = toAllergyLabels(),
        )
    }

    private fun JsonObject.toProductDetail(): ProductDetail? {
        val productName = getString("productName")?.takeIf(String::isNotBlank) ?: return null
        val nutrition = getObject("nutrition")

        return ProductDetail(
            barcode = getString("barcode").orEmpty(),
            productName = productName,
            brandName = getDisplayManufacturer(),
            imageUrl = getString("imageUrl"),
            allergyLabels = toAllergyLabels(),
            allergyWarning = getString("allergyWarning"),
            rawMaterials = getString("rawMaterials"),
            totalWeight = getString("totalWeight"),
            nutrition = ProductNutrition(
                energyKcal = nutrition?.getDouble("energyKcal"),
                carbsG = nutrition?.getDouble("carbsG"),
                proteinG = nutrition?.getDouble("proteinG"),
                fatG = nutrition?.getDouble("fatG"),
                sugarG = nutrition?.getDouble("sugarG"),
                sodiumMg = nutrition?.getDouble("sodiumMg"),
                cholesterolMg = nutrition?.getDouble("cholesterolMg"),
            ),
            macroPercents = ProductMacroPercents(
                carbs = nutrition?.getDouble("carbsPercent"),
                protein = nutrition?.getDouble("proteinPercent"),
                fat = nutrition?.getDouble("fatPercent"),
            ),
            analysisMessage = null,
            isDangerous = false,
        )
    }

    private fun JsonObject.getDisplayManufacturer(): String {
        return getString("manufacturer")
            ?.split("/", ",", "(", "[")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: "브랜드 정보 없음"
    }

    private fun JsonObject.toAllergyLabels(): List<String> {
        val allergyClassification = getString("allergyClassification")
            ?.takeIf { value -> value != "정보없음" && value != "미검출" }
        val allergy = getString("allergy")

        return (allergyClassification ?: allergy)
            .orEmpty()
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

    private fun JsonObject.getObject(key: String): JsonObject? {
        return get(key)?.asObjectOrNull()
    }

    private fun JsonObject.getString(key: String): String? {
        return get(key)?.asStringOrNull()
    }

    private fun JsonObject.getDouble(key: String): Double? {
        return get(key)?.takeUnless(JsonElement::isJsonNull)?.asDouble
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? {
        return takeIf { element -> element.isJsonObject }?.asJsonObject
    }

    private fun JsonElement.asStringOrNull(): String? {
        return takeUnless(JsonElement::isJsonNull)?.asString
    }

    private companion object {
        const val PRODUCT_DUMP_MOCK = "mock/product/product_dump.mock.json"
        const val MAX_ALLERGY_LABEL_COUNT = 3
    }
}
