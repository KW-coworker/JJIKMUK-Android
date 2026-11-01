package com.coworker.jjikmuk.feature.auth.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.feature.auth.presentation.signup.*
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun ProfileCompletionRoute(
    viewModel: ProfileCompletionViewModel, onFinished: () -> Unit,
    onSessionLost: () -> Unit, onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    var confirmLogout by remember(session.generation) { mutableStateOf(false) }
    LaunchedEffect(session.generation) {
        if (session.credentials == null) { viewModel.clearDraft(); onSessionLost() }
        else viewModel.open()
    }
    LaunchedEffect(session.canEnterHome, session.profile) {
        if (session.canEnterHome && session.profile?.profileCompleted == true) {
            viewModel.clearDraft()
            onFinished()
        }
    }
    val back = {
        if (!state.busy) {
            if (state.page == ProfilePage.Nickname || state.needsReadback) confirmLogout = true
            else viewModel.back()
        }
    }
    BackHandler(onBack = back)
    if (!state.loaded) {
        Surface(Modifier.fillMaxSize(), color = JjikmukTheme.colors.surface) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("프로필 추가 입력", style = JjikmukTheme.typography.h1)
                Text(state.loadError ?: "프로필을 확인하고 있어요", style = JjikmukTheme.typography.bodyL)
                if (state.loading) CircularProgressIndicator()
                else JjikmukPrimaryButton("다시 확인", viewModel::reload)
                JjikmukSecondaryButton("로그아웃", { confirmLogout = true }, enabled = !state.busy)
            }
        }
    } else when (state.page) {
        ProfilePage.Nickname -> SignUpNicknameScreen(state.form, viewModel::nickname, back, viewModel::nextNickname)
        ProfilePage.Conditions -> SignUpConditionsScreen(state.form, viewModel::condition, back, viewModel::nextConditions)
        ProfilePage.Allergies -> SignUpAllergiesScreen(state.form, viewModel::allergy, back, viewModel::nextAllergies)
        ProfilePage.Vegetarian -> SignUpVegetarianScreen(state.form, viewModel::vegetarian, back, viewModel::review)
        ProfilePage.Review -> SignUpProfileScreen(
            uiState = state.form.copy(isSubmitting = state.busy), onNicknameChange = viewModel::nickname,
            onPhotoClick = {}, onEditConditionsClick = viewModel::editConditions,
            onBackClick = back, onNextClick = viewModel::submit,
            description = "닉네임과 선택한 조건을 확인해 주세요.\n조건을 선택하지 않아도 완료할 수 있어요.",
            photoContent = {
                Text("프로필 사진은 완료 후\n마이페이지에서 설정할 수 있어요.",
                    style = JjikmukTheme.typography.bodyM, color = JjikmukTheme.colors.textSecondary)
            },
            actionContent = {
                if (state.needsReadback) JjikmukPrimaryButton(
                    if (state.loading) "확인 중…" else "완료 상태 다시 확인", viewModel::reload, enabled = !state.busy)
                else JjikmukPrimaryButton(if (state.busy) "완료 처리 중…" else "프로필 완료", viewModel::submit,
                    enabled = state.editable && state.form.nickname.isNotBlank())
                JjikmukSecondaryButton("로그아웃", { confirmLogout = true }, enabled = !state.busy)
            },
        )
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("추가 입력을 나갈까요?") },
        text = { Text("로그아웃하면 저장하지 않은 입력은 사라져요. 다음 로그인 시 추가 입력을 다시 진행할 수 있어요.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; viewModel.clearDraft(); onLogout() }) { Text("로그아웃") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("계속 입력하기") } },
    )
}
