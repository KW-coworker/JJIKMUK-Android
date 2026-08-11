package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthCompleteScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SignUpCompleteScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JjikmukAuthCompleteScreen(
        title = stringResource(R.string.sign_up_complete_title),
        description = stringResource(R.string.sign_up_complete_description),
        buttonText = stringResource(R.string.password_reset_login_action),
        onButtonClick = onLoginClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpCompleteScreenPreview() {
    JjikmukTheme {
        SignUpCompleteScreen(onLoginClick = {})
    }
}
