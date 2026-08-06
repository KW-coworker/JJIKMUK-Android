package com.coworker.jjikmuk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.coworker.jjikmuk.feature.chat.presentation.ChatRoute
import com.coworker.jjikmuk.feature.home.presentation.HomeScreen

@Composable
fun JjikmukAppContent() {
    var chatMessage by rememberSaveable { mutableStateOf<String?>(null) }

    if (chatMessage == null) {
        HomeScreen(onSendMessage = { chatMessage = it })
    } else {
        ChatRoute(
            initialMessage = chatMessage.orEmpty(),
            onBackClick = { chatMessage = null },
        )
    }
}
