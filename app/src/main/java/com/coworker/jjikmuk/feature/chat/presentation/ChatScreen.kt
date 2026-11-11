package com.coworker.jjikmuk.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.coworker.jjikmuk.feature.chat.presentation.component.ChatErrorBubble
import com.coworker.jjikmuk.feature.chat.presentation.component.ChatFollowUpChips
import com.coworker.jjikmuk.feature.chat.presentation.component.ChatMessageBubble
import com.coworker.jjikmuk.feature.chat.presentation.component.ChatTypingBubble
import com.coworker.jjikmuk.feature.chat.presentation.component.ProductCandidatePicker
import com.coworker.jjikmuk.ui.component.ImageSourceBottomSheet
import com.coworker.jjikmuk.ui.component.JjikmukMessageInputBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.component.ScanTargetProfileUiModel
import com.coworker.jjikmuk.ui.component.ScanTargetPopup
import com.coworker.jjikmuk.ui.component.toScanTargetMembers
import com.coworker.jjikmuk.ui.component.toScanTargetProfiles
import com.coworker.jjikmuk.ui.component.rememberJjikmukImageSourceLauncher
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(
    conversationId: String? = null,
    initialMessage: String,
    initialBarcode: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val targetState by viewModel.targetState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId, initialMessage, initialBarcode) {
        viewModel.start(
            conversationId = conversationId,
            initialMessage = initialMessage,
            initialBarcode = initialBarcode,
        )
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { viewModel.syncTargets() }
    ChatScreen(
        onRefreshTargets = viewModel::refreshTargets,
        title = uiState.title ?: createChatTitle(initialMessage),
        uiState = uiState,
        onBackClick = onBackClick,
        onSendMessage = viewModel::sendMessage,
        onRetry = viewModel::retry,
        onFinishTyping = viewModel::finishTyping,
        targetState = targetState,
        onTargetCheckedChange = viewModel::selectTarget,
        onProductCandidateClick = viewModel::selectProductCandidate,
        onDismissProductCandidates = viewModel::dismissProductCandidates,
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
    onDismissProductCandidates: () -> Unit = {},
    targetState: com.coworker.jjikmuk.domain.repository.AnalysisTargetsState = com.coworker.jjikmuk.domain.repository.AnalysisTargetsState(),
    onTargetCheckedChange: (String, Boolean) -> Unit = { _, _ -> },
    onRefreshTargets: () -> Unit = {},
    onRetry: () -> Unit = {},
    onFinishTyping: () -> Unit = {},
) {
    val messageListState = rememberLazyListState()
    val isDragging by messageListState.interactionSource.collectIsDraggedAsState()
    var followLatest by remember { mutableStateOf(true) }
    LaunchedEffect(isDragging) {
        if (!isDragging) followLatest = !messageListState.canScrollForward ||
            (messageListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= messageListState.layoutInfo.totalItemsCount - 2
    }
    val typing = uiState.messages.lastOrNull { it.isTyping }
    LaunchedEffect(uiState.messages.size, uiState.isLoading, typing?.displayedText?.length, uiState.productCandidates.size, uiState.error, followLatest) {
        if (followLatest && !isDragging && !messageListState.isScrollInProgress && messageListState.layoutInfo.totalItemsCount > 0) {
            messageListState.scrollToItem(messageListState.layoutInfo.totalItemsCount - 1)
        }
    }
    var showTargets by rememberSaveable { mutableStateOf(false) }
    val members = targetState.toScanTargetMembers()
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
                selectedProfiles = members.toScanTargetProfiles(R.drawable.ic_launcher_foreground),
                onScanTargetClick = { showTargets = true },
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
                state = messageListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = if (uiState.productCandidates.isEmpty()) 96.dp else 372.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // 후속 질문 칩은 가장 최근 봇 답변과 같은 item에 묶어 말풍선 아래 8dp에 붙인다(spacedBy 28dp 회피).
                val lastMessage = uiState.messages.lastOrNull()
                val suggestionsFor = lastMessage?.id?.takeIf {
                    uiState.suggestedQuestions.isNotEmpty() && !lastMessage.isMine && !lastMessage.isTyping &&
                        !uiState.isLoading && uiState.error == null && uiState.productCandidates.isEmpty()
                }
                items(uiState.messages, key = { message -> message.id }) { message ->
                    Column {
                        ChatMessageBubble(
                            text = (message.displayedText ?: message.text) + if (message.isTyping) "▍" else "",
                            isMine = message.isMine,
                        )
                        if (message.isTyping) Text("전체 보기",
                            modifier = Modifier.padding(start = 36.dp, top = 6.dp).clickable(onClick = onFinishTyping).padding(vertical = 6.dp),
                            color = JjikmukTheme.colors.textSecondary, style = JjikmukTheme.typography.bodyS)
                        if (message.id == suggestionsFor) {
                            ChatFollowUpChips(
                                questions = uiState.suggestedQuestions,
                                onQuestionClick = onSendMessage,
                            )
                        }
                    }
                }
                item {
                    Text(targetState.message ?: if (!targetState.ready) "분석 대상을 확인하고 있어요."
                        else if (targetState.selection?.mode == "none") "분석 대상 없음 · 개인 조건 없이 조회해요."
                        else "분석 대상: " + members.filter { it.isSelected }.joinToString { it.name },
                        style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary)
                }
                uiState.notice?.takeIf { uiState.error == null }?.let { message ->
                    item { Text(message, style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary) }
                    if (uiState.canRetry && !uiState.isLoading) item {
                        Text("다시 확인·재시도", Modifier.clickable(onClick = onRetry).padding(vertical = 12.dp),
                            color = JjikmukTheme.colors.brand, style = JjikmukTheme.typography.bodyM)
                    }
                }
                uiState.error?.let { error ->
                    item(key = "chat-error") { ChatErrorBubble(error, onRetry) }
                }
                if (uiState.isLoading && uiState.error?.isRetrying != true) {
                    item { ChatTypingBubble() }
                }
            }

            ProductCandidatePicker(
                candidates = uiState.productCandidates,
                onCandidateClick = onProductCandidateClick,
                onConfirmClick = onDismissProductCandidates,
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
            if (showTargets) ScanTargetPopup(members = members,
                statusMessage = targetState.message, enabled = targetState.ready, onRefresh = onRefreshTargets,
                onMemberCheckedChange = onTargetCheckedChange,
                onDismissRequest = { showTargets = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 20.dp))
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
