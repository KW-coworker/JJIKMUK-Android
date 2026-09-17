package com.coworker.jjikmuk.feature.auth.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAnimatedLogo
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SPLASH_LOGO_TEXT_GAP, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surface),
    ) {
        JjikmukAnimatedLogo(
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(SPLASH_LOGO_SIZE),
        )
        Text(
            text = stringResource(R.string.app_name),
            color = JjikmukTheme.colors.brand,
            style = JjikmukTheme.typography.brandLogo,
        )
    }
}

private val SPLASH_LOGO_SIZE = 89.dp
private val SPLASH_LOGO_TEXT_GAP = 5.dp

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    JjikmukTheme {
        SplashScreen()
    }
}
