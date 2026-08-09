package com.coworker.jjikmuk.feature.auth.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukOtpTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun AuthOtpScreen(
    otp: String,
    otpError: String?,
    remainingOtpSeconds: Int,
    isOtpComplete: Boolean,
    onOtpChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onResendClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = stringResource(R.string.password_reset_otp_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.password_reset_otp_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 130.dp),
            ) {
                JjikmukOtpTextField(
                    value = otp,
                    onValueChange = onOtpChange,
                    isError = otpError != null,
                )
                if (otpError != null) {
                    Text(
                        text = otpError,
                        color = colors.error,
                        style = JjikmukTheme.typography.bodyM,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 11.dp),
                    )
                }
                OtpTimer(
                    remainingSeconds = remainingOtpSeconds,
                    modifier = Modifier.padding(top = if (otpError == null) 24.dp else 16.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(23.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 528.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = stringResource(R.string.password_reset_otp_not_received),
                        color = colors.textSecondary,
                        style = JjikmukTheme.typography.bodyM,
                    )
                    Text(
                        text = stringResource(R.string.password_reset_otp_resend),
                        color = colors.textPrimary,
                        style = JjikmukTheme.typography.labelL.copy(
                            textDecoration = TextDecoration.Underline,
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onResendClick,
                        ),
                    )
                }
                JjikmukPrimaryButton(
                    text = stringResource(R.string.auth_confirm),
                    onClick = onConfirmClick,
                    enabled = isOtpComplete,
                )
            }
        }
    }
}

@Composable
private fun OtpTimer(
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(39.dp)
            .background(colors.surface, RoundedCornerShape(100.dp))
            .border(1.dp, colors.border, RoundedCornerShape(100.dp))
            .padding(horizontal = 17.dp, vertical = 9.dp),
    ) {
        Text(
            text = stringResource(R.string.password_reset_otp_remaining_time),
            color = colors.textSecondary,
            style = JjikmukTheme.typography.labelS,
        )
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            color = colors.brand,
            style = JjikmukTheme.typography.titleM.asEnglish(),
        )
    }
}

private val AUTH_STATUS_BAR_COLOR = androidx.compose.ui.graphics.Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AuthOtpEmptyPreview() {
    JjikmukTheme {
        AuthOtpScreen(
            otp = "",
            otpError = null,
            remainingOtpSeconds = 180,
            isOtpComplete = false,
            onOtpChange = {},
            onBackClick = {},
            onResendClick = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AuthOtpErrorPreview() {
    JjikmukTheme {
        AuthOtpScreen(
            otp = "0000",
            otpError = "인증번호를 다시 확인해 주세요",
            remainingOtpSeconds = 180,
            isOtpComplete = true,
            onOtpChange = {},
            onBackClick = {},
            onResendClick = {},
            onConfirmClick = {},
        )
    }
}
