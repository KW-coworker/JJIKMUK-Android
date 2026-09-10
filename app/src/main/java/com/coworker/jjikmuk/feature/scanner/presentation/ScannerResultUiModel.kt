package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.coworker.jjikmuk.R

enum class ScannerResultStatus {
    Safe,
    Warning,
}

data class ScannerResultUiModel(
    val status: ScannerResultStatus,
    val productName: String,
    @DrawableRes val productImageRes: Int,
    val nutrients: List<NutrientUiModel>,
)

data class NutrientUiModel(
    val type: NutrientType,
    val value: String,
)

enum class NutrientType(
    val label: String,
    val unit: String,
    @DrawableRes val iconRes: Int,
    val iconBackground: Color,
    val valueColor: Color,
) {
    Calories("칼로리", "kcal", R.drawable.ic_nutrient_calories, Color(0xFFFFF7ED), Color(0xFFFB923C)),
    Carbohydrate("탄수화물", "g", R.drawable.ic_nutrient_carbohydrate, Color(0xFFFFFBEB), Color(0xFFF59E0B)),
    Sugars("당류", "g", R.drawable.ic_nutrient_sugars, Color(0xFFFDF2F8), Color(0xFFEC4899)),
    Fat("지방", "g", R.drawable.ic_nutrient_fat, Color(0xFFFEFCE8), Color(0xFFEAB308)),
    SaturatedFat("포화지방", "g", R.drawable.ic_nutrient_saturated_fat, Color(0xFFFFF7ED), Color(0xFFFB923C)),
    TransFat("트랜스지방", "g", R.drawable.ic_nutrient_trans_fat, Color(0xFFFFF7ED), Color(0xFFFB923C)),
    ProteinEgg("단백질", "g", R.drawable.ic_nutrient_protein_egg, Color(0xFFF0FDF4), Color(0xFF22C55E)),
    Protein("단백질", "g", R.drawable.ic_nutrient_protein, Color(0xFFF0FDF4), Color(0xFF22C55E)),
    Sodium("나트륨", "mg", R.drawable.ic_nutrient_sodium, Color(0xFFEFF6FF), Color(0xFF3B82F6)),
    Cholesterol("콜레스테롤", "mg", R.drawable.ic_nutrient_cholesterol, Color(0xFFFEF2F2), Color(0xFFEF4444)),
    DietaryFiber("식이섬유", "g", R.drawable.ic_nutrient_dietary_fiber, Color(0xFFF0FDF4), Color(0xFF22C55E)),
    Calcium("칼슘", "mg", R.drawable.ic_nutrient_calcium, Color(0xFFF8FAFC), Color(0xFF64748B)),
    Caffeine("카페인", "mg", R.drawable.ic_nutrient_caffeine, Color(0xFFFAF5FF), Color(0xFFA855F7)),
    SugarAlcohol("당알코올", "g", R.drawable.ic_nutrient_sugar_alcohol, Color(0xFFF0FDFA), Color(0xFF14B8A6)),
}

fun sampleScannerResult(status: ScannerResultStatus) = ScannerResultUiModel(
    status = status,
    productName = "해태 포키 블루베리",
    productImageRes = R.drawable.img_scanner_sample_product,
    nutrients = if (status == ScannerResultStatus.Safe) {
        listOf(
            NutrientUiModel(NutrientType.Calories, "500"),
            NutrientUiModel(NutrientType.Carbohydrate, "62"),
            NutrientUiModel(NutrientType.Sugars, "3"),
            NutrientUiModel(NutrientType.Fat, "25"),
        )
    } else {
        // 백엔드 연결 전 아이콘/레이아웃 검증을 위한 테스트 조합입니다.
        listOf(
            NutrientUiModel(NutrientType.ProteinEgg, "12"),
            NutrientUiModel(NutrientType.Calcium, "240"),
            NutrientUiModel(NutrientType.Caffeine, "35"),
            NutrientUiModel(NutrientType.DietaryFiber, "8"),
        )
    },
)
