package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun JjikmukEmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    JjikmukAuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        textStyle = JjikmukTheme.typography.titleM.asEnglish(),
        placeholderStyle = JjikmukTheme.typography.bodyM.asEnglish(),
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier,
    )
}

@Composable
fun JjikmukPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    JjikmukAuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        textStyle = JjikmukTheme.typography.bodyM,
        placeholderStyle = JjikmukTheme.typography.bodyM,
        isError = false,
        visualTransformation = if (isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation(mask = '●')
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        trailingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isPasswordVisible = !isPasswordVisible },
            ) {
                Image(
                    painter = painterResource(
                        if (isPasswordVisible) {
                            R.drawable.ic_password_hidden
                        } else {
                            R.drawable.ic_password_visible
                        },
                    ),
                    contentDescription = if (isPasswordVisible) "비밀번호 가리기" else "비밀번호 보기",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun JjikmukAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    placeholderStyle: TextStyle,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = JjikmukTheme.colors
    val shape = RoundedCornerShape(8.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle.copy(color = colors.textPrimary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(colors.brand),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(colors.surfaceSecondary)
            .border(
                width = 1.dp,
                color = if (isError) colors.error else colors.borderSubtle,
                shape = shape,
            ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(
                            start = 18.dp,
                            end = if (trailingContent == null) 18.dp else 56.dp,
                        ),
                ) {
                    if (value.isEmpty()) {
                        androidx.compose.material3.Text(
                            text = placeholder,
                            color = colors.textSecondary,
                            style = placeholderStyle,
                        )
                    }
                    innerTextField()
                }
                if (trailingContent != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        trailingContent()
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun AuthTextFieldsPreview() {
    JjikmukTheme {
        JjikmukPasswordTextField(
            value = "password",
            onValueChange = {},
            placeholder = "비밀번호 입력",
        )
    }
}
