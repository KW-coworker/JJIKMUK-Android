package com.coworker.jjikmuk.feature.scanner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerAnalysisReportPlaceholder
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerMainRoute

@Composable
fun ScannerNavHost(
    onExitScanner: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ScannerRoute.Main,
        modifier = modifier,
    ) {
        composable(ScannerRoute.Main) {
            ScannerMainRoute(
                onBackClick = onExitScanner,
                onCompareListClick = {
                    navController.navigate(ScannerRoute.AnalysisReport) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(ScannerRoute.AnalysisReport) {
            ScannerAnalysisReportPlaceholder(
                onBackClick = navController::popBackStack,
            )
        }
    }
}
