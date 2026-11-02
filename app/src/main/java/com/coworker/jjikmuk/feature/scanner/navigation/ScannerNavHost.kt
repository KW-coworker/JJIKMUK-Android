package com.coworker.jjikmuk.feature.scanner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerAnalysisReportScreen
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerMainRoute
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerProductDetailRoute

@Composable
fun ScannerNavHost(
    onExitScanner: () -> Unit,
    modifier: Modifier = Modifier,
    onAskProduct: (String) -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ScannerRoute.Main,
        modifier = modifier,
    ) {
        composable(ScannerRoute.Main) { entry ->
            ScannerMainRoute(
                onBackClick = onExitScanner,
                onCompareListClick = {
                    navController.navigate(ScannerRoute.AnalysisReport) {
                        launchSingleTop = true
                    }
                },
                onProductClick = { barcode ->
                    // Ignore repeated taps once the results entry is no longer the resumed top entry.
                    if (navController.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    ) {
                        navController.navigate(ScannerRoute.productDetail(barcode))
                    }
                },
            )
        }

        composable(
            route = ScannerRoute.ProductDetail,
            arguments = listOf(navArgument(ScannerRoute.ProductDetailBarcodeArg) { type = NavType.StringType }),
        ) { entry ->
            val barcode = entry.arguments?.getString(ScannerRoute.ProductDetailBarcodeArg).orEmpty()
            ScannerProductDetailRoute(
                barcode = barcode,
                onBackClick = {
                    if (navController.currentBackStackEntry == entry) navController.popBackStack()
                },
                onAskProduct = onAskProduct,
            )
        }

        composable(ScannerRoute.AnalysisReport) {
            ScannerAnalysisReportScreen(
                onBackClick = navController::popBackStack,
            )
        }
    }
}
