package com.coworker.jjikmuk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * JJIKMUK 디자인 시스템의 의미 기반 타이포그래피 토큰입니다.
 *
 * UI 토큰은 한글 UI의 기본 서체인 Pretendard를 사용합니다. 같은 토큰을 영문,
 * 날짜, 시간 또는 숫자에 사용할 때는 [TextStyle.asEnglish]로 Inter를 적용합니다.
 */
@Immutable
data class JjikmukTypography(
    val brandLogo: TextStyle,
    val section: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val labelS: TextStyle,
    val caption: TextStyle,
) {
    // 기존 `JjikmukTheme.typography.brand` 사용처와의 소스 호환용 별칭입니다.
    val brand: TextStyle
        get() = brandLogo
}

/**
 * 영문 UI, 날짜, 시간, 숫자에 디자인 시스템의 Inter 서체를 적용합니다.
 *
 * 예: `JjikmukTheme.typography.bodyM.asEnglish()`
 */
fun TextStyle.asEnglish(): TextStyle = copy(fontFamily = InterFontFamily)

internal val JjikmukTextStyles = JjikmukTypography(
    brandLogo = designTextStyle(
        fontFamily = Baloo2FontFamily,
        weight = FontWeight.ExtraBold,
        fontSize = 40,
        lineHeightPercent = 120,
    ),
    section = designTextStyle(
        fontFamily = Baloo2FontFamily,
        weight = FontWeight.ExtraBold,
        fontSize = 34,
        lineHeightPercent = 120,
    ),
    h1 = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 24,
        lineHeightPercent = 130,
        letterSpacingPercent = -1.0,
    ),
    h2 = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 20,
        lineHeightPercent = 140,
        letterSpacingPercent = -0.5,
    ),
    h3 = designTextStyle(
        weight = FontWeight.SemiBold,
        fontSize = 18,
        lineHeightPercent = 144,
        letterSpacingPercent = -0.25,
    ),
    titleL = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 16,
        lineHeightPercent = 150,
    ),
    titleM = designTextStyle(
        weight = FontWeight.SemiBold,
        fontSize = 14,
        lineHeightPercent = 138,
    ),
    bodyL = designTextStyle(
        weight = FontWeight.Normal,
        fontSize = 16,
        lineHeightPercent = 150,
    ),
    bodyM = designTextStyle(
        weight = FontWeight.Normal,
        fontSize = 14,
        lineHeightPercent = 143,
    ),
    bodyS = designTextStyle(
        weight = FontWeight.Normal,
        fontSize = 13,
        lineHeightPercent = 138,
    ),
    labelL = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 16,
        lineHeightPercent = 125,
    ),
    labelM = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 14,
        lineHeightPercent = 129,
    ),
    labelS = designTextStyle(
        weight = FontWeight.Bold,
        fontSize = 12,
        lineHeightPercent = 133,
    ),
    caption = designTextStyle(
        weight = FontWeight.Medium,
        fontSize = 11,
        lineHeightPercent = 127,
    ),
)

/**
 * 기존 Material Typography 접근도 동일한 디자인 토큰을 사용하도록 매핑합니다.
 * Caption은 Material 3에 대응 슬롯이 없어 [JjikmukTypography.caption]으로 사용합니다.
 */
internal val JjikmukMaterialTypography = Typography(
    displayLarge = JjikmukTextStyles.brandLogo,
    displayMedium = JjikmukTextStyles.section,
    displaySmall = JjikmukTextStyles.h1,
    headlineLarge = JjikmukTextStyles.h1,
    headlineMedium = JjikmukTextStyles.h2,
    headlineSmall = JjikmukTextStyles.h3,
    titleLarge = JjikmukTextStyles.titleL,
    titleMedium = JjikmukTextStyles.titleM,
    titleSmall = JjikmukTextStyles.titleM,
    bodyLarge = JjikmukTextStyles.bodyL,
    bodyMedium = JjikmukTextStyles.bodyM,
    bodySmall = JjikmukTextStyles.bodyS,
    labelLarge = JjikmukTextStyles.labelL,
    labelMedium = JjikmukTextStyles.labelM,
    labelSmall = JjikmukTextStyles.labelS,
)

private fun designTextStyle(
    weight: FontWeight,
    fontSize: Int,
    lineHeightPercent: Int,
    letterSpacingPercent: Double = 0.0,
    fontFamily: FontFamily = PretendardFontFamily,
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = (fontSize * lineHeightPercent / 100f).sp,
    letterSpacing = (letterSpacingPercent / 100).em,
)
