package com.coworker.jjikmuk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class JjikmukTypography(
    val brand: TextStyle,
)

internal val JjikmukTextStyles = JjikmukTypography(
    brand = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
    ),
)

/**
 * Material 이름을 그대로 사용해 모든 화면에서
 * `MaterialTheme.typography.bodyLarge` 형태로 접근합니다.
 */
internal val JjikmukMaterialTypography = Typography(
    displayLarge = textStyle(FontWeight.Normal, 57, 64, -0.25),
    displayMedium = textStyle(FontWeight.Normal, 45, 52),
    displaySmall = textStyle(FontWeight.Normal, 36, 44),
    headlineLarge = textStyle(FontWeight.Normal, 32, 40),
    headlineMedium = textStyle(FontWeight.Normal, 28, 36),
    headlineSmall = textStyle(FontWeight.Normal, 24, 32),
    titleLarge = textStyle(FontWeight.Normal, 22, 28),
    titleMedium = textStyle(FontWeight.Medium, 16, 24, 0.15),
    titleSmall = textStyle(FontWeight.Medium, 14, 20, 0.1),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = textStyle(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = textStyle(FontWeight.Normal, 12, 16, 0.4),
    labelLarge = textStyle(FontWeight.Medium, 14, 20, 0.1),
    labelMedium = textStyle(FontWeight.Medium, 12, 16, 0.5),
    labelSmall = textStyle(FontWeight.Medium, 11, 16, 0.5),
)

private fun textStyle(
    weight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)
