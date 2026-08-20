package com.coworker.jjikmuk.feature.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukEmailTextField
import com.coworker.jjikmuk.ui.component.JjikmukPasswordTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryIconButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onBackClick = onBackClick,
        onLoginClick = {
            if (viewModel.login()) onLoginSuccess()
        },
        onForgotPasswordClick = onForgotPasswordClick,
        onSignUpClick = onSignUpClick,
        onGoogleLoginClick = onGoogleLoginClick,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val colors = JjikmukTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                LoginTitle(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 20.dp)
                        .offset(y = 10.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp)
                        .offset(y = 120.dp),
                ) {
                    JjikmukEmailTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        placeholder = stringResource(R.string.auth_email_placeholder),
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    )
                    JjikmukPasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        placeholder = stringResource(R.string.auth_password_placeholder),
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focusManager.clearFocus()
                            onLoginClick()
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = 262.dp),
                ) {
                    if (uiState.loginError != null) {
                        Text(
                            text = uiState.loginError,
                            color = colors.error,
                            style = JjikmukTheme.typography.bodyM,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        color = colors.textSecondary,
                        style = JjikmukTheme.typography.titleM,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .authLink(onForgotPasswordClick),
                    )
                }

                JjikmukPrimaryButton(
                    text = stringResource(R.string.auth_login),
                    onClick = {
                        focusManager.clearFocus()
                        onLoginClick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp)
                        .offset(y = 359.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp)
                        .offset(y = 425.dp),
                ) {
                    LoginDivider()
                    JjikmukSecondaryIconButton(
                        icon = painterResource(R.drawable.ic_google_login),
                        contentDescription = stringResource(R.string.auth_google_login),
                        onClick = onGoogleLoginClick,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 17.dp),
        ) {
            Text(
                text = stringResource(R.string.auth_no_account),
                color = colors.textSecondary,
                style = JjikmukTheme.typography.titleM,
            )
            Text(
                text = stringResource(R.string.auth_sign_up_action),
                color = colors.brand,
                style = JjikmukTheme.typography.titleM,
                modifier = Modifier.authLink(onSignUpClick),
            )
        }
    }
}

@Composable
private fun LoginTitle(modifier: Modifier = Modifier) {
    val colors = JjikmukTheme.colors
    val text = buildAnnotatedString {
        append("다시 오신 걸 환영해요!\n어떤 걸 ")
        withStyle(SpanStyle(color = colors.brand)) { append("찍") }
        append("어 ")
        withStyle(SpanStyle(color = colors.brand)) { append("먹") }
        append("어볼까요?")
    }

    Text(
        text = text,
        color = colors.textPrimary,
        style = JjikmukTheme.typography.h1,
        modifier = modifier,
    )
}

@Composable
private fun LoginDivider(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(
            color = JjikmukTheme.colors.borderSubtle,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.auth_or_login_with),
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.titleM.asEnglish(),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(
            color = JjikmukTheme.colors.borderSubtle,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Modifier.authLink(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onClick,
)

private val AUTH_STATUS_BAR_COLOR = androidx.compose.ui.graphics.Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LoginScreenPreview() {
    JjikmukTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onBackClick = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
            onGoogleLoginClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LoginErrorScreenPreview() {
    JjikmukTheme {
        LoginScreen(
            uiState = LoginUiState(loginError = "이메일 또는 비밀번호를 다시 확인해 주세요"),
            onEmailChange = {},
            onPasswordChange = {},
            onBackClick = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
            onGoogleLoginClick = {},
        )
    }
}
