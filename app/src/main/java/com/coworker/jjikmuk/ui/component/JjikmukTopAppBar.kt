package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

sealed interface JjikmukTopAppBarLeading {
    data class ChatHistory(val onClick: () -> Unit) : JjikmukTopAppBarLeading
    data class Back(val onClick: () -> Unit) : JjikmukTopAppBarLeading
    data class Custom(
        val padding: Dp,
        val content: @Composable () -> Unit,
    ) : JjikmukTopAppBarLeading

    data object None : JjikmukTopAppBarLeading
}

@Composable
fun JjikmukTopAppBar(
    selectedProfiles: List<ScanTargetProfileUiModel>,
    onChatHistoryClick: () -> Unit,
    onScanTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBottomDivider: Boolean = false,
) {
    JjikmukTopAppBar(
        selectedProfiles = selectedProfiles,
        onScanTargetClick = onScanTargetClick,
        modifier = modifier,
        showBottomDivider = showBottomDivider,
        leading = JjikmukTopAppBarLeading.ChatHistory(onClick = onChatHistoryClick),
    )
}

@Composable
fun JjikmukTopAppBar(
    selectedProfiles: List<ScanTargetProfileUiModel>,
    onScanTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBottomDivider: Boolean = false,
    leading: JjikmukTopAppBarLeading = JjikmukTopAppBarLeading.None,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    JjikmukTopAppBar(
        modifier = modifier,
        showBottomDivider = showBottomDivider,
        leading = leading,
        centerContent = centerContent,
        trailingContent = {
            ScanTargetButton(
                selectedProfiles = selectedProfiles,
                onClick = onScanTargetClick,
            )
        },
    )
}

@Composable
fun JjikmukTopAppBar(
    modifier: Modifier = Modifier,
    showBottomDivider: Boolean = false,
    leading: JjikmukTopAppBarLeading = JjikmukTopAppBarLeading.None,
    centerContent: @Composable BoxScope.() -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    val leadingPadding = when (leading) {
        is JjikmukTopAppBarLeading.Back -> 22.dp
        is JjikmukTopAppBarLeading.ChatHistory -> 28.dp
        is JjikmukTopAppBarLeading.Custom -> leading.padding
        JjikmukTopAppBarLeading.None -> 0.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = leadingPadding),
                contentAlignment = Alignment.Center,
            ) {
                when (leading) {
                    is JjikmukTopAppBarLeading.Back -> {
                        JjikmukBackButton(onClick = leading.onClick)
                    }

                    is JjikmukTopAppBarLeading.ChatHistory -> {
                        ChatHistoryButton(onClick = leading.onClick)
                    }

                    is JjikmukTopAppBarLeading.Custom -> {
                        leading.content()
                    }

                    JjikmukTopAppBarLeading.None -> Unit
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center),
            ) {
                centerContent()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                trailingContent()
            }
            if (showBottomDivider) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(JjikmukTheme.colors.borderSubtle),
                )
            }
        }
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
