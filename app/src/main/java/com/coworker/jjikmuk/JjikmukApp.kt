package com.coworker.jjikmuk

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.feature.auth.navigation.AuthNavHost
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukApp() {
    AuthNavHost()
}

@Preview(showBackground = true)
@Composable
private fun JjikmukAppPreview() {
    JjikmukTheme {
        JjikmukApp()
    }
}
