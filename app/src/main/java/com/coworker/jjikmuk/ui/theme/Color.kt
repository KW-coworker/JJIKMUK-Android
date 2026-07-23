package com.coworker.jjikmuk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 디자인 원본과 1:1로 대응하는 원시 컬러입니다.
 *
 * 화면에서는 팔레트를 직접 사용하지 않고 [JjikmukTheme.colors]의 역할 기반
 * 컬러를 사용합니다.
 */
internal object JjikmukPalette {
    val Primary700 = Color(0xFF0F6B28)
    val Primary600 = Color(0xFF138932)
    val Primary500 = Color(0xFF16A635)
    val Primary200 = Color(0xFFB8EAB5)
    val Primary100 = Color(0xFFDDF6DA)
    val Primary50 = Color(0xFFF3FBF2)

    val Neutral900 = Color(0xFF101825)
    val Neutral600 = Color(0xFF667085)
    val Neutral400 = Color(0xFF98A2B3)
    val Neutral350 = Color(0xFFBDBDBD)
    val Neutral300 = Color(0xFFD0D5DD)
    val Neutral200 = Color(0xFFEAECF0)
    val Neutral100 = Color(0xFFF2F4F7)
    val Neutral50 = Color(0xFFF9FAFB)
    val White = Color(0xFFFFFFFF)

    val Info = Color(0xFFB7D4FF)
    val Warning = Color(0xFFFEF8F8)
    val Pin = Color(0xFF7DAA64)
    val Error = Color(0xFFFF6B6B)
    val Edit = Color(0xFF4D84FF)
}

@Immutable
data class JjikmukColors(
    val brand: Color,
    val brandStrong: Color,
    val brandPressed: Color,
    val brandSubtle: Color,
    val brandSubtler: Color,
    val brandSubtlest: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val border: Color,
    val borderSubtle: Color,
    val disabled: Color,
    val info: Color,
    val warning: Color,
    val error: Color,
    val pin: Color,
    val edit: Color,
)

internal val LightJjikmukColors = JjikmukColors(
    brand = JjikmukPalette.Primary500,
    brandStrong = JjikmukPalette.Primary600,
    brandPressed = JjikmukPalette.Primary700,
    brandSubtle = JjikmukPalette.Primary200,
    brandSubtler = JjikmukPalette.Primary100,
    brandSubtlest = JjikmukPalette.Primary50,
    textPrimary = JjikmukPalette.Neutral900,
    textSecondary = JjikmukPalette.Neutral600,
    textTertiary = JjikmukPalette.Neutral400,
    textDisabled = JjikmukPalette.Neutral350,
    background = JjikmukPalette.White,
    surface = JjikmukPalette.White,
    surfaceSecondary = JjikmukPalette.Neutral50,
    border = JjikmukPalette.Neutral300,
    borderSubtle = JjikmukPalette.Neutral200,
    disabled = JjikmukPalette.Neutral100,
    info = JjikmukPalette.Info,
    warning = JjikmukPalette.Warning,
    error = JjikmukPalette.Error,
    pin = JjikmukPalette.Pin,
    edit = JjikmukPalette.Edit,
)
