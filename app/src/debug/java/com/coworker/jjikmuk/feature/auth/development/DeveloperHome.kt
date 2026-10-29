package com.coworker.jjikmuk.feature.auth.development

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.coworker.jjikmuk.JjikmukAppContent

// UI access only. Never create credentials or persist a fake authenticated session.
private const val DeveloperHomeRoute = "development/home"

@Composable
fun DeveloperHomeEntry(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("개발용 홈·스캐너 열기 (서버 로그인 없음)")
    }
}

fun NavHostController.openDeveloperHome() {
    navigate(DeveloperHomeRoute) { launchSingleTop = true }
}

fun NavGraphBuilder.developerHomeGraph(navController: NavHostController) {
    composable(DeveloperHomeRoute) {
        val exit: () -> Unit = { navController.popBackStack() }
        BackHandler(onBack = exit)
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = exit, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Text("개발용 UI · 서버 로그인 없음 · 나가기")
            }
            Box(Modifier.weight(1f)) {
                JjikmukAppContent(onReturnToAuthChoice = exit)
            }
        }
    }
}
