package com.coworker.jjikmuk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coworker.jjikmuk.feature.chat.presentation.ChatRoute
import com.coworker.jjikmuk.feature.home.presentation.HomeScreen
import com.coworker.jjikmuk.feature.product.presentation.ProductScreen
import com.coworker.jjikmuk.feature.scanner.navigation.ScannerNavHost
import com.coworker.jjikmuk.ui.component.MainTab

@Composable
fun JjikmukAppContent() {
    var chatMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showScanner by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (chatMessage == null) {
            when (selectedTab) {
                MainTab.Product -> {
                    ProductScreen(
                        selectedTab = selectedTab,
                        onTabClick = { tab -> selectedTab = tab },
                        onScannerClick = { showScanner = true },
                    )
                }

                else -> {
                    HomeScreen(
                        selectedTab = selectedTab,
                        onTabClick = { tab -> selectedTab = tab },
                        onSendMessage = { message -> chatMessage = message },
                        onScannerClick = { showScanner = true },
                    )
                }
            }
        } else {
            ChatRoute(
                initialMessage = chatMessage.orEmpty(),
                onBackClick = { chatMessage = null },
            )
        }

        if (showScanner) {
            ScannerNavHost(
                onExitScanner = { showScanner = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
