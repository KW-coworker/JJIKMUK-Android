package com.coworker.jjikmuk.feature.auth.presentation.passwordreset

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthCompleteScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun PasswordResetCompleteScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JjikmukAuthCompleteScreen(
        title = stringResource(R.string.password_reset_complete_title),
        description = stringResource(R.string.password_reset_complete_description),
        buttonText = stringResource(R.string.password_reset_login_action),
        onButtonClick = onLoginClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PasswordResetCompleteScreenPreview() {
    JjikmukTheme {
        PasswordResetCompleteScreen(onLoginClick = {})
    }
}
