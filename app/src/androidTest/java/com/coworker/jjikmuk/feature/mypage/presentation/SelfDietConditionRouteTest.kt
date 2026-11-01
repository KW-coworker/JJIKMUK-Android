package com.coworker.jjikmuk.feature.mypage.presentation

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
class SelfDietConditionRouteTest {
    @get:Rule val compose = createComposeRule()
    private val sessions = AuthSessionManager(object : SessionStore {
        override suspend fun read(): SessionCredentials? = null
        override suspend fun write(credentials: SessionCredentials) = Unit
        override suspend fun clear() = Unit
    })
    private fun profile(allergies: String? = "egg", diet: String? = "highProtein") =
        AuthProfile(1, "test@example.com", "테스트사용자", allergies, "disease", diet, "dislike", "USER", AuthProvider.LOCAL, true, true, null)
    private var server = profile()
    private var fail = false
    private var calls = 0
    private var reads = 0
    private var submitted: ProfilePatch? = null
    private val left = mutableStateOf(false)
    private val attached = mutableStateOf(true)
    private fun ProfileField.apply(old: String?) = when (this) {
        ProfileField.Unchanged -> old
        ProfileField.Clear -> null
        is ProfileField.Value -> value
    }
    private val repository = object : AuthRepository by HttpAuthRepository.unconfigured() {
        override suspend fun me(accessToken: String): AuthResult<AuthProfile> { reads++; return AuthResult.Success(server) }
        override suspend fun patchProfile(accessToken: String, patch: ProfilePatch): AuthResult<AuthProfile> {
            calls++; submitted = patch
            server = profile(patch.allergies.apply(server.allergies), patch.specialDiet.apply(server.specialDiet))
            return if (fail) AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) else AuthResult.Success(server)
        }
    }
    private val vm = SelfDietConditionViewModel(AuthProfiles(repository, sessions, AuthenticatedRequests(sessions, repository)), sessions)

    @Before fun show() {
        runBlocking {
            sessions.acceptLogin(sessions.beginLogin(), AuthLogin("test-token", 1, "test@example.com", "unused", 900, true), System.currentTimeMillis())
            sessions.acceptProfile(sessions.state.value.generation, server)
        }
        compose.setContent {
            JjikmukTheme {
                if (left.value) Text("마이페이지로 돌아옴")
                else if (attached.value) SelfDietConditionRoute({ left.value = true }, vm)
            }
        }
    }
    private fun toggle(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()

    @Test fun editingWheatSavesOnlyAllergiesAndShowsCompletion() {
        compose.onNodeWithText("수정하기").assertIsNotEnabled()
        toggle("밀"); compose.onNodeWithText("수정하기").performClick()
        compose.onNodeWithText("식이조건을 저장했어요.").assertIsDisplayed()
        compose.onNodeWithText("수정하기").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals("egg,wheat", (submitted!!.allergies as ProfileField.Value).value)
            assertSame(ProfileField.Unchanged, submitted!!.specialDiet); assertEquals(1, calls)
        }
    }

    @Test fun clearingSelectionsExplicitlyRemovesServerConditions() {
        toggle("계란"); toggle("고단백"); compose.onNodeWithText("수정하기").performClick()
        compose.onNodeWithText("식이조건을 저장했어요.").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(ProfileField.Clear, submitted!!.allergies); assertSame(ProfileField.Clear, submitted!!.specialDiet)
            assertNull(sessions.state.value.profile!!.allergies)
        }
    }

    @Test fun lostResponseUsesReadbackAndDoesNotResendPatch() {
        compose.runOnIdle { fail = true }
        toggle("밀"); compose.onNodeWithText("수정하기").performClick()
        compose.onNodeWithText("서버 정보 다시 확인").assertIsDisplayed()
        compose.onNodeWithText("수정하기").assertIsNotEnabled()
        compose.onNodeWithText("서버 정보 다시 확인").performClick()
        compose.onNodeWithText("서버에 저장된 조건을 확인했어요.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(2, reads); assertFalse(vm.state.value.hasChanges) }
    }

    @Test fun draftSurvivesRecreationAndLeavingNeedsConfirmation() {
        toggle("밀")
        compose.runOnIdle { attached.value = false }; compose.waitForIdle()
        compose.runOnIdle { attached.value = true }
        compose.onNodeWithText("수정하기").assertIsEnabled()
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.onNodeWithText("계속 수정하기").performClick()
        compose.runOnIdle { assertEquals(setOf("egg", "wheat"), vm.state.value.draft.allergies); assertEquals(1, reads) }
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("마이페이지로 돌아옴").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, calls); assertNull(vm.state.value.saved) }
    }
}
