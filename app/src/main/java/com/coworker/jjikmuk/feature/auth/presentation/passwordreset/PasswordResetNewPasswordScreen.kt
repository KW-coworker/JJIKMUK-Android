package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.auth.presentation.common.AuthPasswordCreationScreen

@Composable
fun PasswordResetNewPasswordRoute(
    viewModel: PasswordResetViewModel,
    onBackClick: () -> Unit,
    onPasswordChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthPasswordCreationScreen(
        title = stringResource(R.string.password_reset_new_password_title),
        description = stringResource(R.string.password_reset_new_password_description),
        passwordPlaceholder = stringResource(R.string.password_reset_new_password_placeholder),
        passwordConfirmPlaceholder = stringResource(R.string.password_reset_password_confirm_placeholder),
        password = uiState.newPassword,
        passwordConfirm = uiState.newPasswordConfirm,
        validationError = uiState.newPasswordError,
        isPasswordValid = uiState.isNewPasswordValid,
        passwordTooShortMessage = stringResource(R.string.password_reset_password_too_short),
        passwordMismatchMessage = stringResource(R.string.password_reset_password_mismatch),
        confirmButtonText = stringResource(R.string.auth_confirm),
        onPasswordChange = viewModel::updateNewPassword,
        onPasswordConfirmChange = viewModel::updateNewPasswordConfirm,
        onBackClick = onBackClick,
        onConfirmClick = onPasswordChanged,
        modifier = modifier,
    )
}
