package com.coworker.jjikmuk.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.login.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class LoginRateLimitUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun cooldownDisablesButtonAndKeyboardSubmission() {
        var calls = 0
        val repo = object : AuthRepository by HttpAuthRepository.unconfigured() {
            override suspend fun login(email: String, password: String): AuthResult<AuthLogin> {
                calls++
                return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 429, "LOGIN_RATE_LIMITED", retryAfterSeconds = 173))
            }
        }
        val sessions = AuthSessionManager(object : SessionStore {
            override suspend fun read(): SessionCredentials? = null
            override suspend fun write(credentials: SessionCredentials) = Unit
            override suspend fun clear() = Unit
        })
        val vm = LoginViewModel(repo, sessions)
        rule.setContent { JjikmukTheme {
            LoginRoute(vm, {}, {}, {}, {}, {})
        } }
        rule.runOnIdle { vm.updateEmail("user@example.com"); vm.updatePassword("old") }
        rule.onNodeWithText("로그인").performClick()
        rule.waitUntil(5_000) { vm.uiState.value.retrySeconds > 0 }
        rule.onNodeWithText("로그인").assertIsNotEnabled()
        rule.onNodeWithText("초 후 다시 시도", substring = true).assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).onLast().performImeAction()
        rule.runOnIdle { assertEquals(1, calls) }
    }
}
