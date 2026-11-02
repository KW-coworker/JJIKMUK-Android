package com.coworker.jjikmuk.data.repository

/** The legacy family editor uses display labels; wire values always use canonical server IDs. */
internal object FamilyConditions {
    val allergies = linkedMapOf(
        "egg" to "계란", "milk" to "우유", "soy" to "대두", "wheat" to "밀", "pork" to "돼지고기",
        "chicken" to "닭고기", "shrimp" to "새우", "crab" to "게", "squid" to "오징어", "mackerel" to "고등어",
        "shellfish" to "조개류", "oyster" to "굴", "mussel" to "홍합", "abalone" to "전복", "peach" to "복숭아",
        "tomato" to "토마토", "peanut" to "땅콩", "walnut" to "호두", "buckwheat" to "메밀", "pine_nut" to "잣",
        "sulfites" to "아황산류", "sesame" to "참깨", "almond" to "아몬드", "mustard" to "머스타드", "celery" to "셀러리", "beef" to "소고기")
    private val vegetarian = linkedMapOf("vegan" to "비건", "lactoVegetarian" to "락토", "ovoVegetarian" to "오보",
        "lactoOvoVegetarian" to "락토오보", "pescatarian" to "페스코", "pollotarian" to "폴로")
    private val preferences = linkedMapOf("lowSodium" to "저염", "glutenFree" to "글루텐프리", "lowCalorie" to "저칼로리",
        "lowSugar" to "저당", "highProtein" to "고단백", "lowFat" to "저지방")
    data class Decoded(val vegetarian: String, val allergies: Set<String>, val preferences: Set<String>, val supported: Boolean)
    fun decode(allergyCsv: String?, dietCsv: String?): Decoded {
        fun split(csv: String?) = csv.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).toSet()
        val a = split(allergyCsv); val d = split(dietCsv)
        val diets = vegetarian.filterKeys { it in d }.values
        return Decoded(diets.singleOrNull() ?: "해당 없음", a.map { allergies[it] ?: it }.toSet(),
            preferences.filterKeys { it in d }.values.toSet(),
            a.all { it in allergies } && d.all { it in vegetarian || it in preferences } && diets.size <= 1)
    }
    fun encode(vegetarianLabel: String, allergyLabels: Set<String>, preferenceLabels: Set<String>): Pair<String?, String?>? {
        if (allergyLabels.any { it !in allergies.values } || preferenceLabels.any { it !in preferences.values }) return null
        val diet = if (vegetarianLabel in setOf("", "해당 없음")) null
            else vegetarian.entries.firstOrNull { it.value == vegetarianLabel }?.key ?: return null
        return allergies.filterValues { it in allergyLabels }.keys.joinToString(",").ifEmpty { null } to
            (listOfNotNull(diet) + preferences.filterValues { it in preferenceLabels }.keys).joinToString(",").ifEmpty { null }
    }
}
