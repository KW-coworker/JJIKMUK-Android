package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.presentation.splash.SplashScreen
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/**
 * 로그인 확인 화면. 확인 중이거나 다음 화면으로 넘어가는 동안에는 별도 문구 없이 스플래시 화면을 그대로 보여 주고,
 * 확인에 실패했을 때만 재시도/로그아웃 UI를 띄운다.
 */
@Composable
fun SessionEntryScreen(
    state: SessionEntryState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    splashLogoModifier: Modifier = Modifier,
) {
    BackHandler { /* Never expose the previous account/home behind startup verification. */ }
    if (state !is SessionEntryState.Retry) {
        SplashScreen(logoModifier = splashLogoModifier)
        return
    }
    Surface(color = JjikmukTheme.colors.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(state.failure.userMessage(), style = JjikmukTheme.typography.bodyL)
            Spacer(Modifier.height(24.dp))
            JjikmukPrimaryButton("다시 확인", onRetry)
            Spacer(Modifier.height(12.dp))
            JjikmukSecondaryButton("로그아웃", onLogout)
        }
    }
}
