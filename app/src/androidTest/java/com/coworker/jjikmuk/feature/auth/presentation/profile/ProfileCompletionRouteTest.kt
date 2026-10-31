package com.coworker.jjikmuk.feature.auth.presentation.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileCompletionRouteTest {
    @get:Rule val compose = createComposeRule()
    private val sessions = AuthSessionManager(object : SessionStore {
        override suspend fun read(): SessionCredentials? = null
        override suspend fun write(credentials: SessionCredentials) = Unit
        override suspend fun clear() = Unit
    })
    private fun profile(complete: Boolean = false) = AuthProfile(1, "test@example.com", "테스트사용자", null,
        null, null, null, "USER", AuthProvider.GOOGLE, complete, false, null)
    private var result: AuthResult<AuthProfile> = AuthResult.Success(profile(true))
    private var server = profile()
    private var calls = 0
    private var reads = 0
    private val finished = mutableStateOf(false)
    private val signedOut = mutableStateOf(false)
    private val repository = object : AuthRepository by HttpAuthRepository.unconfigured() {
        override suspend fun completeProfile(accessToken: String, input: ProfileSubmission): AuthResult<AuthProfile> {
            calls++; return result
        }
        override suspend fun me(accessToken: String): AuthResult<AuthProfile> { reads++; return AuthResult.Success(server) }
    }
    private val vm = ProfileCompletionViewModel(repository,
        AuthProfiles(repository, sessions, AuthenticatedRequests(sessions, repository)), sessions)

    @Before fun setup() {
        runBlocking {
            sessions.acceptLogin(sessions.beginLogin(), AuthLogin("test-only", 1, "test@example.com", "unused", 3600, false), System.currentTimeMillis())
            sessions.acceptProfile(sessions.state.value.generation, profile())
        }
        compose.setContent {
            JjikmukTheme {
                when {
                    finished.value -> Text("프로필 완료 후 진입")
                    signedOut.value -> Text("로그아웃됨")
                    else -> ProfileCompletionRoute(vm, { finished.value = true }, { signedOut.value = true }, {
                        runBlocking { sessions.clear() }
                    })
                }
            }
        }
    }
    private fun reviewWithoutConditions() {
        compose.onNodeWithText("닉네임 생성").assertIsDisplayed()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("맞춤 조건 선택").assertIsDisplayed()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("프로필 완료").performScrollTo()
    }

    @Test fun emptyConditionsReachCompletionOnlyAfterServerSuccess() {
        reviewWithoutConditions()
        compose.runOnIdle { assertFalse(sessions.state.value.canEnterHome); assertEquals(0, calls) }
        compose.onNodeWithText("프로필 완료").performClick()
        compose.onNodeWithText("프로필 완료 후 진입").assertIsDisplayed()
        compose.runOnIdle { assertTrue(sessions.state.value.canEnterHome); assertEquals(1, calls) }
    }

    @Test fun duplicateNicknameStaysOnReviewWithInputIntact() {
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "NICKNAME_ALREADY_EXISTS", "nickname"))
        reviewWithoutConditions(); compose.onNodeWithText("프로필 완료").performClick()
        compose.onNodeWithText("이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(finished.value); assertEquals("테스트사용자", vm.state.value.form.nickname)
            assertNotNull(sessions.state.value.credentials)
        }
    }

    @Test fun lostCompletionUsesReadbackWithoutSecondMutation() {
        result = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        reviewWithoutConditions(); compose.onNodeWithText("프로필 완료").performClick()
        compose.onNodeWithText("완료 상태 다시 확인").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, calls); assertFalse(finished.value); server = profile(true) }
        compose.onNodeWithText("완료 상태 다시 확인").performClick()
        compose.onNodeWithText("프로필 완료 후 진입").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(1, reads) }
    }

    @Test fun backRequiresExplicitLogoutAndNeverExposesHome() {
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.onNodeWithText("계속 입력하기").performClick()
        compose.runOnIdle { assertNotNull(sessions.state.value.credentials); assertFalse(finished.value) }
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.onNodeWithText("로그아웃").performClick()
        compose.onNodeWithText("로그아웃됨").assertIsDisplayed()
        compose.runOnIdle { assertNull(sessions.state.value.credentials); assertEquals(0, calls) }
    }
}
