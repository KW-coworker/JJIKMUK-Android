package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

data class ScanTargetMemberUiModel(
    val id: String,
    val name: String,
    val relation: String,
    val emoji: String? = null,
    val isSelected: Boolean,
    val vegetarian: String = "",
    val allergies: Set<String> = emptySet(),
    val preferences: Set<String> = emptySet(),
)

@Composable
fun ScanTargetPopup(
    members: List<ScanTargetMemberUiModel>,
    onMemberCheckedChange: (memberId: String, checked: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    enabled: Boolean = true,
    onRefresh: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .height(if (statusMessage == null) 306.dp else 378.dp)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.disabled,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ScanTargetPopupHeader(
                onDismissRequest = onDismissRequest,
            )
            if (statusMessage != null) {
                Text(statusMessage, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = JjikmukTheme.colors.textSecondary, maxLines = 2)
                if (onRefresh != null) Text("최신 선택 다시 확인", Modifier.clickable(onClick = onRefresh).padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = JjikmukTheme.colors.brand)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(208.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                items(members, key = { it.id }) { member ->
                    ScanTargetMemberRow(
                        member = member,
                        onCheckedChange = { checked ->
                            if (enabled) onMemberCheckedChange(member.id, checked)
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
            .height(54.dp)
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_scan_target_people),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "스캔 대상 설정",
                color = JjikmukTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier.size(16.dp).clickable(onClick = onDismissRequest)
                .semantics { contentDescription = "닫기" },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_scan_target_close_first), null, Modifier.size(9.33333.dp))
            Image(painterResource(R.drawable.ic_scan_target_close_second), null, Modifier.size(9.33333.dp))
        }
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
            isMe = member.id == "me",
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = member.name,
                color = JjikmukTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = member.relation,
                color = JjikmukTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(JjikmukTheme.colors.disabled)
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
    isMe: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(JjikmukTheme.colors.disabled)
            .border(width = 1.dp, color = JjikmukTheme.colors.borderSubtle, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        com.coworker.jjikmuk.feature.auth.photo.AccountAvatarContent(isMe) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                )
            }
        }
    }
}

@Composable
private fun ScanTargetSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (checked) JjikmukTheme.colors.edit else JjikmukTheme.colors.disabled
    val thumbAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = thumbAlignment,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(JjikmukTheme.colors.surface),
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
            .height(42.dp)
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(width = 1.dp, color = JjikmukTheme.colors.borderSubtle)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "활성화된 가족의 알레르기만 검사합니다.",
            color = JjikmukTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanTargetPopupPreview() {
    JjikmukTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(JjikmukTheme.colors.surface)
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
                modifier = Modifier.offset(x = maxWidth - 240.dp, y = 39.dp),
            )
        }
    }
}
