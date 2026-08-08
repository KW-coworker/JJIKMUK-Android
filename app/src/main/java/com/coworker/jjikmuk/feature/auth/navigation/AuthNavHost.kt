package com.coworker.jjikmuk.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetEmailRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetNewPasswordRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetOtpRoute
import com.coworker.jjikmuk.feature.auth.presentation.placeholder.AuthPlaceholderScreen
import com.coworker.jjikmuk.feature.auth.presentation.placeholder.ConditionsPlaceholderScreen
import com.coworker.jjikmuk.feature.auth.presentation.placeholder.PlaceholderAction
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpUiState
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
    val signUpState by signUpViewModel.uiState.collectAsStateWithLifecycle()

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
            state = signUpState,
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
        AuthPlaceholderScreen(
            title = "새 비밀번호 생성 완료",
            primaryActions = listOf(
                PlaceholderAction("로그인하기") {
                    viewModel.reset()
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.Login) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            ),
            blockSystemBack = true,
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.signUpGraph(
    navController: NavHostController,
    viewModel: SignUpViewModel,
    state: SignUpUiState,
) {
    composable(AuthRoute.SignUpEmail) {
        AuthPlaceholderScreen(
            title = "회원가입 - 이메일 입력",
            primaryActions = listOf(
                PlaceholderAction("다음") { navController.navigate(AuthRoute.SignUpOtp) },
            ),
            onBackClick = {
                viewModel.reset()
                navController.popBackStack()
            },
        )
    }
    composable(AuthRoute.SignUpOtp) {
        AuthPlaceholderScreen(
            title = "회원가입 - OTP 인증",
            primaryActions = listOf(
                PlaceholderAction("인증 성공") {
                    viewModel.markOtpVerified()
                    navController.navigate(AuthRoute.SignUpPassword) {
                        popUpTo(AuthRoute.SignUpOtp) { inclusive = true }
                    }
                },
            ),
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpPassword) {
        AuthPlaceholderScreen(
            title = "회원가입 - 비밀번호 생성",
            primaryActions = listOf(
                PlaceholderAction("완료") { navController.navigate(AuthRoute.SignUpNickname) },
            ),
            onBackClick = {
                viewModel.restartFromEmail()
                navController.popBackStack()
            },
        )
    }
    composable(AuthRoute.SignUpNickname) {
        AuthPlaceholderScreen(
            title = "회원가입 - 닉네임 생성",
            primaryActions = listOf(
                PlaceholderAction("완료") { navController.navigate(AuthRoute.SignUpConditions) },
            ),
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpConditions) {
        ConditionsPlaceholderScreen(
            hasVegetarianCondition = state.hasVegetarianCondition,
            hasAllergyCondition = state.hasAllergyCondition,
            onConditionsChange = viewModel::updateConditions,
            onNextClick = { navController.navigate(nextRouteAfterConditions(state)) },
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpVegetarian) {
        AuthPlaceholderScreen(
            title = "회원가입 - 채식 식단 선택",
            primaryActions = listOf(
                PlaceholderAction("선택 완료") {
                    navController.navigate(
                        if (state.hasAllergyCondition) AuthRoute.SignUpAllergies else AuthRoute.SignUpProfile,
                    )
                },
            ),
            onBackClick = navController::popBackStack,
        )
    }
    composable(AuthRoute.SignUpAllergies) {
        AuthPlaceholderScreen(
            title = "회원가입 - 알레르기 항목 선택",
            primaryActions = listOf(
                PlaceholderAction("선택 완료") { navController.navigate(AuthRoute.SignUpProfile) },
            ),
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

private fun nextRouteAfterConditions(state: SignUpUiState): String = when {
    state.hasVegetarianCondition -> AuthRoute.SignUpVegetarian
    state.hasAllergyCondition -> AuthRoute.SignUpAllergies
    else -> AuthRoute.SignUpProfile
}

private fun navigateToHome(navController: NavHostController) {
    navController.navigate(AuthRoute.Home) {
        popUpTo(AuthRoute.AuthChoice) { inclusive = true }
        launchSingleTop = true
    }
}

private const val SPLASH_DURATION_MILLIS = 1_000L
