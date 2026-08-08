package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukPasswordTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun PasswordResetNewPasswordRoute(
    viewModel: PasswordResetViewModel,
    onBackClick: () -> Unit,
    onPasswordChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PasswordResetNewPasswordScreen(
        uiState = uiState,
        onNewPasswordChange = viewModel::updateNewPassword,
        onNewPasswordConfirmChange = viewModel::updateNewPasswordConfirm,
        onBackClick = onBackClick,
        onConfirmClick = onPasswordChanged,
        modifier = modifier,
    )
}

@Composable
fun PasswordResetNewPasswordScreen(
    uiState: PasswordResetUiState,
    onNewPasswordChange: (String) -> Unit,
    onNewPasswordConfirmChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val confirmFocusRequester = remember { FocusRequester() }
    val colors = JjikmukTheme.colors
    val validationError = uiState.newPasswordError

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
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 20.dp)
                    .offset(y = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.password_reset_new_password_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Spacer(modifier = Modifier.padding(top = 18.dp))
                Text(
                    text = stringResource(R.string.password_reset_new_password_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 129.dp),
            ) {
                JjikmukPasswordTextField(
                    value = uiState.newPassword,
                    onValueChange = onNewPasswordChange,
                    placeholder = stringResource(R.string.password_reset_new_password_placeholder),
                    isError = validationError == PasswordValidationError.TooShort,
                    showVisibilityToggle = false,
                    imeAction = ImeAction.Next,
                    onImeAction = confirmFocusRequester::requestFocus,
                )
                Spacer(modifier = Modifier.padding(top = 15.dp))
                JjikmukPasswordTextField(
                    value = uiState.newPasswordConfirm,
                    onValueChange = onNewPasswordConfirmChange,
                    placeholder = stringResource(R.string.password_reset_password_confirm_placeholder),
                    isError = validationError == PasswordValidationError.Mismatch,
                    showVisibilityToggle = false,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (uiState.isNewPasswordValid) onConfirmClick()
                    },
                    modifier = Modifier.focusRequester(confirmFocusRequester),
                )
                if (validationError != null) {
                    Spacer(modifier = Modifier.padding(top = 11.dp))
                    Text(
                        text = stringResource(
                            when (validationError) {
                                PasswordValidationError.TooShort -> R.string.password_reset_password_too_short
                                PasswordValidationError.Mismatch -> R.string.password_reset_password_mismatch
                            },
                        ),
                        color = colors.error,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.auth_confirm),
                onClick = {
                    focusManager.clearFocus()
                    onConfirmClick()
                },
                enabled = uiState.isNewPasswordValid,
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
private fun PasswordResetNewPasswordEmptyPreview() {
    JjikmukTheme {
        PasswordResetNewPasswordScreen(
            uiState = PasswordResetUiState(),
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetNewPasswordTooShortPreview() {
    JjikmukTheme {
        PasswordResetNewPasswordScreen(
            uiState = PasswordResetUiState(newPassword = "12345"),
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetNewPasswordMismatchPreview() {
    JjikmukTheme {
        PasswordResetNewPasswordScreen(
            uiState = PasswordResetUiState(
                newPassword = "1234567",
                newPasswordConfirm = "7654321",
            ),
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetNewPasswordCompletePreview() {
    JjikmukTheme {
        PasswordResetNewPasswordScreen(
            uiState = PasswordResetUiState(
                newPassword = "1234567",
                newPasswordConfirm = "1234567",
            ),
            onNewPasswordChange = {},
            onNewPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}
