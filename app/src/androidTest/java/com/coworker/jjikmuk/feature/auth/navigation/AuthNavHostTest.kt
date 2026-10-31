package com.coworker.jjikmuk.feature.auth.navigation

import androidx.lifecycle.SavedStateHandle
import com.coworker.jjikmuk.feature.mypage.presentation.AccountActionViewModel
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.feature.auth.domain.*
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import kotlinx.coroutines.runBlocking
import com.coworker.jjikmuk.feature.auth.presentation.login.LoginViewModel
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpViewModel
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sessions = AuthSessionManager(object : SessionStore {
        override suspend fun read(): SessionCredentials? = null
        override suspend fun write(credentials: SessionCredentials) = Unit
        override suspend fun clear() = Unit
    })
    private var meResult: AuthResult<AuthProfile> = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
    private var signupResult: AuthResult<Long> = AuthResult.Success(42L)
    private var signupCalls = 0
    private var accountCalls = 0
    private var accountResult: AuthResult<Unit> = AuthResult.Success(Unit)
    private val repository = object : AuthRepository by HttpAuthRepository.unconfigured() {
        override suspend fun me(accessToken: String): AuthResult<AuthProfile> = meResult
        override suspend fun deleteAccount(accessToken: String, userId: Long): AuthResult<Unit> {
            accountCalls++; return accountResult
        }
        override suspend fun changePassword(accessToken: String, userId: Long, currentPassword: String, newPassword: String): AuthResult<Unit> {
            accountCalls++; return accountResult
        }
        override suspend fun sendEmail(email: String, purpose: EmailPurpose) = AuthResult.Success(EmailDispatch(email, purpose, 300))
        override suspend fun verifyEmail(email: String, purpose: EmailPurpose, code: String) =
            AuthResult.Success(EmailProof("test-proof", email, purpose, 1800))
        override suspend fun signUp(input: SignUpSubmission): AuthResult<Long> {
            signupCalls++; return signupResult
        }
    }
    private val login = LoginViewModel(repository, sessions)
    private val signup = SignUpViewModel(repository)
    private val passwordReset = PasswordResetViewModel(HttpAuthRepository.unconfigured(), sessions)
    private val account = AccountActionViewModel(AuthAccountActions(repository, AuthenticatedRequests(sessions, repository), sessions), sessions, SavedStateHandle())
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        composeRule.setContent {
            JjikmukTheme {
                AuthNavHost(navController = navController, signUpViewModel = signup, passwordResetViewModel = passwordReset, loginViewModel = login,
                    accountActionViewModel = account)
            }
        }
        // These tests isolate auth navigation from permission onboarding.
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.AuthChoice) {
                popUpTo(AuthRoute.Splash) { inclusive = true }
            }
        }
    }

    private fun openVerifiedSignUpProfile() {
        composeRule.runOnUiThread {
            runBlocking {
                signup.updateEmail("test@example.com"); signup.validateEmail()
                signup.updateOtp("0427"); signup.verifyOtp()
            }
            signup.updatePassword("Password123!"); signup.updatePasswordConfirm("Password123!")
            signup.updateNickname("테스트사용자")
            navController.navigate(AuthRoute.SignUpEmail)
            navController.navigate(AuthRoute.SignUpProfile)
        }
    }

    @Test
    fun successfulRegistrationAloneOpensCompletionThenLogin() {
        openVerifiedSignUpProfile()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.SignUpComplete }
        composeRule.runOnIdle {
            assertEquals(1, signupCalls)
            assertEquals("", signup.uiState.value.password)
            assertTrue(sessions.state.value.credentials == null)
        }
        composeRule.onNodeWithText("로그인하기").performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.Login }
        composeRule.runOnUiThread {
            assertTrue(navController.popBackStack())
            assertEquals(AuthRoute.AuthChoice, navController.currentDestination?.route)
        }
    }

    @Test
    fun unknownRegistrationRetainsDraftAndOffersReverificationInsteadOfReplay() {
        signupResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        openVerifiedSignUpProfile()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("이메일 다시 인증").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.SignUpEmail }
        composeRule.runOnIdle {
            assertEquals(1, signupCalls)
            assertEquals("테스트사용자", signup.uiState.value.nickname)
            assertEquals("Password123!", signup.uiState.value.password)
            assertFalse(signup.uiState.value.registrationCompleted)
        }
    }

    @Test
    fun splashIsRemovedFromBackStack() {
        assertEquals(AuthRoute.AuthChoice, navController.currentDestination?.route)

        composeRule.runOnUiThread {
            assertFalse(navController.popBackStack())
        }
    }

    @Test
    fun verifiedSignUpOtpCannotBeReentered() {
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.SignUpEmail)
            navController.navigate(AuthRoute.SignUpOtp)
            navController.navigate(AuthRoute.SignUpPassword) {
                popUpTo(AuthRoute.SignUpOtp) { inclusive = true }
            }

            assertEquals(AuthRoute.SignUpPassword, navController.currentDestination?.route)
            assertTrue(navController.popBackStack())
            assertEquals(AuthRoute.SignUpEmail, navController.currentDestination?.route)
        }
    }

    @Test
    fun signUpCompleteRemovesInputScreens() {
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.SignUpEmail)
            navController.navigate(AuthRoute.SignUpProfile)
            navController.navigate(AuthRoute.SignUpComplete) {
                popUpTo(AuthRoute.SignUpEmail) { inclusive = true }
            }

            assertEquals(AuthRoute.SignUpComplete, navController.currentDestination?.route)
            assertTrue(navController.popBackStack())
            assertEquals(AuthRoute.AuthChoice, navController.currentDestination?.route)
        }
    }

    @Test
    fun unverifiedLoginShowsRetryAndPreservesSession() {
        composeRule.runOnUiThread {
            runBlocking {
                sessions.acceptLogin(sessions.beginLogin(),
                    AuthLogin("test-only", 1, "test@example.com", "unused", 3600, true), System.currentTimeMillis())
            }
            navController.navigate(AuthRoute.Home)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            navController.currentDestination?.route == AuthRoute.SessionEntry &&
                login.entryState.value is com.coworker.jjikmuk.feature.auth.presentation.login.SessionEntryState.Retry
        }
        composeRule.onNodeWithText("다시 확인").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(sessions.state.value.credentials != null) }
        composeRule.onNodeWithText("로그아웃").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navController.currentDestination?.route == AuthRoute.AuthChoice }
        composeRule.runOnUiThread { assertFalse(navController.popBackStack()) }
    }

    @Test
    fun incompleteProfileHasSeparateGateAndCannotEnterHome() {
        meResult = AuthResult.Success(AuthProfile(1, "test@example.com", "임시닉네임", null, null, null, null,
            "USER", AuthProvider.LOCAL, false, true, null))
        composeRule.runOnUiThread {
            runBlocking {
                sessions.acceptLogin(sessions.beginLogin(),
                    AuthLogin("test-only", 1, "test@example.com", "unused", 3600, true), System.currentTimeMillis())
            }
            navController.navigate(AuthRoute.SessionEntry) {
                popUpTo(AuthRoute.AuthChoice) { inclusive = true }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            login.entryState.value == com.coworker.jjikmuk.feature.auth.presentation.login.SessionEntryState.ProfileRequired
        }
        composeRule.onNodeWithText("프로필 추가 입력이 필요해요").assertIsDisplayed()
        composeRule.runOnIdle { assertFalse(sessions.state.value.canEnterHome) }
        composeRule.onNodeWithText("로그아웃").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navController.currentDestination?.route == AuthRoute.AuthChoice }
    }

    @Test
    fun startupWithoutSessionReturnsToAuthenticationChoice() {
        composeRule.runOnUiThread { navController.navigate(AuthRoute.SessionEntry) }
        composeRule.waitUntil(timeoutMillis = 5_000) { navController.currentDestination?.route == AuthRoute.AuthChoice }
        composeRule.runOnUiThread { assertFalse(navController.popBackStack()) }
    }

    @Test
    fun directHomeNavigationWithoutSessionReturnsToAuth() {
        composeRule.runOnUiThread { navController.navigate(AuthRoute.Home) }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            navController.currentDestination?.route == AuthRoute.AuthChoice
        }
    }

    @Test
    fun logoutRemovesHomeEvenIfServerUnavailable() {
        composeRule.runOnUiThread {
            runBlocking {
                sessions.acceptLogin(sessions.beginLogin(),
                    AuthLogin("test-only", 1, "test@example.com", "unused", 3600, true), System.currentTimeMillis())
            }
            navController.navigate(AuthRoute.Home) {
                popUpTo(AuthRoute.AuthChoice) { inclusive = true }
            }
            login.logout() // Repository is deliberately unconfigured; local navigation must still finish.
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            navController.currentDestination?.route == AuthRoute.AuthChoice
        }
        composeRule.runOnUiThread {
            assertFalse(sessions.state.value.canEnterHome)
            assertFalse(navController.popBackStack())
        }
    }

    private fun openAccountAction(route: String, hasPassword: Boolean = true) {
        composeRule.runOnUiThread {
            runBlocking {
                sessions.acceptLogin(sessions.beginLogin(),
                    AuthLogin("test-only", 1, "test@example.com", "unused", 3600, true), System.currentTimeMillis())
                sessions.acceptProfile(sessions.state.value.generation,
                    AuthProfile(1, "test@example.com", "테스트사용자", null, null, null, null,
                        "USER", if (hasPassword) AuthProvider.LOCAL else AuthProvider.GOOGLE, true, hasPassword, null))
            }
            navController.navigate(route)
        }
    }

    @Test
    fun withdrawalWaitsForConsentAndCompletionSurvivesSessionRemoval() {
        openAccountAction(AuthRoute.AccountWithdrawal)
        composeRule.onNodeWithText("탈퇴하기").assertIsNotEnabled()
        composeRule.onNodeWithText("계속 이용하기").assertIsEnabled()
        composeRule.onNodeWithText("위 내용을 모두 확인했으며, 이에 동의합니다.").performScrollTo().performClick()
        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.onNodeWithText("회원탈퇴 완료").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, accountCalls); assertTrue(sessions.state.value.credentials == null)
            assertEquals(AuthRoute.AccountWithdrawal, navController.currentDestination?.route)
        }
        composeRule.onNodeWithText("처음으로").performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.AuthChoice }
        composeRule.runOnUiThread { assertFalse(navController.popBackStack()) }
    }

    @Test
    fun passwordSuccessShowsLoginNoticeAndRemovesAccountBackStack() {
        openAccountAction(AuthRoute.AccountPassword)
        composeRule.onNodeWithText("현재 비밀번호").performTextInput("old")
        composeRule.onNodeWithText("새 비밀번호").performTextInput("Password123")
        composeRule.onNodeWithText("새 비밀번호 확인").performTextInput("Password123")
        composeRule.onNodeWithText("비밀번호 변경하기").performScrollTo().performClick()
        composeRule.onNodeWithText("비밀번호가 변경되었어요").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, accountCalls); assertTrue(sessions.state.value.credentials == null) }
        composeRule.onNodeWithText("로그인하기").performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.Login }
        composeRule.runOnUiThread { assertFalse(navController.popBackStack()) }
    }

    @Test
    fun unknownWithdrawalDisablesRepeatAndRequiresExplicitSignOut() {
        accountResult = AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
        openAccountAction(AuthRoute.AccountWithdrawal)
        composeRule.onNodeWithText("위 내용을 모두 확인했으며, 이에 동의합니다.").performScrollTo().performClick()
        composeRule.onNodeWithText("탈퇴하기").performClick()
        composeRule.onNodeWithText("탈퇴하기").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(1, accountCalls); assertTrue(sessions.state.value.credentials != null) }
        composeRule.onNodeWithText("다시 로그인하기").performClick()
        composeRule.waitUntil(5_000) { navController.currentDestination?.route == AuthRoute.AuthChoice }
        composeRule.runOnIdle { assertEquals(1, accountCalls); assertTrue(sessions.state.value.credentials == null) }
    }

    @Test
    fun googleOnlyAccountExplainsWhyPasswordChangeIsUnavailable() {
        openAccountAction(AuthRoute.AccountPassword, hasPassword = false)
        composeRule.onNodeWithText("Google로 로그인한 계정입니다. 비밀번호 변경을 사용할 수 없습니다.").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, accountCalls) }
    }

}
