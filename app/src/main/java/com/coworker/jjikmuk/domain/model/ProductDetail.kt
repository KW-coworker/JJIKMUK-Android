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
    val productId: Long? = null,
    val personalConditionsApplied: Boolean = true,
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
    val basis: ProductNutritionBasis = ProductNutritionBasis.Unknown,
) {
    val packageBasisKnown: Boolean get() = basis == ProductNutritionBasis.Package
    val basisLabel: String get() = when (basis) {
        ProductNutritionBasis.Package -> "상품 전체 용량 기준"
        ProductNutritionBasis.Per100g -> "100g 기준"
        ProductNutritionBasis.Per100ml -> "100ml 기준"
        ProductNutritionBasis.Unknown -> "영양정보 기준량 확인 불가"
    }
    val energyLabel: String get() = when (basis) {
        ProductNutritionBasis.Package -> "총열량"
        ProductNutritionBasis.Per100g -> "100g 기준 열량"
        ProductNutritionBasis.Per100ml -> "100ml 기준 열량"
        ProductNutritionBasis.Unknown -> "열량"
    }
}

enum class ProductNutritionBasis { Package, Per100g, Per100ml, Unknown }

data class ProductMacroPercents(
    val carbs: Double?,
    val protein: Double?,
    val fat: Double?,
)
