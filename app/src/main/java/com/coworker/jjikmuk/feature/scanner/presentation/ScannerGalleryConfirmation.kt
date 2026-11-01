package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File

/** Selecting a gallery photo never sends it. Only the primary button authorizes OCR. */
@Composable
internal fun ScannerGalleryConfirmation(photo: File, onScan: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ScannerGalleryConfirmationContent(photo = photo, onScan = onScan, onCancel = onCancel)
    }
}

@Composable
private fun ScannerGalleryConfirmationContent(photo: Any, onScan: () -> Unit, onCancel: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = JjikmukTheme.colors.surface) {
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            // Preserve the design spacing on phones; allow scrolling on short/landscape windows.
            val contentHeight = maxHeight.coerceAtLeast(650.dp)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .height(contentHeight).fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(top = 50.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("선택한 사진 확인", style = JjikmukTheme.typography.h2,
                    color = JjikmukTheme.colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("제품 포장 앞면과 제품명이 잘 보이는지 확인해 주세요.",
                    style = JjikmukTheme.typography.titleM,
                    color = JjikmukTheme.colors.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(120f))
                AsyncImage(
                    model = photo, contentDescription = "선택한 제품 사진",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(266.dp)
                        .clip(RoundedCornerShape(20.dp)).background(JjikmukTheme.colors.surfaceSecondary),
                )
                Spacer(Modifier.weight(135f))
                JjikmukPrimaryButton(text = "이 사진으로 스캔", onClick = onScan,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onCancel, modifier = Modifier.height(48.dp)) {
                    Text("취소", style = JjikmukTheme.typography.titleM,
                        color = JjikmukTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Preview(name = "갤러리 사진 확인", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerGalleryConfirmationPreview() {
    JjikmukTheme {
        ScannerGalleryConfirmationContent(R.drawable.scanner_search_preview_product, {}, {})
    }
}
