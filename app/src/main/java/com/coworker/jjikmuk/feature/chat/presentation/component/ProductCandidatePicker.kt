package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun ProductCandidatePicker(
    candidates: List<ChatProductCandidate>,
    onCandidateClick: (ChatProductCandidate) -> Unit,
    onShowMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return
    val showMoreInteractionSource = remember { MutableInteractionSource() }
    val isShowMorePressed by showMoreInteractionSource.collectIsPressedAsState()
    val showMoreContainerColor = if (isShowMorePressed) {
        JjikmukTheme.colors.brandPressed
    } else {
        JjikmukTheme.colors.brandStrong
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(449.dp),
        color = JjikmukTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 37.dp, height = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(JjikmukTheme.colors.border),
            )

            candidates.take(MAX_VISIBLE_CANDIDATE_COUNT).forEach { candidate ->
                ProductCandidateCard(
                    candidate = candidate,
                    onClick = { onCandidateClick(candidate) },
                )
            }

            Button(
                onClick = onShowMoreClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = showMoreContainerColor),
                interactionSource = showMoreInteractionSource,
            ) {
                Text(
                    text = "상품 더보기",
                    color = JjikmukTheme.colors.surface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProductCandidateCard(
    candidate: ChatProductCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(JjikmukTheme.colors.surface)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.border,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = candidate.brandName,
                color = JjikmukTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = candidate.productName,
                color = JjikmukTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                modifier = Modifier.padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                candidate.allergyLabels.take(MAX_VISIBLE_ALLERGY_COUNT).forEach { label ->
                    AllergyChip(label = label)
                }
            }
        }

        ProductImagePlaceholder()
    }
}

@Composable
private fun AllergyChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val emoji = when (label) {
        "우유" -> "🥛"
        "땅콩" -> "🥜"
        "새우" -> "🦐"
        "밀" -> "🌾"
        "대두" -> "🫘"
        else -> "·"
    }

    Text(
        text = "$emoji $label",
        color = JjikmukTheme.colors.error,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JjikmukTheme.colors.warning)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.warning,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun ProductImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(92.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JjikmukTheme.colors.disabled)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "상품\n이미지",
            color = JjikmukTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private const val MAX_VISIBLE_CANDIDATE_COUNT = 2
private const val MAX_VISIBLE_ALLERGY_COUNT = 3

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun ProductCandidatePickerPreview() {
    JjikmukTheme {
        ProductCandidatePicker(
            candidates = listOf(
                ChatProductCandidate(
                    productName = "농심 새우깡 90g",
                    brandName = "과자",
                    barcode = "8801043035989",
                    rawMaterials = null,
                    allergyLabels = listOf("밀", "새우", "대두"),
                    reason = null,
                ),
                ChatProductCandidate(
                    productName = "농심 매운 새우깡 90g",
                    brandName = "과자",
                    barcode = "8801043036078",
                    rawMaterials = null,
                    allergyLabels = listOf("밀", "새우", "대두"),
                    reason = null,
                ),
            ),
            onCandidateClick = {},
            onShowMoreClick = {},
        )
    }
}
