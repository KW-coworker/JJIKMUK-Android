package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.feature.auth.presentation.common.AuthOtpScreen
import kotlinx.coroutines.launch

@Composable
fun SignUpOtpRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onOtpVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    AuthOtpScreen(
        otp = uiState.otp,
        otpError = uiState.otpError,
        remainingOtpSeconds = uiState.remainingOtpSeconds,
        isOtpComplete = uiState.isOtpComplete && uiState.remainingOtpSeconds > 0 && !uiState.isAuthRequestInFlight,
        resendEnabled = !uiState.isAuthRequestInFlight && uiState.resendSeconds == 0,
        resendSeconds = uiState.resendSeconds,
        onOtpChange = viewModel::updateOtp,
        onBackClick = onBackClick,
        onResendClick = { scope.launch { viewModel.resendOtp() } },
        onConfirmClick = { scope.launch { if (viewModel.verifyOtp()) onOtpVerified() } },
        modifier = modifier,
    )
}
