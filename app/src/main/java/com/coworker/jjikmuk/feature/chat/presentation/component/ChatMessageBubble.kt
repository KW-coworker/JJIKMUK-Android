package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.InterFontFamily
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.PretendardFontFamily

@Composable
fun ChatMessageBubble(
    text: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isMine) JjikmukTheme.colors.brandStrong else JjikmukTheme.colors.disabled
    val textColor = if (isMine) JjikmukTheme.colors.surface else JjikmukTheme.colors.textPrimary
    val shape = if (isMine) {
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 0.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 0.dp,
            bottomEnd = 24.dp,
        )
    }

    if (isMine) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ChatBubbleContent(
                text = text,
                textColor = textColor,
                bubbleColor = bubbleColor,
                shape = shape,
                maxWidth = 292.dp,
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChatBotProfile()
            Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))
            ChatBubbleContent(
                text = text,
                textColor = textColor,
                bubbleColor = bubbleColor,
                shape = shape,
                maxWidth = 252.dp,
            )
        }
    }
}

@Composable
private fun ChatBotProfile(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .background(
                color = JjikmukTheme.colors.textDisabled,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun ChatBubbleContent(
    text: String,
    textColor: Color,
    bubbleColor: Color,
    shape: RoundedCornerShape,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .background(
                color = bubbleColor,
                shape = shape,
            )
            .padding(24.dp),
    ) {
        Text(
            text = text.toMixedLanguageTextStyle(
                koreanFontFamily = PretendardFontFamily,
                englishFontFamily = InterFontFamily,
            ),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun String.toMixedLanguageTextStyle(
    koreanFontFamily: FontFamily,
    englishFontFamily: FontFamily,
) = buildAnnotatedString {
    append(this@toMixedLanguageTextStyle)

    this@toMixedLanguageTextStyle.forEachIndexed { index, character ->
        addStyle(
            style = SpanStyle(
                fontFamily = if (character.isKorean()) {
                    koreanFontFamily
                } else {
                    englishFontFamily
                },
            ),
            start = index,
            end = index + 1,
        )
    }
}

private fun Char.isKorean(): Boolean {
    return this in '\uAC00'..'\uD7A3' ||
        this in '\u1100'..'\u11FF' ||
        this in '\u3130'..'\u318F'
}

@Preview(showBackground = true)
@Composable
private fun ChatMessageBubblePreview() {
    JjikmukTheme {
        ChatMessageBubble(
            text = "포키 블루베리 맛은 어때?",
            isMine = true,
        )
    }
}
