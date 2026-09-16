package com.coworker.jjikmuk.ui.catalog

import androidx.annotation.StringRes
import com.coworker.jjikmuk.R

/**
 * 앱 전역에서 사용하는 알레르기 표시 정보.
 *
 * [id]는 저장/API 연동에 사용할 안정적인 문자열이고, [icon]은 화면에 표시할
 * Unicode 이모지다. 사용자에게 보이는 이름은 [labelRes]를 통해 가져온다.
 */
enum class FoodAllergy(
    val id: String,
    val icon: String,
    @StringRes val labelRes: Int,
) {
    EGG("egg", "🥚", R.string.allergy_egg),
    MILK("milk", "🥛", R.string.allergy_milk),
    SOY("soy", "🫘", R.string.allergy_soy),
    WHEAT("wheat", "🌾", R.string.allergy_wheat),
    PORK("pork", "🥓", R.string.allergy_pork),
    CHICKEN("chicken", "🍗", R.string.allergy_chicken),
    SHRIMP("shrimp", "🦐", R.string.allergy_shrimp),
    CRAB("crab", "🦀", R.string.allergy_crab),
    SQUID("squid", "🦑", R.string.allergy_squid),
    MACKEREL("mackerel", "🐟", R.string.allergy_mackerel),
    SHELLFISH("shellfish", "🐚", R.string.allergy_shellfish),
    OYSTER("oyster", "🦪", R.string.allergy_oyster),
    MUSSEL("mussel", "🦪", R.string.allergy_mussel),
    ABALONE("abalone", "🐚", R.string.allergy_abalone),
    PEACH("peach", "🍑", R.string.allergy_peach),
    TOMATO("tomato", "🍅", R.string.allergy_tomato),
    PEANUT("peanut", "🥜", R.string.allergy_peanut),
    WALNUT("walnut", "🌰", R.string.allergy_walnut),
    BUCKWHEAT("buckwheat", "🍜", R.string.allergy_buckwheat),
    PINE_NUT("pine_nut", "🫘", R.string.allergy_pine_nut),
    SULFITES("sulfites", "🧪", R.string.allergy_sulfites),
    SESAME("sesame", "🧂", R.string.allergy_sesame),
    ALMOND("almond", "🫘", R.string.allergy_almond),
    MUSTARD("mustard", "🍯", R.string.allergy_mustard),
    CELERY("celery", "🥒", R.string.allergy_celery),
    BEEF("beef", "🥩", R.string.allergy_beef),
    ;

    companion object {
        private val entriesById = entries.associateBy(FoodAllergy::id)

        fun fromId(id: String): FoodAllergy? = entriesById[id]
    }
}

/** 앱 전역에서 사용하는 식이조건 및 채식 세부유형 표시 정보. */
enum class DietaryCondition(
    val id: String,
    val icon: String,
    @StringRes val labelRes: Int,
) {
    LOW_SUGAR("low_sugar", "📉", R.string.dietary_condition_low_sugar),
    LOW_SODIUM("low_sodium", "🧂", R.string.dietary_condition_low_sodium),
    VEGAN("vegan", "🥗", R.string.dietary_condition_vegan),
    LOW_CALORIE("low_calorie", "🏃", R.string.dietary_condition_low_calorie),
    GLUTEN_FREE("gluten_free", "🍞", R.string.dietary_condition_gluten_free),
    LOW_FAT("low_fat", "🥑", R.string.dietary_condition_low_fat),
    HIGH_PROTEIN("high_protein", "💪", R.string.dietary_condition_high_protein),
    LACTO("lacto", "🥗", R.string.dietary_condition_lacto),
    OVO("ovo", "🥗", R.string.dietary_condition_ovo),
    LACTO_OVO("lacto_ovo", "🥗", R.string.dietary_condition_lacto_ovo),
    PESCO("pesco", "🥗", R.string.dietary_condition_pesco),
    POLLO("pollo", "🥗", R.string.dietary_condition_pollo),
    ;

    companion object {
        private val entriesById = entries.associateBy(DietaryCondition::id)

        fun fromId(id: String): DietaryCondition? = entriesById[id]
    }
}
