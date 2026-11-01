package com.coworker.jjikmuk.feature.auth.presentation.signup

/** Backend API_REFERENCE.md at f49478e: classifications.allergens and filters.data.
 * UI IDs stay unchanged; unknown future values must never be silently omitted.
 */
internal object SignUpConditionMapping {
    private val allergies = listOf(
        "egg", "milk", "soy", "wheat", "pork", "chicken", "shrimp", "crab", "squid",
        "mackerel", "shellfish", "oyster", "mussel", "abalone", "peach", "tomato",
        "peanut", "walnut", "buckwheat", "pine_nut", "sulfites", "sesame", "almond",
        "mustard", "celery", "beef",
    ).associateWith { it }
    private val nutrition = mapOf(
        SignUpCondition.LowSugar to "lowSugar",
        SignUpCondition.LowSodium to "lowSodium",
        SignUpCondition.GlutenFree to "glutenFree",
        SignUpCondition.LowCalorie to "lowCalorie",
        SignUpCondition.LowFat to "lowFat",
        SignUpCondition.HighProtein to "highProtein",
    )
    private val vegetarian = mapOf(
        VegetarianDiet.Vegan to "vegan",
        VegetarianDiet.Lacto to "lactoVegetarian",
        VegetarianDiet.Ovo to "ovoVegetarian",
        VegetarianDiet.LactoOvo to "lactoOvoVegetarian",
        VegetarianDiet.Pesco to "pescatarian",
        VegetarianDiet.Pollo to "pollotarian",
    )

    fun encode(state: SignUpUiState): Conditions? {
        val selectedAllergies = if (state.hasAllergyCondition) state.allergies else emptySet()
        if (selectedAllergies.any { it !in allergies }) return null
        val diet = if (state.hasVegetarianCondition) vegetarian[state.vegetarianDiet] ?: return null else null
        val selectedNutrition = state.selectedConditions - setOf(SignUpCondition.Allergy, SignUpCondition.Vegetarian)
        if (selectedNutrition.any { it !in nutrition }) return null
        return Conditions(
            allergies.filterKeys { it in selectedAllergies }.values.joinToString(",").ifEmpty { null },
            (listOfNotNull(diet) + nutrition.filterKeys { it in selectedNutrition }.values)
                .joinToString(",").ifEmpty { null },
        )
    }

    /** Refuse unknown server values instead of silently clearing them on full submission. */
    fun decode(allergyCsv: String?, dietCsv: String?): SignUpUiState? {
        fun values(csv: String?) = csv.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val selectedAllergies = values(allergyCsv)
        val selectedDiet = values(dietCsv)
        if (selectedAllergies.any { it !in allergies } ||
            selectedDiet.any { it !in nutrition.values && it !in vegetarian.values }) return null
        val vegetarians = vegetarian.filterValues { it in selectedDiet }.keys
        if (vegetarians.size > 1) return null
        val selected = nutrition.filterValues { it in selectedDiet }.keys.toMutableSet()
        if (selectedAllergies.isNotEmpty()) selected += SignUpCondition.Allergy
        if (vegetarians.isNotEmpty()) selected += SignUpCondition.Vegetarian
        return SignUpUiState(selectedConditions = selected, allergies = selectedAllergies,
            vegetarianDiet = vegetarians.singleOrNull())
    }

    data class Conditions(val allergies: String?, val specialDiet: String?)
}
