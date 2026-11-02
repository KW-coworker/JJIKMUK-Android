package com.coworker.jjikmuk.feature.chat

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.feature.chat.presentation.ChatScreen
import com.coworker.jjikmuk.feature.chat.presentation.ChatUiState
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductCandidatePickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun candidate(name: String) = ChatProductCandidate(
        productName = name, brandName = "해태", barcode = null,
        rawMaterials = null, allergyLabels = emptyList(), reason = null,
    )

    private fun showChat(onSelect: (ChatProductCandidate) -> Unit = {}): androidx.compose.runtime.MutableState<ChatUiState> {
        val state = mutableStateOf(ChatUiState(productCandidates = listOf(candidate("해태)홈런볼"), candidate("초코홈런볼골드"))))
        compose.setContent {
            JjikmukTheme {
                ChatScreen(
                    title = "홈런볼 먹어도 돼?", uiState = state.value, onBackClick = {}, onSendMessage = {},
                    onProductCandidateClick = onSelect,
                    onDismissProductCandidates = { state.value = state.value.copy(productCandidates = emptyList()) },
                )
            }
        }
        return state
    }

    @Test fun confirmButtonIsLabeledConfirmAndClosesSheet() {
        showChat()
        compose.onNodeWithText("로그인").assertDoesNotExist()
        compose.onNodeWithText("메세지를 입력하세요").assertDoesNotExist()

        compose.onNodeWithText("확인").assertIsDisplayed().performClick()

        compose.onNodeWithText("확인").assertDoesNotExist()
        compose.onNodeWithText("해태)홈런볼").assertDoesNotExist()
        compose.onNodeWithText("메세지를 입력하세요").assertIsDisplayed()
    }

    @Test fun tappingCandidateStillSelectsIt() {
        var selected: ChatProductCandidate? = null
        showChat(onSelect = { selected = it })
        compose.onNodeWithText("초코홈런볼골드").performClick()
        compose.runOnIdle { assertEquals("초코홈런볼골드", selected?.productName) }
    }
}
