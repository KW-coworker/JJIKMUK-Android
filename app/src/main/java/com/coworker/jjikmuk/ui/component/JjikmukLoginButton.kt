package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = when {
        !enabled -> JjikmukTheme.colors.disabled
        isPressed -> JjikmukTheme.colors.brandPressed
        else -> JjikmukTheme.colors.brandStrong
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = JjikmukTheme.colors.surface,
            disabledContainerColor = JjikmukTheme.colors.disabled,
            disabledContentColor = JjikmukTheme.colors.textDisabled,
        ),
        interactionSource = interactionSource,
    ) {
        Text(
            text = "로그인",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukLoginButtonPreview() {
    JjikmukTheme {
        JjikmukLoginButton(onClick = {})
    }
}
