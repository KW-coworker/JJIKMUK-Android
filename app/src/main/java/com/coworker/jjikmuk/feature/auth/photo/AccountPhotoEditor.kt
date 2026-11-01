package com.coworker.jjikmuk.feature.auth.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import com.coworker.jjikmuk.feature.auth.domain.ProfilePhoto
import com.coworker.jjikmuk.ui.component.JjikmukProfileImagePicker
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun rememberPrivatePhotoModel(photo: ProfilePhoto?): ImageRequest? {
    val context = LocalContext.current
    return remember(context, photo) {
        photo?.let { ImageRequest.Builder(context).data(it.bytes)
            .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPhotoEditor(viewModel: ProfilePhotoViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var options by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::selected)
    LaunchedEffect(state.generation) { options = false; confirmDelete = false; viewModel.load() }
    JjikmukProfileImagePicker(imageModel = rememberPrivatePhotoModel(state.photo),
        onClick = { if (state.canEdit) options = true }, containerSize = 108.dp, imageSize = 88.dp,
        imageBackgroundColor = JjikmukTheme.colors.textTertiary, imageBorderWidth = 3.dp,
        actionIconEndPadding = 3.dp, actionIconBottomPadding = 14.dp, showDefaultImage = true)
    if (state.busy) Text("사진 확인 중…", style = JjikmukTheme.typography.bodyS)
    state.message?.let { Text(it, color = JjikmukTheme.colors.textSecondary, style = JjikmukTheme.typography.bodyS) }
    if (state.mustReload || state.message != null) TextButton(onClick = viewModel::load, enabled = !state.busy) { Text("최신 사진 확인") }
    if (options) ModalBottomSheet(onDismissRequest = { options = false }) {
        Text("갤러리에서 선택", modifier = Modifier.fillMaxWidth().clickable {
            options = false
            if (viewModel.beginSelection()) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.padding(22.dp), style = JjikmukTheme.typography.bodyL)
        if (state.photo != null) Text("기본 사진으로 변경", modifier = Modifier.fillMaxWidth().clickable {
            options = false; confirmDelete = true
        }.padding(22.dp), style = JjikmukTheme.typography.bodyL)
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("프로필 사진 삭제") }, text = { Text("저장된 사진을 삭제하고 기본 사진으로 변경할까요?") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } })
}
