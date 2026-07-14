package com.coworker.jjikmuk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Inter),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Inter),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Inter),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Inter),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Inter),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Inter),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Inter),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Inter),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Inter),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Inter),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Inter),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Inter),
)
