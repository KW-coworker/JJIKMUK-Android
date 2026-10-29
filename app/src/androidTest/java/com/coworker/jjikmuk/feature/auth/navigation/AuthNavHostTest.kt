package com.coworker.jjikmuk.feature.auth.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.feature.auth.domain.AuthLogin
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
    private val login = LoginViewModel(HttpAuthRepository.unconfigured(), sessions)
    private val signup = SignUpViewModel(HttpAuthRepository.unconfigured())
    private val passwordReset = PasswordResetViewModel(HttpAuthRepository.unconfigured(), sessions)
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        composeRule.setContent {
            JjikmukTheme {
                AuthNavHost(navController = navController, signUpViewModel = signup, passwordResetViewModel = passwordReset, loginViewModel = login)
            }
        }
        // These tests isolate auth navigation from permission onboarding.
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.AuthChoice) {
                popUpTo(AuthRoute.Splash) { inclusive = true }
            }
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
    fun loginSuccessRemovesAuthenticationStack() {
        runBlocking {
            sessions.acceptLogin(sessions.beginLogin(),
                AuthLogin("test-only", 1, "test@example.com", "unused", 3600, true), System.currentTimeMillis())
        }
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.Login)
            navController.navigate(AuthRoute.Home) {
                popUpTo(AuthRoute.AuthChoice) { inclusive = true }
            }

            assertEquals(AuthRoute.Home, navController.currentDestination?.route)
            assertFalse(navController.popBackStack())
        }
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

}
