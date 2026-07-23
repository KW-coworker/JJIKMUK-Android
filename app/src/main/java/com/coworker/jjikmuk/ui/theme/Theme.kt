package com.coworker.jjikmuk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = JjikmukPalette.Primary500,
    onPrimary = JjikmukPalette.White,
    primaryContainer = JjikmukPalette.Primary100,
    onPrimaryContainer = JjikmukPalette.Primary700,
    secondary = JjikmukPalette.Primary600,
    onSecondary = JjikmukPalette.White,
    secondaryContainer = JjikmukPalette.Primary50,
    onSecondaryContainer = JjikmukPalette.Primary700,
    background = JjikmukPalette.White,
    onBackground = JjikmukPalette.Neutral900,
    surface = JjikmukPalette.White,
    onSurface = JjikmukPalette.Neutral900,
    surfaceVariant = JjikmukPalette.Neutral50,
    onSurfaceVariant = JjikmukPalette.Neutral600,
    outline = JjikmukPalette.Neutral300,
    outlineVariant = JjikmukPalette.Neutral200,
    error = JjikmukPalette.Error,
    onError = JjikmukPalette.White,
    errorContainer = JjikmukPalette.Warning,
    onErrorContainer = JjikmukPalette.Neutral900,
)

private val LocalJjikmukColors = staticCompositionLocalOf { LightJjikmukColors }
private val LocalJjikmukTypography = staticCompositionLocalOf { JjikmukTextStyles }

@Composable
fun JjikmukTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalJjikmukColors provides LightJjikmukColors,
        LocalJjikmukTypography provides JjikmukTextStyles,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = JjikmukMaterialTypography,
            content = content,
        )
    }
}

object JjikmukTheme {
    val colors: JjikmukColors
        @Composable
        @ReadOnlyComposable
        get() = LocalJjikmukColors.current

    val typography: JjikmukTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalJjikmukTypography.current
}
