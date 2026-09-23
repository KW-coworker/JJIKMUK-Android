package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun AccountSecurityScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            JjikmukTopAppBar(
                leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                showBottomDivider = true,
                centerContent = {
                    Text(
                        text = "계정 및 보안",
                        color = JjikmukTheme.colors.textPrimary,
                        style = JjikmukTheme.typography.labelL,
                    )
                },
            )
        },
        containerColor = JjikmukTheme.colors.surfaceSecondary,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JjikmukTheme.colors.surface)
                    .padding(horizontal = 26.dp)
                    .padding(top = 20.dp, bottom = 10.dp),
            ) {
                Text(
                    text = "앱 설정",
                    color = JjikmukTheme.colors.textTertiary,
                    style = JjikmukTheme.typography.titleM,
                )
                Spacer(modifier = Modifier.height(10.dp))
                AccountSecurityMenuItem(
                    title = "비밀번호 변경",
                    iconRes = R.drawable.ic_setting_key,
                    onClick = {},
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(JjikmukTheme.colors.surfaceSecondary),
            )
            AccountWithdrawalRow(onClick = {})
        }
    }
}

@Composable
private fun AccountSecurityMenuItem(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(57.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.bodyL,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AccountWithdrawalRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(JjikmukTheme.colors.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "회원 탈퇴",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyM,
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountSecurityScreenPreview() {
    JjikmukTheme {
        AccountSecurityScreen(onBackClick = {})
    }
}
