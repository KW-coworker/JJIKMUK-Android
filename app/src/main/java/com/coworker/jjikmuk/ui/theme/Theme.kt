package com.coworker.jjikmuk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = White,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary700,
    secondary = Primary600,
    onSecondary = White,
    secondaryContainer = Primary50,
    onSecondaryContainer = Primary700,
    background = White,
    onBackground = Neutral900,
    surface = White,
    onSurface = Neutral900,
    surfaceVariant = Neutral50,
    onSurfaceVariant = Neutral600,
    outline = Neutral300,
    outlineVariant = Neutral200,
    error = Error,
    onError = White,
    errorContainer = Warning,
    onErrorContainer = Neutral900,
)

@Composable
fun JjikmukTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
