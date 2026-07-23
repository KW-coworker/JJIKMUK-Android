package com.coworker.jjikmuk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.feature.chat.presentation.ChatRoute
import com.coworker.jjikmuk.feature.home.presentation.HomeScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukApp() {
    var chatMessage by rememberSaveable { mutableStateOf<String?>(null) }

    if (chatMessage == null) {
        HomeScreen(
            onSendMessage = { message ->
                chatMessage = message
            },
        )
    } else {
        ChatRoute(
            initialMessage = chatMessage.orEmpty(),
            onBackClick = {
                chatMessage = null
            },
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
