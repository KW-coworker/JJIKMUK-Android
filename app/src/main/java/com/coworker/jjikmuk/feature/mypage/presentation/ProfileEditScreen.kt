package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.component.JjikmukProfileImagePicker
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun ProfileEditRoute(onBackClick: () -> Unit, viewModel: ProfileEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.open()
        onDispose { viewModel.close() }
    }
    ProfileEditScreen(onBackClick = onBackClick, state = state,
        onNicknameChange = viewModel::updateNickname, onSave = viewModel::save, onReload = viewModel::reload,
        photoEditor = { com.coworker.jjikmuk.feature.auth.photo.AccountPhotoEditor() })
}

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ProfileEditState = ProfileEditState(),
    onNicknameChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onReload: () -> Unit = {},
    photoEditor: (@Composable () -> Unit)? = null,
) {

    Scaffold(
        topBar = {
            JjikmukTopAppBar(
                leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                showBottomDivider = true,
                centerContent = {
                    Text(
                        text = "프로필 수정",
                        color = JjikmukTheme.colors.textPrimary,
                        style = JjikmukTheme.typography.labelL,
                    )
                },
            )
        },
        containerColor = JjikmukTheme.colors.surface,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            if (photoEditor != null) photoEditor() else JjikmukProfileImagePicker(
                imageModel = null,
                onClick = {},
                containerSize = 108.dp,
                imageSize = 88.dp,
                imageBackgroundColor = JjikmukTheme.colors.textTertiary,
                imageBorderWidth = 3.dp,
                actionIconEndPadding = 3.dp,
                actionIconBottomPadding = 14.dp,
                showDefaultImage = false,
            )
            Spacer(modifier = Modifier.height(62.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProfileEditFieldGroup(
                    label = "이름(닉네임)",
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    enabled = state.loaded && !state.busy,
                    textStyle = JjikmukTheme.typography.labelM,
                    textColor = JjikmukTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(20.dp))
                ProfileEditFieldGroup(
                    label = "이메일 계정",
                    value = state.email,
                    onValueChange = {},
                    textStyle = JjikmukTheme.typography.labelM.asEnglish(),
                    textColor = JjikmukTheme.colors.textSecondary,
                    enabled = false,
                    helperText = "이메일 주소는 변경할 수 없습니다.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                state.message?.let {
                    Text(it, color = JjikmukTheme.colors.textSecondary, style = JjikmukTheme.typography.bodyM)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                JjikmukPrimaryButton(
                    text = when {
                        state.busy -> "확인 중…"
                        state.mustReload || !state.loaded -> "최신 정보 확인"
                        else -> "저장"
                    },
                    onClick = if (state.mustReload || !state.loaded) onReload else onSave,
                    enabled = !state.busy,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileEditFieldGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helperText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.labelM,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ProfileEditTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = textStyle.copy(color = textColor),
        )
        helperText?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyM,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ProfileEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(JjikmukTheme.colors.brand),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions.Default,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = shape,
            )
            .padding(horizontal = 18.dp),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxSize(),
            ) {
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ProfileEditScreenPreview() {
    JjikmukTheme {
        ProfileEditScreen(onBackClick = {}, state = ProfileEditState(nickname = "코워커", email = "coworker@example.com", loaded = true))
    }
}
