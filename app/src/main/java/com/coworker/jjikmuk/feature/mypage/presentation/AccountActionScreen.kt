package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.auth.presentation.common.authPasswordValidationError
import com.coworker.jjikmuk.feature.auth.presentation.common.AuthPasswordValidationError
import com.coworker.jjikmuk.ui.component.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.launch

/** Lives outside Home so clearing the session cannot discard the confirmed completion screen. */
@Composable
fun AccountActionRoute(
    action: AccountAction, viewModel: AccountActionViewModel,
    onBack: () -> Unit, onAuthRequired: () -> Unit, onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(action) { viewModel.open(action) }
    LaunchedEffect(session.generation, state.busy, state.complete, state.ownerGeneration) {
        val latest = viewModel.state.value
        val ownerChanged = latest.ownerGeneration != null && latest.ownerGeneration != session.generation
        if ((!session.canEnterHome || ownerChanged) && !latest.busy && !latest.complete) {
            viewModel.leave()
            onAuthRequired()
        }
    }
    val back = { if (!state.busy) { viewModel.leave(); onBack() } }
    val recover = { scope.launch { viewModel.reauthenticate(); onAuthRequired() }; Unit }
    when {
        state.complete -> {
            BackHandler(onBack = onComplete)
            Column(Modifier.fillMaxSize().background(JjikmukTheme.colors.surface)) {
                if (state.storageWarning) Text("기기의 로그인 정보 정리에 실패했습니다. 앱 데이터 저장 상태를 확인해 주세요.",
                    color = JjikmukTheme.colors.error, modifier = Modifier.padding(20.dp))
                if (action == AccountAction.Withdrawal) AccountWithdrawalCompleteScreen(onComplete)
                else JjikmukAuthCompleteScreen(title = "비밀번호가 변경되었어요",
                    description = "모든 기기에서 다시 로그인해야 합니다.\n새 비밀번호로 로그인해 주세요.",
                    buttonText = "로그인하기", onButtonClick = onComplete)
            }
        }
        state.action != action -> Unit
        action == AccountAction.Withdrawal -> AccountWithdrawalScreen(
            hasConsented = state.consented, onConsentChange = viewModel::consent,
            onBackClick = back, onWithdrawClick = viewModel::submit,
            busy = state.busy, submissionEnabled = state.canSubmit, message = state.message,
            recoveryAction = if (state.unknown || state.requiresLogin) ({ JjikmukSecondaryButton("다시 로그인하기", recover) }) else null,
        )
        else -> AccountPasswordChangeScreen(state, viewModel::editCurrent, viewModel::editNew,
            viewModel::editConfirmation, viewModel::submit, back, recover)
    }
}

@Composable
internal fun AccountPasswordChangeScreen(
    state: AccountActionState, onCurrent: (String) -> Unit, onNew: (String) -> Unit,
    onConfirmation: (String) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit, onRecover: () -> Unit,
) {
    BackHandler { if (!state.busy) onBack() }
    Column(Modifier.fillMaxSize().background(JjikmukTheme.colors.surface).safeDrawingPadding().imePadding()) {
        JjikmukAuthTopBar(onBackClick = { if (!state.busy) onBack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("비밀번호 변경", style = JjikmukTheme.typography.h1, color = JjikmukTheme.colors.textPrimary)
            if (!state.hasPassword) {
                Text("Google로 로그인한 계정입니다. 비밀번호 변경을 사용할 수 없습니다.",
                    style = JjikmukTheme.typography.bodyL, color = JjikmukTheme.colors.textSecondary)
            } else {
                Text(stringResource(R.string.auth_password_requirements), style = JjikmukTheme.typography.bodyM,
                    color = JjikmukTheme.colors.textSecondary)
                Text("변경하면 모든 기기에서 다시 로그인해야 합니다.", style = JjikmukTheme.typography.bodyM)
                JjikmukPasswordTextField(state.currentPassword, onCurrent, "현재 비밀번호",
                    isError = state.errorField == "currentPassword")
                JjikmukPasswordTextField(state.newPassword, onNew, "새 비밀번호",
                    isError = state.errorField in setOf("password", "newPassword"))
                JjikmukPasswordTextField(state.confirmation, onConfirmation, "새 비밀번호 확인",
                    onImeAction = { if (state.canSubmit) onSubmit() })
                val validation = authPasswordValidationError(state.newPassword, state.confirmation)
                if (validation != null) Text(
                    if (validation == AuthPasswordValidationError.Mismatch) "비밀번호가 일치하지 않습니다."
                    else stringResource(R.string.auth_password_requirements),
                    color = JjikmukTheme.colors.error, style = JjikmukTheme.typography.bodyM)
            }
            state.message?.let { Text(it, color = JjikmukTheme.colors.error, style = JjikmukTheme.typography.bodyM) }
            if (state.unknown || state.requiresLogin) JjikmukSecondaryButton("다시 로그인하기", onRecover)
            if (state.hasPassword) JjikmukPrimaryButton(if (state.busy) "변경 처리 중…" else "비밀번호 변경하기",
                onSubmit, enabled = state.canSubmit)
        }
    }
}
