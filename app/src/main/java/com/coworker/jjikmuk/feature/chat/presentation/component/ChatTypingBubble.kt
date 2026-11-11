package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

private const val DOT_COUNT = 3
private const val DOT_DELAY_MILLIS = 150
private const val DOT_HALF_CYCLE_MILLIS = 600
private const val DOT_MIN_ALPHA = 0.3f
private val DotSize = 7.dp
private val DotLift = 4.dp

/**
 * 답변을 기다리는 동안 보이는 "• • •" 말풍선 (Figma C4 WaitingBubble).
 * 아바타·말풍선 모양·위치는 기존 봇 말풍선과 같고, 점 3개가 150ms씩 늦게 물결치듯 계속 움직인다.
 */
@Composable
fun ChatTypingBubble(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "답변을 준비 중이에요" },
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatBotProfile()
        Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))
        Row(
            modifier = Modifier
                .height(54.dp)
                .background(color = JjikmukTheme.colors.disabled, shape = BotBubbleShape)
                .padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "chatTypingDots")
            repeat(DOT_COUNT) { index -> TypingDot(transition, index) }
        }
    }
}

@Composable
private fun TypingDot(transition: InfiniteTransition, index: Int) {
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DOT_HALF_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(index * DOT_DELAY_MILLIS),
        ),
        label = "chatTypingDot$index",
    )
    val liftPx = with(LocalDensity.current) { DotLift.toPx() }
    Box(
        modifier = Modifier
            .size(DotSize)
            .graphicsLayer {
                alpha = DOT_MIN_ALPHA + (1f - DOT_MIN_ALPHA) * progress
                translationY = -liftPx * progress
            }
            .background(color = JjikmukTheme.colors.textTertiary, shape = CircleShape),
    )
}

/** 기존 봇 말풍선(ChatMessageBubble)과 같은 모양. */
private val BotBubbleShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 24.dp,
)

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun ChatTypingBubblePreview() {
    JjikmukTheme {
        ChatTypingBubble(modifier = Modifier.padding(20.dp))
    }
}
