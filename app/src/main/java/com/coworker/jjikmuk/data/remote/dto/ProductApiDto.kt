package com.coworker.jjikmuk.data.remote.dto

data class ProductSearchApiResponse(
    val message: String? = null,
    val data: List<ProductSearchItemDto>? = emptyList(),
)

data class ProductDetailApiResponse(
    val message: String? = null,
    val data: ProductDetailDataDto? = null,
)

data class ProductDetailDataDto(
    val product: ProductDto? = null,
    val nutrientPercents: ProductNutrientPercentsDto? = null,
    val analysis: ProductAnalysisDto? = null,
)

data class ProductSearchItemDto(
    val product: ProductDto? = null,
    val analysis: ProductAnalysisDto? = null,
)

data class ProductDto(
    val productId: Long? = null,
    val barcode: String? = null,
    val productName: String? = null,
    val manufacturer: String? = null,
    val reportNo: String? = null,
    val allergy: String? = null,
    val nutrientText: String? = null,
    val imageUrl: String? = null,
    val source: String? = null,
    val rawMaterials: String? = null,
    val energyKcal: Double? = null,
    val carbsG: Double? = null,
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val sugarG: Double? = null,
    val sodiumMg: Double? = null,
    val cholesterolMg: Double? = null,
    val allergyWarning: String? = null,
    val cleanProductName: String? = null,
    val totalWeight: String? = null,
    val nutrition: ProductNutritionDto? = null,
    val carbsPercent: Double? = null,
    val proteinPercent: Double? = null,
    val fatPercent: Double? = null,
    val sodiumG: Double? = null,
    val cholesterolG: Double? = null,
)

data class ProductNutrientPercentsDto(
    val energyPercent: Int? = null,
    val carbsPercent: Int? = null,
    val proteinPercent: Int? = null,
    val fatPercent: Int? = null,
    val sugarPercent: Int? = null,
    val sodiumPercent: Int? = null,
    val cholesterolPercent: Int? = null,
    val carbsMacroPercent: Double? = null,
    val proteinMacroPercent: Double? = null,
    val fatMacroPercent: Double? = null,
)

data class ProductAnalysisDto(
    val status: String? = null,
    val isDangerous: Boolean? = null,
    val dangerousIngredients: List<String>? = emptyList(),
    val verificationRequired: Boolean? = null,
    val message: String? = null,
)

/** nutrition-v1: all numbers below are already converted by the server. */
data class ProductNutritionDto(
    val sourceBasis: ProductNutritionBasisDto? = null,
    val sourceValues: ProductNutrientValuesDto? = null,
    val packageQuantity: ProductNutritionQuantityDto? = null,
    val per100g: ProductNutritionPortionDto? = null,
    val per100ml: ProductNutritionPortionDto? = null,
    val perPackage: ProductNutritionPortionDto? = null,
    val packageCalculationStatus: String? = null,
    val packageCalculationReason: String? = null,
)

data class ProductNutritionPortionDto(
    val amount: Double? = null,
    val unit: String? = null,
    val values: ProductNutrientValuesDto? = null,
)

data class ProductNutrientValuesDto(
    val energyKcal: Double? = null,
    val carbsG: Double? = null,
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val sugarG: Double? = null,
    val sodiumMg: Double? = null,
    val cholesterolMg: Double? = null,
)

data class ProductNutritionBasisDto(val status: String? = null)
data class ProductNutritionQuantityDto(val amount: Double? = null, val unit: String? = null)
