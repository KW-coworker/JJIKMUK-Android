package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukEmailTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun PasswordResetEmailRoute(
    viewModel: PasswordResetViewModel,
    onBackClick: () -> Unit,
    onCodeSent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PasswordResetEmailScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onBackClick = onBackClick,
        onSendCodeClick = {
            if (viewModel.sendResetCode()) onCodeSent()
        },
        modifier = modifier,
    )
}

@Composable
fun PasswordResetEmailScreen(
    uiState: PasswordResetUiState,
    onEmailChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSendCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val colors = JjikmukTheme.colors

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
                    text = stringResource(R.string.password_reset_email_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.password_reset_email_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 129.dp),
            ) {
                JjikmukEmailTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    isError = uiState.emailError != null,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (uiState.isEmailNotEmpty) onSendCodeClick()
                    },
                )
                if (uiState.emailError != null) {
                    Text(
                        text = uiState.emailError,
                        color = colors.error,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.password_reset_send_code),
                onClick = {
                    focusManager.clearFocus()
                    onSendCodeClick()
                },
                enabled = uiState.isEmailNotEmpty,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 569.dp),
            )
        }
    }
}

private val AUTH_STATUS_BAR_COLOR = androidx.compose.ui.graphics.Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetEmailEmptyPreview() {
    JjikmukTheme {
        PasswordResetEmailScreen(
            uiState = PasswordResetUiState(),
            onEmailChange = {},
            onBackClick = {},
            onSendCodeClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetEmailFilledPreview() {
    JjikmukTheme {
        PasswordResetEmailScreen(
            uiState = PasswordResetUiState(email = "coworker@kw.ac.kr"),
            onEmailChange = {},
            onBackClick = {},
            onSendCodeClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetEmailErrorPreview() {
    JjikmukTheme {
        PasswordResetEmailScreen(
            uiState = PasswordResetUiState(
                email = "unknown@example.com",
                emailError = "이메일 주소를 다시 확인해 주세요",
            ),
            onEmailChange = {},
            onBackClick = {},
            onSendCodeClick = {},
        )
    }
}
