package com.coworker.jjikmuk.feature.auth.navigation

import com.coworker.jjikmuk.feature.auth.presentation.profile.ProfileCompletionRoute
import com.coworker.jjikmuk.feature.auth.presentation.profile.ProfileCompletionViewModel
import com.coworker.jjikmuk.feature.auth.google.GoogleLoginRoute
import com.coworker.jjikmuk.feature.auth.google.GoogleLoginViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.coworker.jjikmuk.feature.mypage.presentation.AccountAction
import com.coworker.jjikmuk.feature.mypage.presentation.AccountActionRoute
import com.coworker.jjikmuk.feature.mypage.presentation.AccountActionViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coworker.jjikmuk.feature.auth.development.developerHomeGraph
import com.coworker.jjikmuk.feature.auth.development.openDeveloperHome
import com.coworker.jjikmuk.JjikmukAppContent
import com.coworker.jjikmuk.feature.auth.presentation.choice.AuthChoiceScreen
import com.coworker.jjikmuk.feature.auth.presentation.login.LoginViewModel
import com.coworker.jjikmuk.feature.auth.presentation.login.LoginRoute
import com.coworker.jjikmuk.feature.auth.presentation.login.SessionEntryState
import com.coworker.jjikmuk.feature.auth.presentation.login.SessionEntryScreen
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetViewModel
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetCompleteScreen
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetEmailRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetNewPasswordRoute
import com.coworker.jjikmuk.feature.auth.presentation.passwordreset.PasswordResetOtpRoute
import com.coworker.jjikmuk.feature.auth.presentation.permission.PermissionApprovalRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpConditionsRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpAllergiesRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpCompleteScreen
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpEmailRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpNicknameRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpOtpRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpPasswordRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpProfileRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpUiState
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpVegetarianRoute
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpViewModel
import com.coworker.jjikmuk.feature.auth.presentation.splash.SplashScreen
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun AuthNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    passwordResetViewModel: PasswordResetViewModel = hiltViewModel(),
    signUpViewModel: SignUpViewModel = hiltViewModel(),
    accountActionViewModel: AccountActionViewModel = hiltViewModel(),
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel(),
    googleLoginViewModel: GoogleLoginViewModel = hiltViewModel(),
    pendingPhotoViewModel: com.coworker.jjikmuk.feature.auth.photo.PendingSignupPhotoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(loginViewModel, context) {
        loginViewModel.notices.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    SharedTransitionLayout {
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Splash,
        modifier = modifier,
    ) {
        composable(
            route = AuthRoute.Splash,
            exitTransition = {
                fadeOut(animationSpec = tween(SPLASH_TRANSITION_MILLIS))
            },
        ) {
            LaunchedEffect(Unit) {
                delay(SPLASH_DURATION_MILLIS)
                val destination = if (loginViewModel.hasRestoredSession()) AuthRoute.SessionEntry else AuthRoute.PermissionApproval
                navController.navigate(destination) {
                    popUpTo(AuthRoute.Splash) { inclusive = true }
                }
            }
            SplashScreen(
                logoModifier = Modifier.sharedElement(
                    rememberSharedContentState("launch-logo"),
                    animatedVisibilityScope = this,
                    boundsTransform = { _, _ -> tween(SPLASH_TRANSITION_MILLIS, easing = LaunchEasing) },
                ),
            )
        }

        composable(
            route = AuthRoute.PermissionApproval,
            enterTransition = {
                fadeIn(animationSpec = tween(420, delayMillis = 180))
            },
        ) {
            PermissionApprovalRoute(
                logoModifier = Modifier.sharedElement(
                    rememberSharedContentState("launch-logo"),
                    animatedVisibilityScope = this,
                    boundsTransform = { _, _ -> tween(SPLASH_TRANSITION_MILLIS, easing = LaunchEasing) },
                ),
                onContinue = {
                    navController.navigate(AuthRoute.SessionEntry) {
                        popUpTo(AuthRoute.PermissionApproval) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.PermissionApproval) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AuthRoute.SessionEntry) {
            val session by loginViewModel.sessionState.collectAsStateWithLifecycle()
            val entry by loginViewModel.entryState.collectAsStateWithLifecycle()
            LaunchedEffect(session.generation) { loginViewModel.checkSession() }
            LaunchedEffect(entry, session) {
                val destination = when {
                    entry == SessionEntryState.SignedOut && session.credentials == null -> AuthRoute.AuthChoice
                    entry == SessionEntryState.ProfileRequired && session.credentials != null && session.profile?.profileCompleted == false -> AuthRoute.ProfileCompletion
                    else -> null
                }
                if (destination != null) navController.navigate(destination) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
            }
            if (entry == SessionEntryState.Ready && session.canEnterHome && session.profile != null) {
                com.coworker.jjikmuk.feature.auth.photo.PendingSignupPhotoRoute(pendingPhotoViewModel) {
                    if (navController.currentDestination?.route == AuthRoute.SessionEntry && loginViewModel.sessionState.value.canEnterHome)
                        navController.navigate(AuthRoute.Home) {
                            popUpTo(navController.graph.id) { inclusive = false }
                            launchSingleTop = true
                        }
                }
            } else SessionEntryScreen(entry, loginViewModel::checkSession, loginViewModel::logout)
        }

        composable(AuthRoute.ProfileCompletion) {
            fun returnToEntry() {
                if (navController.currentDestination?.route != AuthRoute.ProfileCompletion) return
                navController.navigate(AuthRoute.SessionEntry) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
            }
            ProfileCompletionRoute(profileCompletionViewModel, onFinished = ::returnToEntry,
                onSessionLost = ::returnToEntry, onLogout = loginViewModel::logout)
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
                        navController.navigate(AuthRoute.SessionEntry) {
                            popUpTo(AuthRoute.AuthChoice) { inclusive = true }
                        }
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
                    googleLoginViewModel.prepareEntry()
                    loginViewModel.reset()
                    navController.navigate(AuthRoute.GoogleLogin) { launchSingleTop = true }
                },
                onDeveloperHomeClick = {
                    loginViewModel.reset()
                    navController.openDeveloperHome()
                },
            )
        }

        composable(AuthRoute.GoogleLogin) {
            GoogleLoginRoute(googleLoginViewModel,
                onSignedIn = {
                    navController.navigate(AuthRoute.SessionEntry) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onPasswordReset = {
                    passwordResetViewModel.reset()
                    navController.navigate(AuthRoute.PasswordResetEmail) {
                        popUpTo(AuthRoute.GoogleLogin) { inclusive = true }
                    }
                })
        }

        passwordResetGraph(
            navController = navController,
            viewModel = passwordResetViewModel,
        )
        signUpGraph(
            navController = navController,
            viewModel = signUpViewModel,
        )

        developerHomeGraph(navController)

        for ((route, action) in listOf(AuthRoute.AccountPassword to AccountAction.Password,
            AuthRoute.AccountWithdrawal to AccountAction.Withdrawal)) {
            composable(route) {
                fun exitTo(destination: String) {
                    loginViewModel.reset()
                    navController.navigate(destination) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                AccountActionRoute(action, accountActionViewModel,
                    onBack = { navController.popBackStack() },
                    onAuthRequired = { exitTo(AuthRoute.SessionEntry) },
                    onComplete = { exitTo(if (action == AccountAction.Withdrawal) AuthRoute.AuthChoice else AuthRoute.Login) })
            }
        }

        composable(AuthRoute.Home) {
            val session by loginViewModel.sessionState.collectAsStateWithLifecycle()
            if (!session.canEnterHome || session.profile == null) {
                LaunchedEffect(session.generation, currentEntry?.destination?.route) {
                    if (currentEntry?.destination?.route != AuthRoute.Home) return@LaunchedEffect
                    signUpViewModel.reset()
                    passwordResetViewModel.reset()
                    navController.navigate(AuthRoute.SessionEntry) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            } else {
                key(session.generation) {
                    JjikmukAppContent(onReturnToAuthChoice = loginViewModel::logout, profile = session.profile,
                        onPasswordChangeClick = { navController.navigate(AuthRoute.AccountPassword) { launchSingleTop = true } },
                        onWithdrawalClick = { navController.navigate(AuthRoute.AccountWithdrawal) { launchSingleTop = true } })
                }
            }
        }
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
        SignUpProfileRoute(
            viewModel = viewModel,
            onEditConditionsClick = { navController.navigate(AuthRoute.SignUpConditions) },
            onBackClick = navController::popBackStack,
            onRegistrationComplete = {
                navController.navigate(AuthRoute.SignUpComplete) {
                    popUpTo(AuthRoute.SignUpEmail) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onReverifyEmail = {
                navController.navigate(AuthRoute.SignUpEmail) {
                    popUpTo(AuthRoute.SignUpEmail) { inclusive = true }
                }
            },
            onLoginClick = {
                viewModel.reset()
                navController.navigate(AuthRoute.Login) {
                    popUpTo(AuthRoute.AuthChoice) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onEditPassword = {
                navController.navigate(AuthRoute.SignUpPassword) {
                    popUpTo(AuthRoute.SignUpPassword) { inclusive = true }
                }
            },
        )
    }
    composable(AuthRoute.SignUpComplete) {
        SignUpCompleteScreen(
            onLoginClick = {
                viewModel.reset()
                navController.navigate(AuthRoute.Login) {
                    popUpTo(AuthRoute.AuthChoice) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
}

internal fun nextRouteAfterConditions(
    state: SignUpUiState,
): String = when {
    state.hasAllergyCondition -> AuthRoute.SignUpAllergies
    state.hasVegetarianCondition -> AuthRoute.SignUpVegetarian
    else -> AuthRoute.SignUpProfile
}

private const val SPLASH_DURATION_MILLIS = 1_800L
private const val SPLASH_TRANSITION_MILLIS = 780
private val LaunchEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
