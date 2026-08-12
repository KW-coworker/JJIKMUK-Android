package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.ui.component.JjikmukLoginButton
import com.coworker.jjikmuk.ui.component.JjikmukProductListCard
import com.coworker.jjikmuk.ui.component.JjikmukProductListCardUiModel
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun ProductCandidatePicker(
    candidates: List<ChatProductCandidate>,
    onCandidateClick: (ChatProductCandidate) -> Unit,
    onShowMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProductCandidateOverlayColor),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ProductCandidateBottomSheet(
            candidates = candidates,
            onCandidateClick = onCandidateClick,
            onShowMoreClick = onShowMoreClick,
        )
    }
}

@Composable
private fun ProductCandidateBottomSheet(
    candidates: List<ChatProductCandidate>,
    onCandidateClick: (ChatProductCandidate) -> Unit,
    onShowMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 37.dp, height = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(JjikmukTheme.colors.border),
            )

            Spacer(modifier = Modifier.height(14.dp))

            candidates.take(MAX_VISIBLE_CANDIDATE_COUNT).forEach { candidate ->
                ProductCandidateCard(
                    candidate = candidate,
                    onClick = { onCandidateClick(candidate) },
                )
            }

            JjikmukLoginButton(
                onClick = onShowMoreClick,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProductCandidateCard(
    candidate: ChatProductCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JjikmukProductListCard(
        product = candidate.toProductListCardUiModel(),
        onClick = onClick,
        modifier = modifier,
    )
}

private fun ChatProductCandidate.toProductListCardUiModel(): JjikmukProductListCardUiModel =
    JjikmukProductListCardUiModel(
        brandName = brandName,
        productName = productName,
        barcode = barcode,
        imageUrl = null,
        allergyLabels = allergyLabels,
    )

private const val MAX_VISIBLE_CANDIDATE_COUNT = 2
private val ProductCandidateOverlayColor = Color.Black.copy(alpha = 0.25f)

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
