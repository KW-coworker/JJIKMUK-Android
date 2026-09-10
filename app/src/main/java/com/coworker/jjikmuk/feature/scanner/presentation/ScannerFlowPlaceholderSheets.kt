package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ScannerResultPlaceholder {
    Safe,
    Warning,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerResultPlaceholderSheet(
    result: ScannerResultPlaceholder,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        PlaceholderSheetContent(
            title = if (result == ScannerResultPlaceholder.Safe) {
                "일반 스캔 - 안심 상품 (임시)"
            } else {
                "일반 스캔 - 위험 상품 (임시)"
            },
            description = "다음 단계에서 Figma UI로 교체할 화면입니다.",
            onDismissRequest = onDismissRequest,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerCompareListPlaceholderSheet(
    productCount: Int,
    onDismissRequest: () -> Unit,
    onCompareClick: () -> Unit,
) {
    val canCompare = productCount >= 2

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        PlaceholderSheetContent(
            title = "비교 스캔 목록 (임시)",
            description = if (canCompare) {
                "스캔된 상품 ${productCount}개"
            } else {
                "스캔 상품 수 부족: 제품 2개 이상부터 비교할 수 있습니다."
            },
            onDismissRequest = onDismissRequest,
            primaryLabel = "${productCount}개 상품 비교하기",
            primaryEnabled = canCompare,
            onPrimaryClick = onCompareClick,
        )
    }
}

@Composable
private fun PlaceholderSheetContent(
    title: String,
    description: String,
    onDismissRequest: () -> Unit,
    primaryLabel: String? = null,
    primaryEnabled: Boolean = true,
    onPrimaryClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description)
        Spacer(modifier = Modifier.height(24.dp))
        if (primaryLabel != null) {
            Button(
                onClick = onPrimaryClick,
                enabled = primaryEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = primaryLabel)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "취소")
        }
    }
}
