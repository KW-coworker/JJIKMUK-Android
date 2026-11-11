package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.dto.*
import com.coworker.jjikmuk.domain.model.ProductNutrition
import com.coworker.jjikmuk.domain.model.ProductNutritionBasis

internal fun ProductDto.toDisplayedNutrition(): ProductNutrition {
    val contract = nutrition
    val packageValues = contract?.perPackage?.takeIf { contract.packageCalculationStatus != "UNAVAILABLE" }
    if (packageValues != null) return packageValues.values.toNutrition(ProductNutritionBasis.Package)

    // API owner confirmed: missing source basis means legacy values are per 100g.
    // Explicitly known bases never use this default; server conversions take precedence.
    val per100g = contract?.per100g?.values
    val per100ml = contract?.per100ml?.values
    val default100g = contract?.sourceBasis?.status != "KNOWN" && per100g == null && per100ml == null
    val values = when {
        per100g != null -> per100g
        per100ml != null -> per100ml
        default100g -> contract?.sourceValues ?: ProductNutrientValuesDto(
            energyKcal, carbsG, proteinG, fatG, sugarG, sodiumMg, cholesterolMg)
        else -> null
    }
    val basis = when {
        per100g != null || default100g -> ProductNutritionBasis.Per100g
        per100ml != null -> ProductNutritionBasis.Per100ml
        else -> ProductNutritionBasis.Unknown
    }
    val quantity = contract?.packageQuantity?.takeIf { it.amount != null && it.amount.isFinite() && it.amount > 0 }
        ?: parsePackageQuantity(totalWeight)
    val compatible = when (basis) {
        ProductNutritionBasis.Per100g -> quantity?.unit.equals("g", ignoreCase = true)
        ProductNutritionBasis.Per100ml -> quantity?.unit.equals("ml", ignoreCase = true)
        else -> false
    }
    return if (compatible) values.toNutrition(ProductNutritionBasis.Package, quantity!!.amount!! / 100.0)
        else values.toNutrition(basis)
}

private fun ProductNutrientValuesDto?.toNutrition(basis: ProductNutritionBasis, factor: Double = 1.0): ProductNutrition {
    fun Double?.scaled() = this?.takeIf { it.isFinite() && it >= 0 }?.times(factor)?.takeIf { it.isFinite() }
    return ProductNutrition(this?.energyKcal.scaled(), this?.carbsG.scaled(), this?.proteinG.scaled(),
        this?.fatG.scaled(), this?.sugarG.scaled(), this?.sodiumMg.scaled(), this?.cholesterolMg.scaled(), basis)
}

// Prefer the server's parsed quantity, especially for multipacks. Parse only unambiguous local labels.
private fun parsePackageQuantity(raw: String?): ProductNutritionQuantityDto? {
    val match = Regex("""^\s*(\d+(?:\.\d+)?)\s*(kg|mg|g|ml|l)\s*$""", RegexOption.IGNORE_CASE)
        .matchEntire(raw.orEmpty()) ?: return null
    val amount = match.groupValues[1].toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    return when (match.groupValues[2].lowercase(java.util.Locale.ROOT)) {
        "kg" -> ProductNutritionQuantityDto(amount * 1000, "g")
        "mg" -> ProductNutritionQuantityDto(amount / 1000, "g")
        "l" -> ProductNutritionQuantityDto(amount * 1000, "ml")
        "ml" -> ProductNutritionQuantityDto(amount, "ml")
        else -> ProductNutritionQuantityDto(amount, "g")
    }.takeIf { it.amount?.isFinite() == true }
}
