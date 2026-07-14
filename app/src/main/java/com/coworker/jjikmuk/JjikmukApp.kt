package com.coworker.jjikmuk

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Text(
            text = "Hello JJIKMUK!",
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukAppPreview() {
    JjikmukTheme {
        JjikmukApp()
    }
}
