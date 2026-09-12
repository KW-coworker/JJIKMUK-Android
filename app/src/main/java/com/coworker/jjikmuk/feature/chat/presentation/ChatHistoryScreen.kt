package com.coworker.jjikmuk.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukBottomNavigationBar
import com.coworker.jjikmuk.ui.component.JjikmukDraggableScannerFab
import com.coworker.jjikmuk.ui.component.JjikmukSearchField
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.component.MainTab
import com.coworker.jjikmuk.ui.component.ScanTargetMemberUiModel
import com.coworker.jjikmuk.ui.component.ScanTargetPopup
import com.coworker.jjikmuk.ui.component.defaultScanTargetMembers
import com.coworker.jjikmuk.ui.component.toScanTargetProfiles
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun ChatHistoryScreen(
    selectedTab: MainTab = MainTab.History,
    onTabClick: (MainTab) -> Unit = {},
    onBackClick: () -> Unit = {},
    onChatClick: (ChatHistoryUiModel) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onScannerClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var showScanTargetPopup by rememberSaveable { mutableStateOf(false) }
    val selectedChatIds = remember { mutableStateListOf<String>() }
    val scanTargetMembers = remember {
        mutableStateListOf<ScanTargetMemberUiModel>().apply {
            addAll(defaultScanTargetMembers())
        }
    }
    val selectedProfiles = scanTargetMembers.toScanTargetProfiles(
        defaultImageResId = R.drawable.ic_launcher_foreground,
    )
    val chatHistories = remember {
        mutableStateListOf<ChatHistoryUiModel>().apply {
            addAll(defaultChatHistories())
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isEditMode) {
                JjikmukTopAppBar(
                    leading = JjikmukTopAppBarLeading.Back(
                        onClick = {
                            isEditMode = false
                            selectedChatIds.clear()
                        },
                    ),
                )
            } else {
                JjikmukTopAppBar(
                    selectedProfiles = selectedProfiles,
                    onScanTargetClick = { showScanTargetPopup = true },
                    leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                )
            }
        },
        bottomBar = {
            if (!isEditMode) {
                JjikmukBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabClick = onTabClick,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JjikmukTheme.colors.background)
                .padding(innerPadding),
        ) {
            if (isEditMode) {
                ChatHistoryEditContent(
                    histories = chatHistories,
                    selectedChatIds = selectedChatIds,
                    onSelectAllClick = {
                        selectedChatIds.clear()
                        selectedChatIds.addAll(chatHistories.map { history -> history.id })
                    },
                    onChatCheckedChange = { historyId, checked ->
                        if (checked) {
                            if (historyId !in selectedChatIds) selectedChatIds.add(historyId)
                        } else {
                            selectedChatIds.remove(historyId)
                        }
                    },
                    onDeleteSelectedClick = {
                        chatHistories.removeAll { history -> history.id in selectedChatIds }
                        selectedChatIds.clear()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ChatHistoryContent(
                    histories = chatHistories,
                    onEditClick = { isEditMode = true },
                    onNewChatClick = onNewChatClick,
                    onChatClick = onChatClick,
                    onDeleteChatClick = { history ->
                        chatHistories.removeAll { chatHistory -> chatHistory.id == history.id }
                        selectedChatIds.remove(history.id)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                JjikmukDraggableScannerFab(
                    onClick = onScannerClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 21.dp, bottom = 16.dp),
                )
            }

            if (showScanTargetPopup) {
                ScanTargetPopup(
                    members = scanTargetMembers,
                    onMemberCheckedChange = { memberId, checked ->
                        val memberIndex = scanTargetMembers.indexOfFirst { member -> member.id == memberId }
                        if (memberIndex >= 0) {
                            scanTargetMembers[memberIndex] = scanTargetMembers[memberIndex].copy(
                                isSelected = checked,
                            )
                        }
                    },
                    onDismissRequest = { showScanTargetPopup = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 15.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryContent(
    histories: List<ChatHistoryUiModel>,
    onEditClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onChatClick: (ChatHistoryUiModel) -> Unit,
    onDeleteChatClick: (ChatHistoryUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ChatHistoryHeader(
            onEditClick = onEditClick,
            onNewChatClick = onNewChatClick,
        )
        ChatHistoryList(
            histories = histories,
            onChatClick = onChatClick,
            onDeleteChatClick = onDeleteChatClick,
            contentPadding = PaddingValues(bottom = 120.dp),
        )
    }
}

@Composable
private fun ChatHistoryHeader(
    onEditClick: () -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, bottom = 22.dp),
    ) {
        Text(
            text = "Chat",
            color = JjikmukTheme.colors.brand,
            style = JjikmukTheme.typography.section,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(modifier = Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatHistorySearchBar(
                modifier = Modifier.weight(1f),
            )
            ChatHistoryNewChatButton(
                onClick = onNewChatClick,
            )
            ChatHistoryActionButton(
                text = "편집",
                textColor = JjikmukTheme.colors.edit,
                backgroundColor = JjikmukTheme.colors.info,
                borderColor = JjikmukTheme.colors.edit,
                onClick = onEditClick,
            )
        }
    }
}

@Composable
private fun ChatHistoryNewChatButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_new),
            contentDescription = "새 대화",
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun ChatHistorySearchBar(
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    JjikmukSearchField(
        value = query,
        onValueChange = { query = it },
        placeholder = "채팅 내역 검색",
        onClearClick = { query = "" },
        modifier = modifier,
    )
}

@Composable
private fun ChatHistoryActionButton(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = JjikmukTheme.typography.labelS,
        )
    }
}

@Composable
private fun ChatHistoryList(
    histories: List<ChatHistoryUiModel>,
    onChatClick: (ChatHistoryUiModel) -> Unit,
    onDeleteChatClick: (ChatHistoryUiModel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(histories, key = { history -> history.id }) { history ->
            SwipeableChatHistoryRow(
                history = history,
                onClick = { onChatClick(history) },
                onDeleteClick = { onDeleteChatClick(history) },
            )
        }
    }
}

@Composable
private fun SwipeableChatHistoryRow(
    history: ChatHistoryUiModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { ChatHistorySwipeActionWidth.toPx() }
    var rowOffsetPx by remember { mutableStateOf(0f) }
    var dragStartOffsetPx by remember { mutableStateOf(0f) }

    LaunchedEffect(revealWidthPx) {
        rowOffsetPx = rowOffsetPx.coerceIn(-revealWidthPx, revealWidthPx)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChatHistoryRowHeight),
    ) {
        ChatHistorySwipeActionBackground(
            offsetPx = rowOffsetPx,
            revealWidthPx = revealWidthPx,
            onDeleteClick = {
                rowOffsetPx = 0f
                onDeleteClick()
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(rowOffsetPx.toInt(), 0) }
                .pointerInput(revealWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragStartOffsetPx = rowOffsetPx
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val nextOffsetPx = rowOffsetPx + dragAmount
                            rowOffsetPx = when {
                                dragStartOffsetPx > 0f -> nextOffsetPx.coerceIn(0f, revealWidthPx)
                                dragStartOffsetPx < 0f -> nextOffsetPx.coerceIn(-revealWidthPx, 0f)
                                else -> nextOffsetPx.coerceIn(-revealWidthPx, revealWidthPx)
                            }
                        },
                        onDragEnd = {
                            rowOffsetPx = when {
                                dragStartOffsetPx > 0f && rowOffsetPx >= revealWidthPx / 2f -> revealWidthPx
                                dragStartOffsetPx > 0f -> 0f
                                dragStartOffsetPx < 0f && rowOffsetPx <= -revealWidthPx / 2f -> -revealWidthPx
                                dragStartOffsetPx < 0f -> 0f
                                rowOffsetPx >= revealWidthPx / 2f -> revealWidthPx
                                rowOffsetPx <= -revealWidthPx / 2f -> -revealWidthPx
                                else -> 0f
                            }
                            dragStartOffsetPx = rowOffsetPx
                        },
                        onDragCancel = {
                            rowOffsetPx = dragStartOffsetPx
                        },
                    )
                },
        ) {
            ChatHistoryRow(
                history = history,
                onClick = {
                    if (rowOffsetPx == 0f) {
                        onClick()
                    } else {
                        rowOffsetPx = 0f
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatHistorySwipeActionBackground(
    offsetPx: Float,
    revealWidthPx: Float,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPinAction = offsetPx > 0f
    val isDeleteAction = offsetPx < 0f
    val backgroundColor = when {
        isPinAction -> Color(0xFFFFD66D)
        isDeleteAction -> Color(0xFFFF6B6B)
        else -> JjikmukTheme.colors.surface
    }
    val iconResId = when {
        isPinAction -> R.drawable.ic_chat_pin
        isDeleteAction -> R.drawable.ic_chat_delete
        else -> null
    }
    val iconAlignment = when {
        isPinAction -> Alignment.CenterStart
        isDeleteAction -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val iconPadding = if (isPinAction) {
        Modifier.padding(start = (ChatHistorySwipeActionWidth - ChatHistorySwipeIconSize) / 2)
    } else {
        Modifier.padding(end = (ChatHistorySwipeActionWidth - ChatHistorySwipeIconSize) / 2)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChatHistoryRowHeight)
            .background(backgroundColor),
    ) {
        if (iconResId != null) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(iconAlignment)
                    .then(iconPadding)
                    .size(ChatHistorySwipeIconSize)
                    .then(
                        if (isDeleteAction) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDeleteClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ChatHistoryRow(
    history: ChatHistoryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(JjikmukTheme.colors.surface)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 30.dp, top = 16.dp, end = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = history.title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.h3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(190.dp),
            )
            Row(
                modifier = Modifier.width(260.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = history.preview,
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.bodyS,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "– ${history.time}",
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.bodyS,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(JjikmukTheme.colors.borderSubtle),
        )
    }
}

@Composable
private fun ChatHistoryEditContent(
    histories: List<ChatHistoryUiModel>,
    selectedChatIds: List<String>,
    onSelectAllClick: () -> Unit,
    onChatCheckedChange: (historyId: String, checked: Boolean) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 43.dp, end = 16.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "전체 선택",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelM,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelectAllClick,
                    ),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatHistoryActionButton(
                    text = "삭제",
                    textColor = Color(0xFFE7000B),
                    backgroundColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFFE2E2),
                    onClick = onDeleteSelectedClick,
                )
                ChatHistoryActionButton(
                    text = "편집",
                    textColor = JjikmukTheme.colors.edit,
                    backgroundColor = JjikmukTheme.colors.info,
                    borderColor = JjikmukTheme.colors.edit,
                    onClick = {},
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            items(histories, key = { history -> history.id }) { history ->
                ChatHistorySelectableRow(
                    history = history,
                    checked = history.id in selectedChatIds,
                    onCheckedChange = { checked ->
                        onChatCheckedChange(history.id, checked)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatHistorySelectableRow(
    history: ChatHistoryUiModel,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(JjikmukTheme.colors.surface)
            .clickable { onCheckedChange(!checked) },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 30.dp, top = 16.dp)
                .width(190.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = history.title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.h3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = history.preview,
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.bodyS,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "– ${history.time}",
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.bodyS,
                    maxLines = 1,
                )
            }
        }
        ChatHistorySelectionCircle(
            checked = checked,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 21.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(JjikmukTheme.colors.borderSubtle),
        )
    }
}

@Composable
private fun ChatHistorySelectionCircle(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    if (checked) {
        Icon(
            painter = painterResource(R.drawable.ic_condition_check),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = modifier.size(26.dp),
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(JjikmukTheme.colors.disabled),
        )
    }
}

data class ChatHistoryUiModel(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
)

private val ChatHistoryRowHeight = 80.dp
private val ChatHistorySwipeActionWidth = 80.dp
private val ChatHistorySwipeIconSize = 30.dp

private fun defaultChatHistories(): List<ChatHistoryUiModel> =
    List(5) { index ->
        ChatHistoryUiModel(
            id = "chat-$index",
            title = "포키 블루베리의 맛...",
            preview = if (index == 0) "Secondary line of text" else "포키 맛 종류에는 뭐뭐....",
            time = "12:00 am",
        )
    }

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ChatHistoryScreenPreview() {
    JjikmukTheme {
        ChatHistoryScreen()
    }
}
