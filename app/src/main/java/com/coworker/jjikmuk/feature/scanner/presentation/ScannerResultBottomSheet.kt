package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerResultBottomSheet(
    result: ScannerResultUiModel,
    onDismissRequest: () -> Unit,
    onProductDetailClick: () -> Unit,
    onSecondaryActionClick: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = JjikmukTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { ScannerSheetDragHandle() },
    ) {
        ScannerResultContent(
            result = result,
            onProductDetailClick = onProductDetailClick,
            onSecondaryActionClick = onSecondaryActionClick,
        )
    }
}

@Composable
private fun ScannerSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 4.dp)
            .size(width = 37.dp, height = 5.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFFBDBDBD)),
    )
}

@Composable
private fun ScannerResultContent(
    result: ScannerResultUiModel,
    onProductDetailClick: () -> Unit,
    onSecondaryActionClick: () -> Unit,
) {
    val isWarning = result.status == ScannerResultStatus.Warning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(if (isWarning) 20.dp else 26.dp))
        Image(
            painter = painterResource(result.productImageRes),
            contentDescription = result.productName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 256.dp, height = 234.dp),
        )
        Text(
            text = if (isWarning) "Warning" else "SAFE",
            color = if (isWarning) JjikmukTheme.colors.error else JjikmukTheme.colors.brand,
            style = JjikmukTheme.typography.brandLogo,
        )
        Text(
            text = result.productName,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.h2,
        )
        Spacer(modifier = Modifier.height(if (isWarning) 17.dp else 27.dp))

        if (isWarning) {
            WarningNotice()
            Spacer(modifier = Modifier.height(8.dp))
        }

        NutrientPanel(nutrients = result.nutrients)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "※ 1회 제공량(100g) 당 성분 함량을 제공합니다",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.caption,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        ResultButton(
            label = "상품 상세페이지",
            primary = true,
            onClick = onProductDetailClick,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ResultButton(
            label = if (isWarning) "대체 상품 추천받기" else "챗봇에게 질문하기",
            primary = false,
            onClick = onSecondaryActionClick,
        )
    }
}

@Composable
private fun WarningNotice() {
    Row(
        modifier = Modifier
            .width(321.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(JjikmukTheme.colors.warning),
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(86.dp)
                .background(JjikmukTheme.colors.error),
        )
        Image(
            painter = painterResource(R.drawable.ic_scanner_result_warning),
            contentDescription = null,
            modifier = Modifier.padding(start = 20.dp, top = 19.dp).size(24.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp, top = 16.dp, end = 12.dp)) {
            Text(
                text = "섭취 주의",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.titleM,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "사용자님에게 위험한 성분이 포함되어 있습니다.",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.bodyS,
            )
        }
    }
}

@Composable
private fun NutrientPanel(nutrients: List<NutrientUiModel>) {
    Row(
        modifier = Modifier
            .width(321.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JjikmukTheme.colors.info)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        nutrients.take(4).forEach { NutrientItem(it) }
    }
}

@Composable
private fun NutrientItem(nutrient: NutrientUiModel) {
    Column(
        modifier = Modifier.width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .shadow(3.dp, RoundedCornerShape(12.dp))
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(nutrient.type.iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(nutrient.type.iconRes),
                contentDescription = nutrient.type.label,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nutrient.type.label,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.caption,
            maxLines = 1,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp)) {
                    append(nutrient.value)
                }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 9.sp)) {
                    append(nutrient.type.unit)
                }
            },
            color = nutrient.type.valueColor,
            style = JjikmukTheme.typography.caption.asEnglish(),
            maxLines = 1,
        )
    }
}

@Composable
private fun ResultButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) JjikmukTheme.colors.brandStrong else Color.Transparent,
            contentColor = if (primary) Color.White else JjikmukTheme.colors.textPrimary,
        ),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, JjikmukTheme.colors.border),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = label, style = JjikmukTheme.typography.labelL)
    }
}
