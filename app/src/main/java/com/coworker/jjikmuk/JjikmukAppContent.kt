package com.coworker.jjikmuk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import android.widget.Toast
import com.coworker.jjikmuk.feature.chat.presentation.ChatHistoryScreen
import com.coworker.jjikmuk.feature.chat.presentation.ChatRoute
import com.coworker.jjikmuk.feature.home.presentation.HomeScreen
import com.coworker.jjikmuk.feature.mypage.presentation.AccountSecurityScreen
import com.coworker.jjikmuk.feature.mypage.presentation.DietConditionManagementScreen
import com.coworker.jjikmuk.feature.mypage.presentation.MyPageScreen
import com.coworker.jjikmuk.feature.mypage.presentation.ProfileEditScreen
import com.coworker.jjikmuk.feature.mypage.presentation.SettingsScreen
import com.coworker.jjikmuk.feature.product.presentation.ProductScreen
import com.coworker.jjikmuk.feature.scanner.navigation.ScannerNavHost
import com.coworker.jjikmuk.ui.component.MainTab

@Composable
fun JjikmukAppContent() {
    val context = LocalContext.current
    var chatMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var chatConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var showDietConditionManagement by rememberSaveable { mutableStateOf(false) }
    var showProfileEdit by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAccountSecurity by rememberSaveable { mutableStateOf(false) }

    fun handleMainTabClick(tab: MainTab) {
        if (tab == MainTab.Diet) {
            Toast.makeText(context, "추후 구현 예정입니다", Toast.LENGTH_SHORT).show()
            return
        }

        selectedTab = tab
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (chatMessage == null) {
            if (showAccountSecurity) {
                AccountSecurityScreen(
                    onBackClick = { showAccountSecurity = false },
                )
            } else if (showSettings) {
                SettingsScreen(
                    onBackClick = {
                        showSettings = false
                        showAccountSecurity = false
                    },
                    onAccountSecurityClick = {
                        showAccountSecurity = true
                    },
                )
            } else if (showProfileEdit) {
                ProfileEditScreen(
                    onBackClick = { showProfileEdit = false },
                )
            } else if (showDietConditionManagement) {
                DietConditionManagementScreen(
                    onBackClick = { showDietConditionManagement = false },
                )
            } else when (selectedTab) {
                MainTab.Product -> {
                    ProductScreen(
                        selectedTab = selectedTab,
                        onTabClick = ::handleMainTabClick,
                        onScannerClick = { showScanner = true },
                    )
                }

                MainTab.History -> {
                    ChatHistoryScreen(
                        selectedTab = selectedTab,
                        onTabClick = ::handleMainTabClick,
                        onBackClick = { selectedTab = MainTab.Home },
                        onChatClick = { history ->
                            chatConversationId = history.id
                            chatMessage = history.title
                        },
                        onNewChatClick = {
                            chatConversationId = null
                            chatMessage = ""
                        },
                        onScannerClick = { showScanner = true },
                    )
                }

                MainTab.My -> {
                    MyPageScreen(
                        selectedTab = selectedTab,
                        onTabClick = ::handleMainTabClick,
                        onBackClick = { selectedTab = MainTab.Home },
                        onFamilyDietSettingClick = {
                            showDietConditionManagement = true
                        },
                        onProfileEditClick = {
                            showProfileEdit = true
                        },
                        onSettingsClick = {
                            showSettings = true
                        },
                    )
                }

                else -> {
                    HomeScreen(
                        selectedTab = selectedTab,
                        onTabClick = ::handleMainTabClick,
                        onChatHistoryClick = { selectedTab = MainTab.History },
                        onSendMessage = { message ->
                            chatConversationId = null
                            chatMessage = message
                        },
                        onScannerClick = { showScanner = true },
                    )
                }
            }
        } else {
            ChatRoute(
                conversationId = chatConversationId,
                initialMessage = chatMessage.orEmpty(),
                onBackClick = {
                    chatConversationId = null
                    chatMessage = null
                },
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
