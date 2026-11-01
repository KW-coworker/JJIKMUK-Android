package com.coworker.jjikmuk.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpEmailScreen
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpUiState
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetEmailScreen
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetUiState
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EmailRetryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signupShowsCooldownAndBlocksKeyboardSubmission() {
        val state = mutableStateOf(SignUpUiState(email = "user@example.com", resendSeconds = 60))
        var calls = 0
        compose.setContent { JjikmukTheme {
            SignUpEmailScreen(state.value, {}, {}, { calls++ })
        } }
        compose.onNodeWithText("60초 후 다시 요청").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(0, calls); state.value = state.value.copy(resendSeconds = 0) }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun passwordResetShowsCooldownAndBlocksKeyboardSubmission() {
        val state = mutableStateOf(PasswordResetUiState(email = "user@example.com", resendSeconds = 42))
        var calls = 0
        compose.setContent { JjikmukTheme {
            PasswordResetEmailScreen(state.value, {}, {}, { calls++ })
        } }
        compose.onNodeWithText("42초 후 다시 요청").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(0, calls); state.value = state.value.copy(resendSeconds = 0) }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
