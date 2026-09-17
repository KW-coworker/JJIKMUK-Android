package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    height: Dp = 43.dp,
    radius: Dp = 12.dp,
    textStyle: TextStyle = JjikmukTheme.typography.bodyS,
    placeholderStyle: TextStyle = JjikmukTheme.typography.bodyS,
    leadingIcon: Boolean = false,
    showSearchIcon: Boolean = true,
    showClearButton: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
) {
    val colors = JjikmukTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    val isActive = isFocused
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
    val backgroundColor = if (isActive) colors.surface else colors.surfaceSecondary
    val borderColor = if (isActive) colors.brandSubtle else colors.borderSubtle

    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(
                start = if (leadingIcon) 16.dp else 17.dp,
                end = 15.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon) {
            SearchIcon(
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(9.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !readOnly,
            readOnly = readOnly,
            singleLine = true,
            textStyle = textStyle.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchClick?.invoke() },
            ),
            cursorBrush = SolidColor(colors.brand),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = colors.textSecondary,
                            style = placeholderStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (value.isBlank()) {
            if (showSearchIcon) {
                SearchIcon(
                    contentDescription = "검색",
                    modifier = Modifier
                        .size(20.dp)
                        .then(
                            if (onSearchClick != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onSearchClick,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        } else if (showClearButton) {
            SearchClearButton(
                onClick = onClearClick ?: { onValueChange("") },
            )
        }
    }
}

@Composable
private fun SearchIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_product_search),
        contentDescription = contentDescription,
        tint = JjikmukTheme.colors.textSecondary,
        modifier = modifier,
    )
}

@Composable
private fun SearchClearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = JjikmukTheme.colors.textTertiary,
        shape = androidx.compose.foundation.shape.CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "×",
                color = JjikmukTheme.colors.surface,
                style = JjikmukTheme.typography.labelS,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun JjikmukSearchFieldPreview() {
    JjikmukTheme {
        JjikmukSearchField(
            value = "",
            onValueChange = {},
            placeholder = "어떤 안심 상품을 찾으시나요?",
            leadingIcon = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )
    }
}
