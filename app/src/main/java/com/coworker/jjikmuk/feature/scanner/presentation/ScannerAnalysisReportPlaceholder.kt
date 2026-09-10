package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScannerAnalysisReportPlaceholder(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPage by rememberSaveable { mutableIntStateOf(1) }
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "비교 스캔 분석 레포트 (임시)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "현재 카드: 레포트 $selectedPage")
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { selectedPage = 1 }) { Text("레포트 1") }
            OutlinedButton(onClick = { selectedPage = 2 }) { Text("레포트 2") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("비교 스캔 메인으로 돌아가기")
        }
    }
}
