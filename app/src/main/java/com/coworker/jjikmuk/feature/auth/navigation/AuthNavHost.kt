package com.coworker.jjikmuk.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coworker.jjikmuk.JjikmukAppContent
import com.coworker.jjikmuk.feature.auth.presentation.choice.AuthChoiceScreen
import com.coworker.jjikmuk.feature.auth.presentation.login.LoginViewModel
import com.coworker.jjikmuk.feature.auth.presentation.login.LoginRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetViewModel
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetCompleteScreen
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetEmailRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetNewPasswordRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetOtpRoute
import com.coworker.jjikmuk.feature.auth.presentation.placeholder.AuthPlaceholderScreen
import com.coworker.jjikmuk.feature.auth.presentation.placeholder.PlaceholderAction
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpConditionsRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpAllergiesRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpEmailRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpNicknameRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpOtpRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpPasswordRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpUiState
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpVegetarianRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = viewModel(),
    passwordResetViewModel: PasswordResetViewModel = viewModel(),
    signUpViewModel: SignUpViewModel = viewModel(),
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Splash,
        modifier = modifier,
    ) {
        composable(AuthRoute.Splash) {
            LaunchedEffect(Unit) {
                delay(SPLASH_DURATION_MILLIS)
                navController.navigate(AuthRoute.AuthChoice) {
                    popUpTo(AuthRoute.Splash) { inclusive = true }
                }
            }
            AuthPlaceholderScreen(
                title = "스플래시",
                primaryActions = emptyList(),
                blockSystemBack = true,
            )
        }

        composable(AuthRoute.AuthChoice) {
            AuthChoiceScreen(
                onLoginClick = { navController.navigate(AuthRoute.Login) },
                onSignUpClick = {
                    signUpViewModel.reset()
                    navController.navigate(AuthRoute.SignUpEmail)
                },
            )
        }

        composable(AuthRoute.Login) {
            LoginRoute(
                viewModel = loginViewModel,
                onBackClick = navController::popBackStack,
                onLoginSuccess = {
                        loginViewModel.reset()
                        navigateToHome(navController)
                },
                onForgotPasswordClick = {
                    passwordResetViewModel.reset()
                    navController.navigate(AuthRoute.PasswordResetEmail)
                },
                onSignUpClick = {
                    signUpViewModel.reset()
                    navController.navigate(AuthRoute.SignUpEmail)
                },
                onGoogleLoginClick = {
                    loginViewModel.reset()
                    navigateToHome(navController)
                },
            )
        }

        passwordResetGraph(
            navController = navController,
            viewModel = passwordResetViewModel,
        )
        signUpGraph(
            navController = navController,
            viewModel = signUpViewModel,
        )

        composable(AuthRoute.Home) {
            JjikmukAppContent()
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.passwordResetGraph(
    navController: NavHostController,
    viewModel: PasswordResetViewModel,
) {
    composable(AuthRoute.PasswordResetEmail) {
        PasswordResetEmailRoute(
            viewModel = viewModel,
            onBackClick = navController::popBackStack,
            onCodeSent = { navController.navigate(AuthRoute.PasswordResetOtp) },
        )
    }
    composable(AuthRoute.PasswordResetOtp) {
        PasswordResetOtpRoute(
            viewModel = viewModel,
            onBackClick = {
                viewModel.restartFromEmail()
                navController.popBackStack()
            },
            onOtpVerified = {
                navController.navigate(AuthRoute.PasswordResetNewPassword) {
                    popUpTo(AuthRoute.PasswordResetOtp) { inclusive = true }
                }
            },
        )
    }
    composable(AuthRoute.PasswordResetNewPassword) {
        PasswordResetNewPasswordRoute(
            viewModel = viewModel,
            onBackClick = {
                viewModel.restartFromEmail()
                navController.popBackStack()
            },
            onPasswordChanged = {
                navController.navigate(AuthRoute.PasswordResetComplete) {
                    popUpTo(AuthRoute.PasswordResetEmail) { inclusive = true }
                }
            },
        )
    }
    composable(AuthRoute.PasswordResetComplete) {
        PasswordResetCompleteScreen(
            onLoginClick = {
                viewModel.reset()
                navController.navigate(AuthRoute.Login) {
                    popUpTo(AuthRoute.Login) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.signUpGraph(
    navController: NavHostController,
    viewModel: SignUpViewModel,
) {
    composable(AuthRoute.SignUpEmail) {
        SignUpEmailRoute(
            viewModel = viewModel,
            onCodeSent = { navController.navigate(AuthRoute.SignUpOtp) },
            onBackClick = {
                viewModel.reset()
                navController.popBackStack()
            },
        )
    }
    composable(AuthRoute.SignUpOtp) {
        SignUpOtpRoute(
            viewModel = viewModel,
            onBackClick = {
                viewModel.restartFromEmail()
                navController.popBackStack()
            },
            onOtpVerified = {
                navController.navigate(AuthRoute.SignUpPassword) {
                    popUpTo(AuthRoute.SignUpOtp) { inclusive = true }
                }
            },
        )
    }
    composable(AuthRoute.SignUpPassword) {
        SignUpPasswordRoute(
            viewModel = viewModel,
            onPasswordCreated = { navController.navigate(AuthRoute.SignUpNickname) },
            onBackClick = {
                viewModel.restartFromEmail()
                navController.popBackStack()
            },
        )
    }
    composable(AuthRoute.SignUpNickname) {
        SignUpNicknameRoute(
            viewModel = viewModel,
            onNicknameCreated = { navController.navigate(AuthRoute.SignUpConditions) },
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpConditions) {
        SignUpConditionsRoute(
            viewModel = viewModel,
            onNextClick = {
                navController.navigate(nextRouteAfterConditions(viewModel.uiState.value))
            },
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpVegetarian) {
        SignUpVegetarianRoute(
            viewModel = viewModel,
            onNextClick = { navController.navigate(AuthRoute.SignUpProfile) },
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpAllergies) {
        SignUpAllergiesRoute(
            viewModel = viewModel,
            onNextClick = {
                val latestState = viewModel.uiState.value
                navController.navigate(
                    if (latestState.hasVegetarianCondition) {
                        AuthRoute.SignUpVegetarian
                    } else {
                        AuthRoute.SignUpProfile
                    },
                )
            },
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpProfile) {
        AuthPlaceholderScreen(
            title = "회원가입 - 프로필 확인",
            primaryActions = listOf(
                PlaceholderAction("다음") {
                    navController.navigate(AuthRoute.SignUpComplete) {
                        popUpTo(AuthRoute.SignUpEmail) { inclusive = true }
                    }
                },
            ),
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpComplete) {
        AuthPlaceholderScreen(
            title = "회원가입 완료",
            primaryActions = listOf(
                PlaceholderAction("확인") {
                    viewModel.reset()
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.AuthChoice) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            ),
            blockSystemBack = true,
        )
    }
}

private fun nextRouteAfterConditions(
    state: SignUpUiState,
): String = when {
    state.hasAllergyCondition -> AuthRoute.SignUpAllergies
    state.hasVegetarianCondition -> AuthRoute.SignUpVegetarian
    else -> AuthRoute.SignUpProfile
}

private fun navigateToHome(navController: NavHostController) {
    navController.navigate(AuthRoute.Home) {
        popUpTo(AuthRoute.AuthChoice) { inclusive = true }
        launchSingleTop = true
    }
}

private const val SPLASH_DURATION_MILLIS = 1_000L
