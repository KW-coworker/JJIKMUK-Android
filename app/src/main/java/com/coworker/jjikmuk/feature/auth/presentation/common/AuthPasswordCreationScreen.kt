package com.coworker.jjikmuk.feature.auth.presentation.common

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukPasswordTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun AuthPasswordCreationScreen(
    title: String,
    description: String,
    passwordPlaceholder: String,
    passwordConfirmPlaceholder: String,
    password: String,
    passwordConfirm: String,
    validationError: AuthPasswordValidationError?,
    isPasswordValid: Boolean,
    passwordTooShortMessage: String,
    passwordMismatchMessage: String,
    confirmButtonText: String,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val confirmFocusRequester = remember { FocusRequester() }
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
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 20.dp)
                    .offset(y = 10.dp),
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Spacer(modifier = Modifier.padding(top = 18.dp))
                Text(
                    text = description,
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
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = passwordPlaceholder,
                    isError = validationError == AuthPasswordValidationError.TooShort,
                    showVisibilityToggle = false,
                    imeAction = ImeAction.Next,
                    onImeAction = confirmFocusRequester::requestFocus,
                )
                Spacer(modifier = Modifier.padding(top = 15.dp))
                JjikmukPasswordTextField(
                    value = passwordConfirm,
                    onValueChange = onPasswordConfirmChange,
                    placeholder = passwordConfirmPlaceholder,
                    isError = validationError == AuthPasswordValidationError.Mismatch,
                    showVisibilityToggle = false,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (isPasswordValid) onConfirmClick()
                    },
                    modifier = Modifier.focusRequester(confirmFocusRequester),
                )
                if (validationError != null) {
                    Spacer(modifier = Modifier.padding(top = 11.dp))
                    Text(
                        text = when (validationError) {
                            AuthPasswordValidationError.TooShort -> passwordTooShortMessage
                            AuthPasswordValidationError.Mismatch -> passwordMismatchMessage
                        },
                        color = colors.error,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            JjikmukPrimaryButton(
                text = confirmButtonText,
                onClick = {
                    focusManager.clearFocus()
                    onConfirmClick()
                },
                enabled = isPasswordValid,
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
private fun AuthPasswordCreationScreenPreview() {
    JjikmukTheme {
        AuthPasswordCreationScreen(
            title = "비밀번호 생성",
            description = "비밀번호를 설정해 주세요\n비밀번호는 6자리 이상이어야 합니다",
            passwordPlaceholder = "비밀번호 설정",
            passwordConfirmPlaceholder = "비밀번호 확인",
            password = "",
            passwordConfirm = "",
            validationError = null,
            isPasswordValid = false,
            passwordTooShortMessage = "비밀번호는 6자리 이상이어야 합니다.",
            passwordMismatchMessage = "비밀번호가 일치하지 않습니다.",
            confirmButtonText = "확인",
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AuthPasswordTooShortPreview() {
    JjikmukTheme {
        AuthPasswordCreationScreen(
            title = "비밀번호 생성",
            description = "비밀번호를 설정해 주세요\n비밀번호는 6자리 이상이어야 합니다",
            passwordPlaceholder = "비밀번호 설정",
            passwordConfirmPlaceholder = "비밀번호 확인",
            password = "123",
            passwordConfirm = "123",
            validationError = AuthPasswordValidationError.TooShort,
            isPasswordValid = false,
            passwordTooShortMessage = "비밀번호는 6자리 이상이어야 합니다.",
            passwordMismatchMessage = "비밀번호가 일치하지 않습니다.",
            confirmButtonText = "확인",
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AuthPasswordMismatchPreview() {
    JjikmukTheme {
        AuthPasswordCreationScreen(
            title = "비밀번호 생성",
            description = "비밀번호를 설정해 주세요\n비밀번호는 6자리 이상이어야 합니다",
            passwordPlaceholder = "비밀번호 설정",
            passwordConfirmPlaceholder = "비밀번호 확인",
            password = "password123",
            passwordConfirm = "different123",
            validationError = AuthPasswordValidationError.Mismatch,
            isPasswordValid = false,
            passwordTooShortMessage = "비밀번호는 6자리 이상이어야 합니다.",
            passwordMismatchMessage = "비밀번호가 일치하지 않습니다.",
            confirmButtonText = "확인",
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}
