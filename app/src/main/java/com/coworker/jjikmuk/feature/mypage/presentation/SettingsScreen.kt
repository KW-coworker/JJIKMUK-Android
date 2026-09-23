package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
fun SettingsScreen(
    onBackClick: () -> Unit,
    onAccountSecurityClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            JjikmukTopAppBar(
                leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                showBottomDivider = true,
                centerContent = {
                    Text(
                        text = "설정",
                        color = JjikmukTheme.colors.textPrimary,
                        style = JjikmukTheme.typography.labelL,
                    )
                },
            )
        },
        containerColor = JjikmukTheme.colors.surface,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 26.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "앱 설정",
                color = JjikmukTheme.colors.textTertiary,
                style = JjikmukTheme.typography.titleM,
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsMenuItem(
                title = "알림 설정",
                iconRes = R.drawable.ic_setting_bell,
            )
            SettingsMenuItem(
                title = "자주 묻는 질문",
                iconRes = R.drawable.ic_setting_help,
            )
            SettingsMenuItem(
                title = "계정 및 보안",
                iconRes = R.drawable.ic_setting_lock,
                showDivider = false,
                onClick = onAccountSecurityClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            SettingsLogoutButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 67.dp),
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
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
                    modifier = Modifier.size(22.dp),
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
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JjikmukTheme.colors.borderSubtle),
            )
        }
    }
}

@Composable
private fun SettingsLogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = JjikmukTheme.colors.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
        border = BorderStroke(
            width = 1.dp,
            color = JjikmukTheme.colors.border,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "로그아웃",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.labelL,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SettingsScreenPreview() {
    JjikmukTheme {
        SettingsScreen(onBackClick = {})
    }
}
