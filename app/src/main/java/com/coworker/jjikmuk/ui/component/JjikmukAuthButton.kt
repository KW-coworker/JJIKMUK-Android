package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    JjikmukAuthButton(
        text = text,
        onClick = onClick,
        style = AuthButtonStyle.Primary,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
fun JjikmukSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    JjikmukAuthButton(
        text = text,
        onClick = onClick,
        style = AuthButtonStyle.Secondary,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun JjikmukAuthButton(
    text: String,
    onClick: () -> Unit,
    style: AuthButtonStyle,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val colors = JjikmukTheme.colors

    val containerColor = when {
        !enabled -> colors.disabled
        style == AuthButtonStyle.Primary && isPressed -> colors.brandPressed
        style == AuthButtonStyle.Primary -> colors.brandStrong
        isPressed -> colors.disabled
        else -> colors.surface
    }
    val contentColor = when {
        !enabled -> colors.textTertiary
        style == AuthButtonStyle.Primary -> colors.surface
        else -> colors.textPrimary
    }
    val borderColor = when {
        style == AuthButtonStyle.Primary && enabled -> Color.Transparent
        else -> colors.border
    }
    val shape = RoundedCornerShape(AUTH_BUTTON_CORNER_RADIUS)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(AUTH_BUTTON_HEIGHT)
            .clip(shape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Text(
            text = text,
            color = contentColor,
            style = JjikmukTheme.typography.labelL,
        )
    }
}

private enum class AuthButtonStyle {
    Primary,
    Secondary,
}

private val AUTH_BUTTON_HEIGHT = 56.dp
private val AUTH_BUTTON_CORNER_RADIUS = 17.dp

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun JjikmukAuthButtonPreview() {
    JjikmukTheme {
        JjikmukPrimaryButton(
            text = "로그인",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun JjikmukDisabledAuthButtonPreview() {
    JjikmukTheme {
        JjikmukSecondaryButton(
            text = "회원가입",
            onClick = {},
            enabled = false,
        )
    }
}
