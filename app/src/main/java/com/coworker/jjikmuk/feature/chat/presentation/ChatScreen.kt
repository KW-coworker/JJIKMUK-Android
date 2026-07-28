package com.coworker.jjikmuk.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.feature.chat.presentation.component.ChatMessageBubble
import com.coworker.jjikmuk.feature.chat.presentation.component.ProductCandidatePicker
import com.coworker.jjikmuk.ui.component.ImageSourceBottomSheet
import com.coworker.jjikmuk.ui.component.JjikmukMessageInputBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.component.ScanTargetProfileUiModel
import com.coworker.jjikmuk.ui.component.rememberJjikmukImageSourceLauncher
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(
    initialMessage: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialMessage) {
        viewModel.start(initialMessage)
    }

    ChatScreen(
        title = createChatTitle(initialMessage),
        uiState = uiState,
        onBackClick = onBackClick,
        onSendMessage = viewModel::sendMessage,
        onProductCandidateClick = viewModel::selectProductCandidate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    title: String,
    uiState: ChatUiState,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onProductCandidateClick: (ChatProductCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputMessage by rememberSaveable { mutableStateOf("") }
    var showImageSourceSheet by rememberSaveable { mutableStateOf(false) }
    val imageSourceLauncher = rememberJjikmukImageSourceLauncher(
        onCameraFinished = { showImageSourceSheet = true },
        onGalleryFinished = { showImageSourceSheet = true },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                title = title,
                onBackClick = onBackClick,
                selectedProfiles = defaultSelectedScanTargetProfiles(),
                onScanTargetClick = {},
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JjikmukTheme.colors.background)
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = if (uiState.productCandidates.isEmpty()) 96.dp else 372.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                items(uiState.messages, key = { message -> message.id }) { message ->
                    ChatMessageBubble(
                        text = message.text,
                        isMine = message.isMine,
                    )
                }
                if (uiState.isLoading) {
                    item {
                        ChatMessageBubble(
                            text = "답변을 준비 중이에요.",
                            isMine = false,
                        )
                    }
                }
            }

            ProductCandidatePicker(
                candidates = uiState.productCandidates,
                onCandidateClick = onProductCandidateClick,
                onShowMoreClick = {},
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.BottomCenter),
            )

            if (uiState.productCandidates.isEmpty()) {
                JjikmukMessageInputBar(
                    text = inputMessage,
                    placeholder = "메세지를 입력하세요",
                    onTextChange = { inputMessage = it },
                    onAddClick = { showImageSourceSheet = true },
                    onSendClick = {
                        val message = inputMessage.trim()
                        if (message.isNotEmpty()) {
                            inputMessage = ""
                            onSendMessage(message)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp),
                )
            }
        }
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
private fun ChatTopBar(
    title: String,
    onBackClick: () -> Unit,
    selectedProfiles: List<ScanTargetProfileUiModel>,
    onScanTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JjikmukTopAppBar(
        selectedProfiles = selectedProfiles,
        onScanTargetClick = onScanTargetClick,
        modifier = modifier,
        showBottomDivider = true,
        leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
        centerContent = {
            Text(
                text = title,
                color = JjikmukTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center),
            )
        },
    )
}

private fun defaultSelectedScanTargetProfiles(): List<ScanTargetProfileUiModel> {
    return listOf(
        ScanTargetProfileUiModel(
            id = "me",
            imageResId = R.drawable.ic_launcher_foreground,
        ),
        ScanTargetProfileUiModel(
            id = "spouse",
            imageResId = R.drawable.ic_launcher_foreground,
            emoji = "👨🏻",
        ),
    )
}

private fun createChatTitle(message: String): String {
    val trimmedMessage = message.trim()
    return if (trimmedMessage.length <= CHAT_TITLE_MAX_LENGTH) {
        trimmedMessage
    } else {
        trimmedMessage.take(CHAT_TITLE_MAX_LENGTH) + ".."
    }
}

private const val CHAT_TITLE_MAX_LENGTH = 12

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ChatScreenPreview() {
    JjikmukTheme {
        ChatScreen(
            title = "포키 블루베리 맛..",
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessageUiModel(
                        id = 1L,
                        text = "포키 블루베리 맛은 어때?",
                        isMine = true,
                    ),
                    ChatMessageUiModel(
                        id = 2L,
                        text = "포키 블루베리에 대해 확인해볼게요.",
                        isMine = false,
                    ),
                ),
            ),
            onBackClick = {},
            onSendMessage = {},
            onProductCandidateClick = {},
        )
    }
}
