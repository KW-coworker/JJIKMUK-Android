package com.coworker.jjikmuk.feature.auth.google

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleLoginRouteTest {
    @get:Rule val compose = createComposeRule()
    private val sessions = AuthSessionManager(object : SessionStore {
        override suspend fun read(): SessionCredentials? = null
        override suspend fun write(credentials: SessionCredentials) = Unit
        override suspend fun clear() = Unit
    })
    private var googleCalls = 0
    private var linkCalls = 0
    private var cancelCalls = 0
    private var pickerCalls = 0
    private var wrongPassword = false
    private var credential: GoogleCredentialResult = GoogleCredentialResult.Token("test-google-token")
    private val destination = mutableStateOf("")
    private val attached = mutableStateOf(true)
    private val repository = object : AuthRepository by HttpAuthRepository.unconfigured() {
        override suspend fun google(idToken: String): AuthResult<GoogleAuthResult> {
            googleCalls++
            return AuthResult.Success(GoogleAuthResult.LinkRequired("test-link-proof", System.currentTimeMillis() + 300_000, 300))
        }
        override suspend fun linkGoogle(linkToken: String, currentPassword: String): AuthResult<AuthLogin> {
            linkCalls++; assertEquals("old", currentPassword)
            return if (wrongPassword) AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 400,
                "GOOGLE_LINK_PASSWORD_INVALID", "currentPassword", remainingAttempts = 4))
            else AuthResult.Success(AuthLogin("test-service-token", 1, "test@example.com", "unused", 900, true))
        }
        override suspend fun cancelGoogleLink(linkToken: String): AuthResult<Unit> {
            assertEquals("test-link-proof", linkToken); cancelCalls++; return AuthResult.Success(Unit)
        }
    }
    private val vm = GoogleLoginViewModel(repository, sessions)
    private fun show() {
        compose.setContent {
            JjikmukTheme {
                if (destination.value.isNotEmpty()) Text(destination.value)
                else if (attached.value) GoogleLoginRoute(vm,
                    onSignedIn = { destination.value = "세션 확인으로 이동" },
                    onBack = { destination.value = "이메일 로그인으로 이동" },
                    onPasswordReset = { destination.value = "비밀번호 재설정으로 이동" },
                    source = GoogleCredentialSource { pickerCalls++; credential })
            }
        }
    }

    @Test fun linkRequiresPasswordAndCompletionSurvivesScreenReentry() {
        show()
        compose.onNodeWithText("기존 계정 연결").assertIsDisplayed()
        compose.runOnIdle { assertNull(sessions.state.value.credentials); assertEquals(0, linkCalls) }
        compose.onNode(hasSetTextAction()).performTextInput("old")
        compose.onNodeWithText("기존 계정에 연결하고 로그인").performScrollTo().performClick()
        compose.onNodeWithText("계속하기").assertIsDisplayed()
        // Recreating the composition must not launch another picker or erase the completion notice.
        compose.runOnIdle { attached.value = false }
        compose.waitForIdle()
        compose.runOnIdle { attached.value = true }
        compose.onNodeWithText("계속하기").assertIsDisplayed().performClick()
        compose.onNodeWithText("세션 확인으로 이동").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, pickerCalls); assertEquals(1, linkCalls); assertNotNull(sessions.state.value.credentials) }
    }

    @Test fun wrongPasswordPreservesInputAndForgotPasswordCancelsProof() {
        wrongPassword = true; show()
        compose.onNode(hasSetTextAction()).performTextInput("old")
        compose.onNodeWithText("기존 계정에 연결하고 로그인").performScrollTo().performClick()
        compose.onNodeWithText("기존 계정의 비밀번호가 올바르지 않습니다.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals("old", vm.state.value.password); assertEquals(4, vm.state.value.remainingAttempts) }
        compose.onNodeWithText("비밀번호 찾기").performScrollTo().performClick()
        compose.onNodeWithText("비밀번호 재설정으로 이동").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, cancelCalls); assertNull(sessions.state.value.credentials); assertEquals("", vm.state.value.password) }
    }

    @Test fun backCancelsLinkAndAnotherEntryCanStartFresh() {
        show()
        compose.onNodeWithText("연결하지 않고 돌아가기").performScrollTo().performClick()
        compose.onNodeWithText("이메일 로그인으로 이동").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, cancelCalls); vm.prepareEntry(); destination.value = "" }
        compose.onNodeWithText("기존 계정 연결").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, pickerCalls); assertEquals(2, googleCalls); assertEquals(0, linkCalls) }
    }

    @Test fun missingSettingsShowsRetryWithoutMakingBackendRequest() {
        credential = GoogleCredentialResult.Failure("Google 로그인 설정이 필요합니다."); show()
        compose.onNodeWithText("Google 로그인 설정이 필요합니다.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, googleCalls); credential = GoogleCredentialResult.Token("test-google-token") }
        compose.onNodeWithText("Google 로그인 다시 시도").performClick()
        compose.onNodeWithText("기존 계정 연결").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, pickerCalls); assertEquals(1, googleCalls) }
    }
}
