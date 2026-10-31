package com.coworker.jjikmuk.feature.auth.google

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.ui.component.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GoogleLoginRoute(
    viewModel: GoogleLoginViewModel, onSignedIn: () -> Unit, onBack: () -> Unit,
    onPasswordReset: () -> Unit, source: GoogleCredentialSource = remember { AndroidGoogleCredentialSource() },
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    fun choose() {
        val ticket = viewModel.beginPicking() ?: return
        scope.launch {
            try { viewModel.receiveCredential(ticket, source.get(context)) }
            catch (cancelled: CancellationException) { viewModel.pickerCancelled(ticket); throw cancelled }
        }
    }
    LaunchedEffect(Unit) { if (!viewModel.state.value.started) choose() }
    LaunchedEffect(state.phase) {
        when {
            state.phase == GooglePhase.Finished && !state.linked -> onSignedIn()
            state.phase == GooglePhase.Exit -> {
                state.message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                if (state.exit == GoogleExit.PasswordReset) onPasswordReset() else onBack()
            }
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == GooglePhase.LinkRequired) while (true) { viewModel.tick(); delay(1_000) }
    }
    val back = { if (state.phase == GooglePhase.Finished) onSignedIn() else viewModel.exit() }
    BackHandler(onBack = back)
    Surface(Modifier.fillMaxSize(), color = JjikmukTheme.colors.surface) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            JjikmukAuthTopBar(onBackClick = back)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (state.phase in setOf(GooglePhase.LinkRequired, GooglePhase.Linking)) "기존 계정 연결" else "Google 로그인",
                    style = JjikmukTheme.typography.h1)
                when (state.phase) {
                    GooglePhase.LinkRequired, GooglePhase.Linking -> {
                        Text("선택한 Google 계정과 같은 이메일로 가입한 계정이 있어요. 기존 비밀번호를 확인하면 두 로그인 방식을 하나의 계정에 연결합니다.",
                            style = JjikmukTheme.typography.bodyL)
                        Text("연결 가능 시간 ${state.remainingSeconds / 60}:${(state.remainingSeconds % 60).toString().padStart(2, '0')}",
                            style = JjikmukTheme.typography.bodyM)
                        state.remainingAttempts?.let { Text("남은 확인 횟수: ${it}회", style = JjikmukTheme.typography.bodyM) }
                        JjikmukPasswordTextField(state.password, viewModel::password, "기존 계정 비밀번호",
                            isError = state.message != null, onImeAction = viewModel::link)
                        state.message?.let { Text(it, color = JjikmukTheme.colors.error) }
                        JjikmukPrimaryButton(if (state.busy) "연결 중…" else "기존 계정에 연결하고 로그인", viewModel::link,
                            enabled = !state.busy && state.password.isNotEmpty())
                        JjikmukSecondaryButton("비밀번호 찾기", { viewModel.exit(GoogleExit.PasswordReset) }, enabled = !state.busy)
                        JjikmukSecondaryButton("연결하지 않고 돌아가기", { viewModel.exit() }, enabled = !state.busy)
                    }
                    GooglePhase.Finished -> {
                        Text("Google 계정이 연결되었어요. 이제 기존 로그인과 Google 로그인을 모두 사용할 수 있어요.",
                            style = JjikmukTheme.typography.bodyL)
                        JjikmukPrimaryButton("계속하기", onSignedIn)
                    }
                    GooglePhase.Idle -> {
                        state.message?.let { Text(it, style = JjikmukTheme.typography.bodyL) }
                        JjikmukPrimaryButton("Google 로그인 다시 시도", ::choose)
                        JjikmukSecondaryButton("이메일 로그인으로 돌아가기", { viewModel.exit() })
                    }
                    else -> {
                        CircularProgressIndicator()
                        Text(when (state.phase) {
                            GooglePhase.Choosing -> "Google 계정을 선택해 주세요"
                            GooglePhase.Cancelling -> "연결을 중단하고 있어요"
                            else -> "로그인 정보를 확인하고 있어요"
                        }, style = JjikmukTheme.typography.bodyL)
                    }
                }
            }
        }
    }
}
