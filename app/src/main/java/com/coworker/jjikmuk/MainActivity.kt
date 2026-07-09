package com.coworker.jjikmuk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.ui.theme.JJIKMUKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JJIKMUKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JjikmukApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun JjikmukApp(modifier: Modifier = Modifier) {
    Text(
        text = "Hello JJIKMUK!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun JjikmukAppPreview() {
    JJIKMUKTheme {
        JjikmukApp()
    }
}
