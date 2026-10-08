package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthCompleteScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun AccountWithdrawalCompleteScreen(
    onReturnToAuthChoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        JjikmukAuthCompleteScreen(
            title = stringResource(R.string.account_withdrawal_complete_title),
            description = stringResource(R.string.account_withdrawal_complete_description),
            buttonText = stringResource(R.string.account_withdrawal_return_to_start),
            onButtonClick = onReturnToAuthChoice,
            buttonBottomPadding = 40.dp,
        )
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(WithdrawalNavigationBarColor),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountWithdrawalCompleteScreenPreview() {
    JjikmukTheme {
        AccountWithdrawalCompleteScreen(onReturnToAuthChoice = {})
    }
}
