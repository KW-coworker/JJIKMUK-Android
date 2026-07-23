package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.Edit
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.Neutral100
import com.coworker.jjikmuk.ui.theme.Neutral200
import com.coworker.jjikmuk.ui.theme.Neutral50
import com.coworker.jjikmuk.ui.theme.Neutral600
import com.coworker.jjikmuk.ui.theme.Neutral900
import com.coworker.jjikmuk.ui.theme.White

data class ScanTargetMemberUiModel(
    val id: String,
    val name: String,
    val relation: String,
    val emoji: String? = null,
    val isSelected: Boolean,
)

@Composable
fun ScanTargetPopup(
    members: List<ScanTargetMemberUiModel>,
    onMemberCheckedChange: (memberId: String, checked: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .border(
                width = 1.dp,
                color = Neutral100,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ScanTargetPopupHeader(
                onDismissRequest = onDismissRequest,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                members.forEach { member ->
                    ScanTargetMemberRow(
                        member = member,
                        onCheckedChange = { checked ->
                            onMemberCheckedChange(member.id, checked)
                        },
                    )
                }
            }
            ScanTargetPopupFooter()
        }
    }
}

@Composable
private fun ScanTargetPopupHeader(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Neutral50)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "♧",
                color = Edit,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "스캔 대상 설정",
                color = Neutral900,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "×",
            color = Neutral600,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable(onClick = onDismissRequest),
        )
    }
}

@Composable
private fun ScanTargetMemberRow(
    member: ScanTargetMemberUiModel,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!member.isSelected) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScanTargetAvatar(
            emoji = member.emoji,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = member.name,
                color = Neutral900,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = member.relation,
                color = Neutral600,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Neutral100)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        ScanTargetSwitch(
            checked = member.isSelected,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ScanTargetAvatar(
    emoji: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Neutral100)
            .border(width = 1.dp, color = Neutral200, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ScanTargetSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (checked) Edit else Neutral100
    val thumbAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = thumbAlignment,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(White),
        )
    }
}

@Composable
private fun ScanTargetPopupFooter(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Neutral50)
            .border(width = 1.dp, color = Neutral200)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "활성화된 가족의 알레르기만 검사합니다.",
            color = Neutral600,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanTargetPopupPreview() {
    JjikmukTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(24.dp),
        ) {
            ScanTargetPopup(
                members = listOf(
                    ScanTargetMemberUiModel(
                        id = "me",
                        name = "코워커",
                        relation = "나",
                        isSelected = true,
                    ),
                    ScanTargetMemberUiModel(
                        id = "spouse",
                        name = "김철수",
                        relation = "배우자",
                        emoji = "👨🏻",
                        isSelected = true,
                    ),
                    ScanTargetMemberUiModel(
                        id = "child",
                        name = "김아기",
                        relation = "자녀",
                        emoji = "👶🏻",
                        isSelected = false,
                    ),
                ),
                onMemberCheckedChange = { _, _ -> },
                onDismissRequest = {},
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
