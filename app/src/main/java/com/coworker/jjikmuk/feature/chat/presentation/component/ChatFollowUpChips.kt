package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/**
 * 봇 답변 아래 후속 질문 칩 (Figma C1~C3 FollowUpChip).
 *
 * 각 칩은 48dp 높이 터치 영역 안에 34dp 칩을 그리고, 터치 영역끼리 붙여 줄 간격 14dp가 되게 한다.
 * 칩은 봇 말풍선 왼쪽 끝(아바타 26dp + 간격 10dp)에 맞추고, 보이는 칩이 말풍선 아래 8dp에서 시작하도록
 * 터치 영역 여백(7dp)을 뺀 1dp만 위에 둔다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatFollowUpChips(
    questions: List<String>,
    onQuestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (questions.isEmpty()) return
    FlowRow(
        modifier = modifier.padding(start = 36.dp, top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        questions.forEach { question ->
            ChatFollowUpChip(text = question, onClick = { onQuestionClick(question) })
        }
    }
}

@Composable
private fun ChatFollowUpChip(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .widthIn(min = 48.dp, max = 252.dp)
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .background(
                    color = if (isPressed) JjikmukTheme.colors.brandSubtlest else JjikmukTheme.colors.surface,
                    shape = CircleShape,
                )
                .border(width = 1.dp, color = JjikmukTheme.colors.brandStrong, shape = CircleShape)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = JjikmukTheme.typography.bodyS,
                color = JjikmukTheme.colors.brandStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun ChatFollowUpChipsPreview() {
    JjikmukTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            ChatMessageBubble(text = "새우깡에는 밀가루, 새우, 팜유, 설탕 등이 들어 있어요.", isMine = false)
            ChatFollowUpChips(
                questions = listOf(
                    "이거 먹어도 돼?",
                    "대두 들어 있어?",
                    "이 제품 원재료 중에 알레르기를 일으킬 수 있는 성분이 있는지 자세히 알려줘",
                ),
                onQuestionClick = {},
            )
        }
    }
}
