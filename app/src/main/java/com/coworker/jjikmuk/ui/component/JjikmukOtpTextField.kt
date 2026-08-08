package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun JjikmukOtpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    digitCount: Int = OTP_DIGIT_COUNT,
) {
    val colors = JjikmukTheme.colors
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { focusRequester.requestFocus() },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(17.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            repeat(digitCount) { index ->
                val digit = value.getOrNull(index)
                val borderColor = when {
                    isError -> colors.error
                    digit != null -> colors.brandSubtle
                    else -> colors.borderSubtle
                }
                val borderWidth = if (isError || digit != null) 2.dp else 1.dp

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(
                            color = if (digit == null) colors.surfaceSecondary else colors.surface,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(8.dp),
                        ),
                ) {
                    if (digit != null) {
                        Text(
                            text = digit.toString(),
                            color = colors.textPrimary,
                            style = JjikmukTheme.typography.h1.asEnglish(),
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = { changedValue ->
                onValueChange(
                    changedValue
                        .filter(Char::isDigit)
                        .take(digitCount),
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            textStyle = JjikmukTheme.typography.h1.copy(
                color = androidx.compose.ui.graphics.Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .alpha(0.01f),
        )
    }
}

private const val OTP_DIGIT_COUNT = 4

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun OtpTextFieldPreview() {
    JjikmukTheme {
        JjikmukOtpTextField(
            value = "113",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun OtpTextFieldErrorPreview() {
    JjikmukTheme {
        JjikmukOtpTextField(
            value = "1133",
            onValueChange = {},
            isError = true,
        )
    }
}
