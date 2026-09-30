package com.coworker.jjikmuk.domain.model

data class ProductDetail(
    val barcode: String,
    val productName: String,
    val brandName: String,
    val imageUrl: String?,
    val allergyLabels: List<String>,
    val allergyWarning: String?,
    val rawMaterials: String?,
    val totalWeight: String?,
    val nutrition: ProductNutrition,
    val macroPercents: ProductMacroPercents,
    val safetyStatus: ProductSafetyStatus,
    val dangerousIngredients: List<String>,
    val analysisMessage: String?,
    val isDangerous: Boolean,
)

enum class ProductSafetyStatus {
    Pass,
    Unknown,
    Danger,
}

data class ProductNutrition(
    val energyKcal: Double?,
    val carbsG: Double?,
    val proteinG: Double?,
    val fatG: Double?,
    val sugarG: Double?,
    val sodiumMg: Double?,
    val cholesterolMg: Double?,
)

data class ProductMacroPercents(
    val carbs: Double?,
    val protein: Double?,
    val fat: Double?,
)
