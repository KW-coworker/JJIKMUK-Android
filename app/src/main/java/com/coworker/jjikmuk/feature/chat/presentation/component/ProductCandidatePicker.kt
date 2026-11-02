package com.coworker.jjikmuk.feature.chat.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
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
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return
    var showAll by rememberSaveable(candidates) { mutableStateOf(false) }
    BackHandler { if (showAll) showAll = false else onConfirmClick() }
    if (showAll) {
        Surface(modifier = modifier.fillMaxSize(), color = JjikmukTheme.colors.surface) {
            Column(Modifier.fillMaxSize()) {
                JjikmukTopAppBar(
                    centerContent = { Text("상품 선택") },
                    leading = JjikmukTopAppBarLeading.Back { showAll = false },
                )
                Text("질문할 상품을 선택해 주세요. (${candidates.size}개)",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = JjikmukTheme.typography.bodyS)
                LazyColumn(contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(candidates) { candidate ->
                        ProductCandidateCard(candidate, onClick = { onCandidateClick(candidate) })
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProductCandidateOverlayColor),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ProductCandidateBottomSheet(
            candidates = candidates,
            onCandidateClick = onCandidateClick,
            onConfirmClick = { if (candidates.size > MAX_VISIBLE_CANDIDATE_COUNT) showAll = true else onConfirmClick() },
        )
    }
}

@Composable
private fun ProductCandidateBottomSheet(
    candidates: List<ChatProductCandidate>,
    onCandidateClick: (ChatProductCandidate) -> Unit,
    onConfirmClick: () -> Unit,
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
                text = if (candidates.size > MAX_VISIBLE_CANDIDATE_COUNT) "더보기" else "확인",
                onClick = onConfirmClick,
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
            onConfirmClick = {},
        )
    }
}
