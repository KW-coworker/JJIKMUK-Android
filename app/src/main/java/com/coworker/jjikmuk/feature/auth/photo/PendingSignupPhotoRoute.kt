package com.coworker.jjikmuk.feature.auth.photo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.ui.component.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun PendingSignupPhotoRoute(viewModel: PendingSignupPhotoViewModel, onContinue: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.open() }
    LaunchedEffect(state.done) { if (state.done) onContinue() }
    BackHandler { viewModel.later() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("프로필 사진", style = JjikmukTheme.typography.h1)
        if (state.busy) Text("사진을 확인하고 있어요", style = JjikmukTheme.typography.bodyL)
        state.pending?.let {
            Text("가입할 때 선택한 사진이에요. 이 계정의 프로필 사진으로 저장할까요?", style = JjikmukTheme.typography.bodyL)
            JjikmukProfileImagePicker(imageModel = rememberPrivatePhotoModel(it), onClick = {})
            if (state.server != null) {
                Text("현재 계정에 저장된 사진", style = JjikmukTheme.typography.bodyM)
                JjikmukProfileImagePicker(imageModel = rememberPrivatePhotoModel(state.server), onClick = {})
            }
            Text("저장하면 같은 계정의 다른 기기에서도 볼 수 있어요.", style = JjikmukTheme.typography.bodyM)
        }
        state.message?.let { Text(it, style = JjikmukTheme.typography.bodyM) }
        if (state.mustReload) JjikmukSecondaryButton("저장된 사진 다시 확인", viewModel::reload, enabled = !state.busy)
        if (state.pending != null) {
            JjikmukPrimaryButton(if (state.uploaded) "임시 사진 정리" else "선택한 사진 저장",
                if (state.uploaded) viewModel::discard else viewModel::upload,
                enabled = if (state.uploaded) !state.busy else state.canUpload)
            if (!state.uploaded) {
                JjikmukSecondaryButton("임시 사진 삭제하고 계속", viewModel::discard, enabled = !state.busy && state.loaded && !state.mustReload)
                Text("계정에 저장된 사진은 유지하고, 이 기기의 임시 사진만 삭제해요.", style = JjikmukTheme.typography.bodyS)
            }
        }
        JjikmukSecondaryButton("나중에 설정하기", viewModel::later, enabled = !state.busy)
    }
}
