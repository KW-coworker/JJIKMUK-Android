package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun ChatMessageBubble(
    text: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isMine) JjikmukTheme.colors.brandStrong else JjikmukTheme.colors.disabled
    val textColor = if (isMine) JjikmukTheme.colors.surface else JjikmukTheme.colors.textPrimary
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMine) {
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 0.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 292.dp)
                    .background(
                        color = bubbleColor,
                        shape = shape,
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
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
