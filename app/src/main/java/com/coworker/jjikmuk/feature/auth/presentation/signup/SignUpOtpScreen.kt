package com.coworker.jjikmuk.feature.auth.presentation.signup

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.auth.presentation.common.AuthOtpScreen

@Composable
fun SignUpOtpRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onOtpVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resendMessage = stringResource(R.string.password_reset_otp_resent_message)
    val expiredMessage = stringResource(R.string.password_reset_otp_expired_message)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SignUpEvent.OtpResent -> resendMessage
                SignUpEvent.OtpExpired -> expiredMessage
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    AuthOtpScreen(
        otp = uiState.otp,
        otpError = uiState.otpError,
        remainingOtpSeconds = uiState.remainingOtpSeconds,
        isOtpComplete = uiState.isOtpComplete,
        onOtpChange = viewModel::updateOtp,
        onBackClick = onBackClick,
        onResendClick = viewModel::resendOtp,
        onConfirmClick = {
            if (viewModel.verifyOtp()) onOtpVerified()
        },
        modifier = modifier,
    )
}
