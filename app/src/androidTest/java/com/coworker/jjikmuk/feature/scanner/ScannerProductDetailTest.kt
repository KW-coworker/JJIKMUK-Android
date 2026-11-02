package com.coworker.jjikmuk.feature.scanner

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.pressBack
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductMacroPercents
import com.coworker.jjikmuk.domain.model.ProductNutrition
import com.coworker.jjikmuk.domain.model.ProductSafetyStatus
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.ProductDetailUiState
import com.coworker.jjikmuk.feature.product.presentation.ProductSearchUiState
import com.coworker.jjikmuk.feature.scanner.navigation.ScannerRoute
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerProductDetailScreen
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerSearchResultsScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScannerProductDetailTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun detailScreen(
        detail: ProductDetailUiState,
        showAlternatives: Boolean = false,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) = compose.setContent {
        JjikmukTheme {
            ScannerProductDetailScreen(
                detail = detail, isLiked = false, showAlternatives = showAlternatives,
                onBackClick = onBack, onRetry = onRetry,
                onFavorite = {}, onCapacity = {}, onAsk = {}, onAlternative = {},
                onMoreAlternatives = {}, onRetryAlternatives = {},
            )
        }
    }

    private fun product() = ProductDetail(
        barcode = "8801019", productName = "스캔 상세 테스트 상품", brandName = "테스트제과",
        imageUrl = null, allergyLabels = emptyList(), allergyWarning = null,
        rawMaterials = "소맥분", totalWeight = "41g",
        nutrition = ProductNutrition(200.0, 20.0, 10.0, 5.0, null, null, null),
        macroPercents = ProductMacroPercents(50.0, 20.0, 30.0), safetyStatus = ProductSafetyStatus.Pass,
        dangerousIngredients = emptyList(), analysisMessage = null, isDangerous = false,
    )

    @Test fun failedDetailOffersRetryAndReturnToScanResults() {
        var retries = 0
        var backs = 0
        detailScreen(
            ProductDetailUiState(referenceBarcode = "0001", errorMessage = "상품 정보를 불러오지 못했어요."),
            onBack = { backs++ }, onRetry = { retries++ },
        )
        compose.onNodeWithText("상품 정보를 불러오지 못했어요.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.onNodeWithText("스캔 결과로 돌아가기").performClick()
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(2, backs)
        }
    }

    @Test fun loadingAndNotYetStartedDetailShowNoStaleProductOrRetry() {
        val state = mutableStateOf(ProductDetailUiState())
        compose.setContent {
            JjikmukTheme {
                ScannerProductDetailScreen(
                    detail = state.value, isLiked = false, showAlternatives = false,
                    onBackClick = {}, onRetry = {}, onFavorite = {}, onCapacity = {}, onAsk = {},
                    onAlternative = {}, onMoreAlternatives = {}, onRetryAlternatives = {},
                )
            }
        }
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
        compose.runOnIdle { state.value = ProductDetailUiState(isLoading = true, referenceBarcode = "0001") }
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
        compose.onAllNodesWithText("스캔 상세 테스트 상품").fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        compose.runOnIdle { state.value = ProductDetailUiState(product = product(), referenceBarcode = "8801019") }
        compose.onAllNodesWithText("스캔 상세 테스트 상품")[0].assertIsDisplayed()
    }

    @Test fun loadedDetailUsesSharedDesignAndAlternativesHaveTitle() {
        val alternatives = mutableStateOf(false)
        compose.setContent {
            JjikmukTheme {
                ScannerProductDetailScreen(
                    detail = ProductDetailUiState(product = product(), referenceBarcode = "8801019"),
                    isLiked = false, showAlternatives = alternatives.value,
                    onBackClick = { alternatives.value = false }, onRetry = {}, onFavorite = {}, onCapacity = {},
                    onAsk = {}, onAlternative = {}, onMoreAlternatives = {}, onRetryAlternatives = {},
                )
            }
        }
        compose.onAllNodesWithText("스캔 상세 테스트 상품")[0].assertIsDisplayed()
        compose.onNodeWithText("대체 상품 추천").assertDoesNotExist()
        compose.runOnIdle { alternatives.value = true }
        compose.onNodeWithText("대체 상품 추천").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.onNodeWithText("대체 상품 추천").assertDoesNotExist()
    }

    /**
     * Mirrors ScannerNavHost's Main → ProductDetail wiring with the real route helpers and results
     * screen: the barcode survives encoding untouched and returning keeps query, list and scroll.
     */
    @Test fun detailRouteKeepsBarcodeAndReturnRestoresResultsScroll() {
        val barcode = "0088 ab/가?#&"
        val search = mutableStateOf(ProductSearchUiState(
            query = "포키", submittedQuery = "포키", hasSearched = true,
            products = List(12) { index ->
                ProductSearchResult(
                    barcode = if (index == 11) barcode else "code-$index",
                    productName = if (index == 11) "목록 마지막 상품" else "후보 상품 $index",
                    brandName = "테스트제과", imageUrl = null, allergyLabels = emptyList(),
                )
            },
        ))
        var opened: String? = null
        var searches = 0
        lateinit var nav: TestNavHostController
        compose.setContent {
            nav = TestNavHostController(compose.activity).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            JjikmukTheme {
                NavHost(nav, startDestination = ScannerRoute.Main) {
                    composable(ScannerRoute.Main) {
                        ScannerSearchResultsScreen(
                            searchUiState = search.value,
                            onQueryChange = { search.value = search.value.copy(query = it) },
                            onSearchSubmit = { searches++ },
                            onProductClick = { product ->
                                product.barcode?.let { nav.navigate(ScannerRoute.productDetail(it)) }
                            },
                            onBackClick = {},
                        )
                    }
                    composable(
                        ScannerRoute.ProductDetail,
                        arguments = listOf(navArgument(ScannerRoute.ProductDetailBarcodeArg) { type = NavType.StringType }),
                    ) { entry ->
                        opened = entry.arguments?.getString(ScannerRoute.ProductDetailBarcodeArg)
                        TextButton(onClick = { nav.popBackStack() }) { Text("상세 화면") }
                    }
                }
            }
        }

        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(11)
        compose.onNodeWithText("목록 마지막 상품").performClick()
        compose.onNodeWithText("상세 화면").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(barcode, opened)
            assertEquals(ScannerRoute.ProductDetail, nav.currentDestination?.route)
        }

        pressBack()
        compose.onNodeWithText("목록 마지막 상품").assertIsDisplayed()
        compose.onNodeWithText("‘포키' 검색 결과 12건").assertIsDisplayed()

        compose.onNodeWithText("목록 마지막 상품").performClick()
        compose.onNodeWithText("상세 화면").performClick()
        compose.onNodeWithText("목록 마지막 상품").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(ScannerRoute.Main, nav.currentDestination?.route)
            assertEquals("Returning from detail must not re-run search", 0, searches)
        }
    }
}
