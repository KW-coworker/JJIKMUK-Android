package com.coworker.jjikmuk.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.home.presentation.component.HomeEmptyContent
import com.coworker.jjikmuk.ui.component.JjikmukBottomNavigationBar
import com.coworker.jjikmuk.ui.component.JjikmukDraggableScannerFab
import com.coworker.jjikmuk.ui.component.JjikmukMessageInputBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.MainTab
import com.coworker.jjikmuk.ui.component.ImageSourceBottomSheet
import com.coworker.jjikmuk.ui.component.ScanTargetMemberUiModel
import com.coworker.jjikmuk.ui.component.ScanTargetPopup
import com.coworker.jjikmuk.ui.component.defaultScanTargetMembers
import com.coworker.jjikmuk.ui.component.rememberJjikmukImageSourceLauncher
import com.coworker.jjikmuk.ui.component.toScanTargetProfiles
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSendMessage: (String) -> Unit,
    onScannerClick: () -> Unit = {},
    selectedTab: MainTab = MainTab.Home,
    onTabClick: (MainTab) -> Unit = {},
    onChatHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showImageSourceSheet by rememberSaveable { mutableStateOf(false) }
    var showScanTargetPopup by rememberSaveable { mutableStateOf(false) }
    var inputMessage by rememberSaveable { mutableStateOf("") }
    val imageSourceLauncher = rememberJjikmukImageSourceLauncher(
        onCameraFinished = { showImageSourceSheet = true },
        onGalleryFinished = { showImageSourceSheet = true },
    )
    val scanTargetMembers = remember {
        mutableStateListOf<ScanTargetMemberUiModel>().apply {
            addAll(defaultScanTargetMembers())
        }
    }
    val selectedProfiles = scanTargetMembers.toScanTargetProfiles(
        defaultImageResId = R.drawable.ic_launcher_foreground,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            JjikmukTopAppBar(
                selectedProfiles = selectedProfiles,
                onChatHistoryClick = onChatHistoryClick,
                onScanTargetClick = { showScanTargetPopup = true },
            )
        },
        bottomBar = {
            JjikmukBottomNavigationBar(
                selectedTab = selectedTab,
                onTabClick = onTabClick,
            )
        },
    ) { innerPadding ->
        HomeContent(
            innerPadding = innerPadding,
            scanTargetMembers = scanTargetMembers,
            showScanTargetPopup = showScanTargetPopup,
            inputMessage = inputMessage,
            onInputMessageChange = { inputMessage = it },
            onAddClick = { showImageSourceSheet = true },
            onSendClick = {
                val message = inputMessage.trim()
                if (message.isNotEmpty()) {
                    inputMessage = ""
                    onSendMessage(message)
                }
            },
            onScannerClick = onScannerClick,
            onScanTargetDismiss = { showScanTargetPopup = false },
            onScanTargetCheckedChange = { memberId, checked ->
                val memberIndex = scanTargetMembers.indexOfFirst { member -> member.id == memberId }
                if (memberIndex >= 0) {
                    scanTargetMembers[memberIndex] = scanTargetMembers[memberIndex].copy(
                        isSelected = checked,
                    )
                }
            },
        )
    }

    if (showImageSourceSheet) {
        ImageSourceBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            onCameraClick = {
                showImageSourceSheet = false
                imageSourceLauncher.openCamera()
            },
            onGalleryClick = {
                showImageSourceSheet = false
                imageSourceLauncher.openGallery()
            },
        )
    }
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    scanTargetMembers: List<ScanTargetMemberUiModel>,
    showScanTargetPopup: Boolean,
    inputMessage: String,
    onInputMessageChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onSendClick: () -> Unit,
    onScannerClick: () -> Unit,
    onScanTargetDismiss: () -> Unit,
    onScanTargetCheckedChange: (memberId: String, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.background)
            .padding(innerPadding),
    ) {
        HomeEmptyContent(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),
        )

        JjikmukMessageInputBar(
            text = inputMessage,
            placeholder = "메세지를 입력하세요",
            onTextChange = onInputMessageChange,
            onAddClick = onAddClick,
            onSendClick = onSendClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 223.dp)
                .fillMaxWidth(),
        )

        JjikmukDraggableScannerFab(
            onClick = onScannerClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 21.dp, bottom = 16.dp),
        )

        if (showScanTargetPopup) {
            ScanTargetPopup(
                members = scanTargetMembers,
                onMemberCheckedChange = onScanTargetCheckedChange,
                onDismissRequest = onScanTargetDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 15.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun HomeScreenPreview() {
    JjikmukTheme {
        HomeScreen(
            onSendMessage = {},
        )
    }
}
