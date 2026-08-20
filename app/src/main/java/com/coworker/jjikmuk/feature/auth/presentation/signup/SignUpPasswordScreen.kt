package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.auth.presentation.common.AuthPasswordCreationScreen

@Composable
fun SignUpPasswordRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onPasswordCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthPasswordCreationScreen(
        title = stringResource(R.string.sign_up_password_title),
        description = stringResource(R.string.sign_up_password_description),
        passwordPlaceholder = stringResource(R.string.sign_up_password_placeholder),
        passwordConfirmPlaceholder = stringResource(R.string.password_reset_password_confirm_placeholder),
        password = uiState.password,
        passwordConfirm = uiState.passwordConfirm,
        validationError = uiState.passwordValidationError,
        isPasswordValid = uiState.isPasswordValid,
        passwordTooShortMessage = stringResource(R.string.password_reset_password_too_short),
        passwordMismatchMessage = stringResource(R.string.password_reset_password_mismatch),
        confirmButtonText = stringResource(R.string.auth_confirm),
        onPasswordChange = viewModel::updatePassword,
        onPasswordConfirmChange = viewModel::updatePasswordConfirm,
        onBackClick = onBackClick,
        onConfirmClick = onPasswordCreated,
        modifier = modifier,
    )
}
