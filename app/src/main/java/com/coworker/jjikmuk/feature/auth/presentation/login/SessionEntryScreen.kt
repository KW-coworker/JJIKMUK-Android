package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/** Temporary status presentation using existing tokens. Final error/onboarding design is pending. */
@Composable
fun SessionEntryScreen(state: SessionEntryState, onRetry: () -> Unit, onLogout: () -> Unit) {
    BackHandler { /* Never expose the previous account/home behind startup verification. */ }
    Surface(color = JjikmukTheme.colors.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            when (state) {
                is SessionEntryState.Retry -> {
                    Text(state.failure.userMessage(), style = JjikmukTheme.typography.bodyL)
                    Spacer(Modifier.height(24.dp))
                    JjikmukPrimaryButton("다시 확인", onRetry)
                    Spacer(Modifier.height(12.dp))
                    JjikmukSecondaryButton("로그아웃", onLogout)
                }
                SessionEntryState.ProfileRequired -> {
                    Text("프로필 추가 입력이 필요해요", style = JjikmukTheme.typography.h1)
                    Spacer(Modifier.height(12.dp))
                    Text("추가 입력 기능을 준비 중이에요. 완료 전에는 홈을 이용할 수 없어요.",
                        style = JjikmukTheme.typography.bodyL)
                    Spacer(Modifier.height(24.dp))
                    JjikmukPrimaryButton("완료 상태 다시 확인", onRetry)
                    Spacer(Modifier.height(12.dp))
                    JjikmukSecondaryButton("로그아웃", onLogout)
                }
                else -> {
                    CircularProgressIndicator(color = JjikmukTheme.colors.brand)
                    Spacer(Modifier.height(24.dp))
                    Text("로그인 정보를 확인하고 있어요", style = JjikmukTheme.typography.bodyL)
                }
            }
        }
    }
}
