package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerCompareListBottomSheet(
    productCount: Int,
    onDismissRequest: () -> Unit,
    onRemoveProduct: () -> Unit,
    onCompareClick: () -> Unit,
) {
    val canCompare = productCount >= 2
    var showInsufficientNotice by rememberSaveable(productCount) {
        mutableStateOf(!canCompare)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = JjikmukTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { CompareSheetDragHandle() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(316.dp),
        ) {
            CompareProductList(
                productCount = productCount,
                onRemoveProduct = onRemoveProduct,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp),
            )

            CompareActionButtons(
                productCount = productCount,
                canCompare = canCompare,
                onCancelClick = onDismissRequest,
                onCompareClick = onCompareClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp),
            )

            if (showInsufficientNotice) {
                InsufficientProductsNotice(
                    onCloseClick = { showInsufficientNotice = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 46.dp),
                )
            }
        }
    }
}

@Composable
private fun CompareProductList(
    productCount: Int,
    onRemoveProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val viewportHeight = 202.dp
    val rowStride = 70.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val viewportPx = with(density) { viewportHeight.toPx() }
    val rowStridePx = with(density) { rowStride.toPx() }
    val contentPx = productCount * rowStridePx - with(density) { 8.dp.toPx() }
    val thumbHeightPx = if (productCount > 3) {
        (viewportPx * viewportPx / contentPx).coerceAtLeast(with(density) { 32.dp.toPx() })
    } else {
        viewportPx
    }
    val maxThumbOffsetPx = (viewportPx - thumbHeightPx).coerceAtLeast(0f)
    val maxScrollPx = (contentPx - viewportPx).coerceAtLeast(1f)
    val thumbOffsetPx by remember(listState, maxThumbOffsetPx, maxScrollPx) {
        derivedStateOf {
            val scrollPx = listState.firstVisibleItemIndex * rowStridePx +
                listState.firstVisibleItemScrollOffset
            (scrollPx / maxScrollPx).coerceIn(0f, 1f) * maxThumbOffsetPx
        }
    }

    Box(
        modifier = modifier
            .width(342.dp)
            .height(viewportHeight),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .padding(end = if (productCount > 3) 7.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(productCount, key = { it }) {
                CompareProductRow(onRemoveClick = onRemoveProduct)
            }
        }

        if (productCount > 3) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(4.dp)
                    .height(viewportHeight)
                    .clip(RoundedCornerShape(100.dp))
                    .background(JjikmukTheme.colors.borderSubtle),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                    .width(4.dp)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .clip(RoundedCornerShape(100.dp))
                    .background(JjikmukTheme.colors.textTertiary)
                    .pointerInput(productCount) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                listState.scrollBy(dragAmount * maxScrollPx / maxThumbOffsetPx)
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun CompareSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 4.dp)
            .size(width = 37.dp, height = 5.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(JjikmukTheme.colors.textDisabled),
    )
}

@Composable
private fun CompareProductRow(onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, JjikmukTheme.colors.border, RoundedCornerShape(16.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.img_scanner_compare_sample_product),
            contentDescription = "오트밀 라이트",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "퀘이커",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.caption,
            )
            Text(
                text = "오트밀 라이트",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.labelM,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_scanner_compare_remove),
            contentDescription = "상품 삭제",
            modifier = Modifier
                .size(36.dp)
                .clickable(role = Role.Button, onClick = onRemoveClick)
                .padding(8.dp),
        )
    }
}

@Composable
private fun InsufficientProductsNotice(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .width(343.dp)
            .height(69.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JjikmukTheme.colors.info)
            .border(1.dp, JjikmukTheme.colors.edit, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_scanner_compare_info),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "스캔 상품 수 부족",
                color = Color(0xFF1E3A8A),
                style = JjikmukTheme.typography.titleM,
            )
            Text(
                text = "제품 2개 이상부터 비교 스캔이 가능합니다",
                color = Color(0xFF1E3A8A),
                style = JjikmukTheme.typography.caption,
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_scanner_compare_toast_close),
            contentDescription = "안내 닫기",
            modifier = Modifier
                .size(16.dp)
                .clickable(role = Role.Button, onClick = onCloseClick),
        )
    }
}

@Composable
private fun CompareActionButtons(
    productCount: Int,
    canCompare: Boolean,
    onCancelClick: () -> Unit,
    onCompareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(342.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompareButton(
            label = "취소",
            onClick = onCancelClick,
            modifier = Modifier.width(100.dp),
        )
        CompareButton(
            label = "${productCount}개 상품 비교하기",
            onClick = onCompareClick,
            enabled = canCompare,
            primary = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompareButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(47.dp),
        shape = RoundedCornerShape(17.dp),
        border = if (primary && enabled) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            JjikmukTheme.colors.border,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) JjikmukTheme.colors.brandStrong else JjikmukTheme.colors.surface,
            contentColor = if (primary) JjikmukTheme.colors.surface else JjikmukTheme.colors.textPrimary,
            disabledContainerColor = JjikmukTheme.colors.disabled,
            disabledContentColor = JjikmukTheme.colors.textTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = label, style = JjikmukTheme.typography.labelM, maxLines = 1)
    }
}
