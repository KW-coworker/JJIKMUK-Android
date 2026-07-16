package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukTopAppBar(
    selectedProfiles: List<ScanTargetProfileUiModel>,
    onChatHistoryClick: () -> Unit,
    onScanTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatHistoryButton(
            onClick = onChatHistoryClick,
        )
        ScanTargetButton(
            selectedProfiles = selectedProfiles,
            onClick = onScanTargetClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukTopAppBarPreview() {
    JjikmukTheme {
        JjikmukTopAppBar(
            selectedProfiles = listOf(
                ScanTargetProfileUiModel(
                    id = "me",
                    imageResId = R.drawable.ic_launcher_foreground,
                ),
                ScanTargetProfileUiModel(
                    id = "spouse",
                    imageResId = R.drawable.ic_launcher_foreground,
                    emoji = "👨🏻",
                ),
                ScanTargetProfileUiModel(
                    id = "child",
                    imageResId = R.drawable.ic_launcher_foreground,
                    emoji = "👶🏻",
                ),
            ),
            onChatHistoryClick = {},
            onScanTargetClick = {},
        )
    }
}
