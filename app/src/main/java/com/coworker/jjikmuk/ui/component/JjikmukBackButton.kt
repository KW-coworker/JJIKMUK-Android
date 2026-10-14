package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.annotation.DrawableRes
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = R.drawable.ic_arrow_back,
    iconSize: Dp = 30.dp,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = "뒤로가기",
            tint = JjikmukTheme.colors.textSecondary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukBackButtonPreview() {
    JjikmukTheme {
        JjikmukBackButton(onClick = {})
    }
}
