package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File

/** Selecting a gallery photo never sends it. Only the primary button authorizes OCR. */
@Composable
internal fun ScannerGalleryConfirmation(photo: File, onScan: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = JjikmukTheme.colors.surface) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("선택한 사진 확인", style = JjikmukTheme.typography.h2)
                Text("제품 포장 앞면과 제품명이 잘 보이는지 확인해 주세요.",
                    style = JjikmukTheme.typography.bodyM)
                AsyncImage(model = photo, contentDescription = "선택한 제품 사진",
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f))
                JjikmukPrimaryButton(text = "이 사진으로 스캔", onClick = onScan,
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onCancel) { Text("취소") }
            }
        }
    }
}
