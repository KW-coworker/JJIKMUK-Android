package com.coworker.jjikmuk.feature.auth.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
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

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        composeRule.setContent {
            JjikmukTheme {
                AuthNavHost(navController = navController)
            }
        }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            navController.currentDestination?.route == AuthRoute.AuthChoice
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
        composeRule.runOnUiThread {
            navController.navigate(AuthRoute.Login)
            navController.navigate(AuthRoute.Home) {
                popUpTo(AuthRoute.AuthChoice) { inclusive = true }
            }

            assertEquals(AuthRoute.Home, navController.currentDestination?.route)
            assertFalse(navController.popBackStack())
        }
    }
}
