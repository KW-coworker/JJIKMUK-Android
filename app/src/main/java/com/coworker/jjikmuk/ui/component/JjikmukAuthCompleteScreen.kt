package com.coworker.jjikmuk.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukAuthCompleteScreen(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors

    BackHandler(enabled = true) {}

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 210.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_password_reset_success),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.size(35.dp))
                Text(
                    text = title,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    style = JjikmukTheme.typography.h1,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = description,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            JjikmukPrimaryButton(
                text = buttonText,
                onClick = onButtonClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .offset(y = 635.dp),
            )
        }
    }
}

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)
