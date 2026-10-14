package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/** Shown only for an OCR product-name recognition failure, not for camera/network errors. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerRecognitionFailureBottomSheet(
    onDismissRequest: () -> Unit,
    onRetakeClick: () -> Unit,
    onManualSearchClick: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = JjikmukTheme.colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        RecognitionFailureContent(onRetakeClick, onManualSearchClick)
    }
}

@Composable
private fun RecognitionFailureContent(
    onRetakeClick: () -> Unit,
    onManualSearchClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 11.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.scanner_sheet_handle), null, Modifier.size(37.dp, 5.dp))
        Spacer(Modifier.height(33.dp))
        Box(
            modifier = Modifier.size(56.dp).background(Color(0xFFFEF3F2), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_scanner_recognition_failure), null, Modifier.size(28.dp))
        }
        Spacer(Modifier.height(21.dp))
        Text("제품명 인식 불가", style = JjikmukTheme.typography.h3,
            color = JjikmukTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 26.dp))
        Spacer(Modifier.height(5.dp))
        Text("아래 내용을 확인하고 다시 찍어주세요", style = JjikmukTheme.typography.bodyS,
            color = JjikmukTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 18.dp))
        Spacer(Modifier.height(31.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(JjikmukTheme.colors.surfaceSecondary, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecognitionTip("제품명이 적힌 포장 앞면을 찍어주세요")
            RecognitionTip("네모칸 안에 제품이 꽉 차도록 맞춰주세요")
            RecognitionTip("빛 반사나 흔들림 없이 밝은 곳에서 찍어주세요")
        }
        Spacer(Modifier.height(24.dp))
        JjikmukPrimaryButton(text = "다시 촬영하기", onClick = onRetakeClick)
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .clickable(role = Role.Button, onClick = onManualSearchClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("제품명 직접 검색하기", style = JjikmukTheme.typography.titleM,
                color = JjikmukTheme.colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RecognitionTip(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.ic_scanner_tip_check), null,
            Modifier.padding(top = 2.dp).size(14.dp))
        Text(text, style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary,
            modifier = Modifier.heightIn(min = 18.dp))
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerRecognitionFailurePreview() {
    JjikmukTheme {
        var showFailure by remember { mutableStateOf(true) }
        ScannerScreen(
            mode = ScannerMode.Normal, onModeChange = {}, onBackClick = {}, onFlashClick = {},
            onGalleryClick = {}, onShutterClick = { showFailure = true }, onCompareListClick = {},
        )
        if (showFailure) {
            ScannerRecognitionFailureBottomSheet(
                onDismissRequest = { showFailure = false },
                onRetakeClick = { showFailure = false },
                onManualSearchClick = { showFailure = false },
            )
        }
    }
}
