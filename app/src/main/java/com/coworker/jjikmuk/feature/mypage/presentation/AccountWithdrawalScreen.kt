package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun AccountWithdrawalRoute(
    onBackClick: () -> Unit,
    onReturnToAuthChoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasConsented by rememberSaveable { mutableStateOf(false) }
    var isComplete by rememberSaveable { mutableStateOf(false) }

    if (isComplete) {
        AccountWithdrawalCompleteScreen(
            onReturnToAuthChoice = onReturnToAuthChoice,
            modifier = modifier,
        )
    } else {
        AccountWithdrawalScreen(
            hasConsented = hasConsented,
            onConsentChange = { hasConsented = it },
            onBackClick = onBackClick,
            onWithdrawClick = {
                // UI preview flow only. Replace with confirmed API success when integrated.
                if (hasConsented) isComplete = true
            },
            modifier = modifier,
        )
    }
}

@Composable
fun AccountWithdrawalScreen(
    hasConsented: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surface),
    ) {
        JjikmukTopAppBar(
            leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
            showBottomDivider = true,
            modifier = Modifier.background(WithdrawalStatusBarColor),
            centerContent = {
                Text(
                    text = stringResource(R.string.account_withdrawal_title),
                    color = JjikmukTheme.colors.textPrimary,
                    style = JjikmukTheme.typography.labelL,
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 30.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.account_withdrawal_heading),
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.h1.withFullLineHeight(),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.account_withdrawal_description),
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyM.withFullLineHeight(),
            )
            Text(
                text = stringResource(R.string.account_withdrawal_warning),
                color = WithdrawalWarningColor,
                style = JjikmukTheme.typography.bodyM.withFullLineHeight(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            DeletedInformationCard()
            Spacer(modifier = Modifier.height(20.dp))
            WithdrawalConsentRow(
                checked = hasConsented,
                onCheckedChange = onConsentChange,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            JjikmukPrimaryButton(
                text = stringResource(R.string.account_withdrawal_action),
                onClick = onWithdrawClick,
                enabled = hasConsented,
            )
            JjikmukSecondaryButton(
                text = stringResource(R.string.account_withdrawal_cancel),
                onClick = onBackClick,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(WithdrawalNavigationBarColor),
        )
    }
}

@Composable
private fun DeletedInformationCard() {
    Surface(
        color = WithdrawalWarningBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WithdrawalWarningBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(19.dp)) {
            Text(
                text = stringResource(R.string.account_withdrawal_deleted_information),
                color = WithdrawalWarningColor,
                style = JjikmukTheme.typography.titleM.withFullLineHeight(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DeletedInformationRow("❤️", stringResource(R.string.account_withdrawal_products))
                DeletedInformationRow("🥗", stringResource(R.string.account_withdrawal_diet))
                DeletedInformationRow("👨‍👩‍👧", stringResource(R.string.account_withdrawal_family))
                DeletedInformationRow("📷", stringResource(R.string.account_withdrawal_history))
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun DeletedInformationRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = emoji,
            style = JjikmukTheme.typography.bodyM.withFullLineHeight()
                .copy(fontSize = 17.sp, lineHeight = 25.5.sp),
        )
        Text(
            text = text,
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyM.withFullLineHeight(),
        )
    }
}

@Composable
private fun WithdrawalConsentRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val shape = if (checked) CircleShape else RoundedCornerShape(7.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (checked) 26.dp else 22.dp)
                .shadow(if (checked) 2.dp else 0.dp, shape)
                .background(if (checked) WithdrawalCheckboxSelected else JjikmukTheme.colors.surface, shape)
                .border(
                    if (checked) 2.dp else 1.dp,
                    if (checked) JjikmukTheme.colors.surface else WithdrawalCheckboxBorder,
                    shape,
                ),
        ) {
            if (checked) {
                Image(
                    painter = painterResource(R.drawable.ic_withdrawal_check),
                    contentDescription = null,
                    modifier = Modifier.size(width = 10.5.dp, height = 7.58333.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.account_withdrawal_consent),
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.titleM.withFullLineHeight(),
            modifier = Modifier.weight(1f),
        )
    }
}

// Preserve Figma's line boxes instead of trimming the first/last line's leading.
private fun TextStyle.withFullLineHeight() = copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

private val WithdrawalWarningColor = Color(0xFFE7000B)
private val WithdrawalWarningBackground = Color(0xFFFFF5F5)
private val WithdrawalWarningBorder = Color(0xFFFFE2E2)
private val WithdrawalCheckboxBorder = Color(0xFFCBD5E1)
private val WithdrawalCheckboxSelected = Color(0xFF2B7FFF)
private val WithdrawalStatusBarColor = Color(0xFFFCFCFF)
internal val WithdrawalNavigationBarColor = Color(0xFFF3F4F9)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountWithdrawalScreenPreview() {
    JjikmukTheme {
        AccountWithdrawalScreen(false, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountWithdrawalConsentedScreenPreview() {
    JjikmukTheme {
        AccountWithdrawalScreen(true, {}, {}, {})
    }
}
