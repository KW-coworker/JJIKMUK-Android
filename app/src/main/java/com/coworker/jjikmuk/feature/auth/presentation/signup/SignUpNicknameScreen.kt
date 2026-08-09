package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukNicknameTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SignUpNicknameRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onNicknameCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpNicknameScreen(
        uiState = uiState,
        onNicknameChange = viewModel::updateNickname,
        onBackClick = onBackClick,
        onNextClick = {
            if (viewModel.validateNickname()) onNicknameCreated()
        },
        modifier = modifier,
    )
}

@Composable
fun SignUpNicknameScreen(
    uiState: SignUpUiState,
    onNicknameChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val colors = JjikmukTheme.colors

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(AUTH_STATUS_BAR_COLOR),
        )
        JjikmukAuthTopBar(onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 20.dp)
                    .offset(y = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.sign_up_nickname_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.sign_up_nickname_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 105.dp),
            ) {
                JjikmukNicknameTextField(
                    value = uiState.nickname,
                    onValueChange = onNicknameChange,
                    placeholder = stringResource(R.string.sign_up_nickname_placeholder),
                    isError = uiState.nicknameError != null,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (uiState.isNicknameNotBlank) onNextClick()
                    },
                )
                if (uiState.nicknameError != null) {
                    Text(
                        text = uiState.nicknameError,
                        color = colors.error,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.sign_up_email_next),
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
                enabled = uiState.isNicknameNotBlank,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 569.dp),
            )
        }
    }
}

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpNicknameEmptyPreview() {
    JjikmukTheme {
        SignUpNicknameScreen(
            uiState = SignUpUiState(),
            onNicknameChange = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpNicknameFilledPreview() {
    JjikmukTheme {
        SignUpNicknameScreen(
            uiState = SignUpUiState(nickname = "코워커"),
            onNicknameChange = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpNicknameErrorPreview() {
    JjikmukTheme {
        SignUpNicknameScreen(
            uiState = SignUpUiState(
                nickname = "타로",
                nicknameError = "이미 사용중인 닉네임입니다",
            ),
            onNicknameChange = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}
