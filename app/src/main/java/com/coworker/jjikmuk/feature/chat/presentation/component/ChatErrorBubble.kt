package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.chat.presentation.ChatErrorUiModel
import com.coworker.jjikmuk.feature.chat.presentation.chatErrorFor
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.PretendardFontFamily

/**
 * 챗봇 오류 말풍선 (Figma 1812:4565 재시도 가능 / 1812:4810 재시도 중 / 1812:5057 재시도 불가).
 *
 * Figma 버튼 영역은 44dp지만 터치 영역 48dp를 위해 버튼을 48dp로 두고,
 * 한글이 없는 Inter 대신 한글 글꼴(Pretendard)을 쓴다. 말풍선 끝·아래 여백(6→4dp)과 설명-버튼 간격(2→0dp)을 줄여 원 위치와 말풍선 크기를 Figma와 같게 맞췄다.
 */
@Composable
fun ChatErrorBubble(
    error: ChatErrorUiModel,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatBotProfile()
        Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))
        Column(
            modifier = Modifier
                .width(252.dp)
                .background(color = JjikmukTheme.colors.alert, shape = ErrorBubbleShape)
                .padding(
                    start = 22.dp,
                    top = 17.dp,
                    end = if (error.retryable) 4.dp else 22.dp,
                    bottom = if (error.retryable) 4.dp else 18.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_chat_error_warning),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = error.title,
                    style = ErrorTitleStyle,
                    color = JjikmukTheme.colors.textPrimary,
                )
            }
            if (error.retryable) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = error.description.keepWordsTogether(),
                        style = ErrorDescriptionStyle,
                        color = JjikmukTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 22.dp, bottom = 13.dp),
                    )
                    ChatErrorRetryButton(
                        isRetrying = error.isRetrying,
                        onClick = onRetryClick,
                    )
                }
            } else {
                Text(
                    text = error.description.keepWordsTogether(),
                    style = ErrorDescriptionStyle,
                    color = JjikmukTheme.colors.textPrimary,
                    modifier = Modifier.padding(start = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatErrorRetryButton(
    isRetrying: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = !isRetrying, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (isRetrying) "다시 시도하는 중" else "다시 시도" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isRetrying) JjikmukTheme.colors.alertActionDisabled else JjikmukTheme.colors.alertAction,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isRetrying) {
                val rotation by rememberInfiniteTransition(label = "chatErrorRetry").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "chatErrorRetryRotation",
                )
                Image(
                    painter = painterResource(R.drawable.ic_chat_error_spinner),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_chat_error_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Figma처럼 어절 단위로 줄바꿈되도록 글자 사이에 WORD JOINER를 넣는다("주세/요." 방지). */
private fun String.keepWordsTogether(): String =
    split(' ').joinToString(" ") { word -> word.toList().joinToString("\u2060") }

private val ErrorBubbleShape = RoundedCornerShape(
    topStart = 25.dp,
    topEnd = 25.dp,
    bottomEnd = 25.dp,
    bottomStart = 0.dp,
)

private val ErrorTitleStyle = TextStyle(
    fontFamily = PretendardFontFamily,
    fontSize = 13.sp,
    lineHeight = 17.29.sp,
    fontWeight = FontWeight.Bold,
)

private val ErrorDescriptionStyle = TextStyle(
    fontFamily = PretendardFontFamily,
    fontSize = 12.sp,
    lineHeight = 17.4.sp,
    fontWeight = FontWeight.Normal,
)

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun ChatErrorBubblePreview() {
    JjikmukTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ChatErrorBubble(chatErrorFor("NETWORK_UNAVAILABLE"), onRetryClick = {})
            ChatErrorBubble(chatErrorFor("NETWORK_UNAVAILABLE").copy(isRetrying = true), onRetryClick = {})
            ChatErrorBubble(chatErrorFor("ANALYSIS_SELECTION_CHANGED"), onRetryClick = {})
        }
    }
}
