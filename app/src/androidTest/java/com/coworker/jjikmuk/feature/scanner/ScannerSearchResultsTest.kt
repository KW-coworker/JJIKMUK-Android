package com.coworker.jjikmuk.feature.scanner

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.ProductSearchUiState
import com.coworker.jjikmuk.feature.product.presentation.component.ProductSearchResultContent
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerSearchResultsScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class ScannerSearchResultsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun resultsSupportSelectionScrollingEditingClearSearchAndBack() {
        val state = mutableStateOf(resultsState())
        var selected: ProductSearchResult? = null
        var submitted: String? = null
        var backs = 0
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            JjikmukTheme {
                ScannerSearchResultsScreen(
                    searchUiState = state.value,
                    onQueryChange = { state.value = state.value.copy(query = it) },
                    onSearchSubmit = { submitted = it; state.value = state.value.copy(submittedQuery = it) },
                    onProductClick = { selected = it },
                    onBackClick = { backs++ },
                )
            }
        }
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
        compose.onNodeWithText("‘포키' 검색 결과 8건").assertIsDisplayed()
        compose.onNodeWithText("상세필터").assertDoesNotExist()
        // Wait for the local Coil image before comparing the Figma card geometry.
        compose.waitUntil(5_000) {
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            ((pixels.height * 0.23f).toInt()..(pixels.height * 0.34f).toInt()).any { y ->
                ((pixels.width * 0.72f).toInt()..(pixels.width * 0.87f).toInt()).any { x ->
                    val color = pixels[x, y]
                    color.red in 0.25f..0.7f && color.blue > color.red + 0.07f
                }
            }
        }
        saveFrame()
        compose.onAllNodesWithText("해태 포키 블루베리")[0].performClick()
        compose.runOnIdle { assertEquals(state.value.products.first(), selected) }
        val list = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
        list.performScrollToIndex(7)
        compose.onNodeWithText("목록 마지막 상품").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(query = "수정 중") }
        compose.onNodeWithText("목록 마지막 상품").assertIsDisplayed()
        compose.onNodeWithText("‘포키' 검색 결과 8건").assertIsDisplayed()
        compose.onNodeWithContentDescription("검색어 지우기").performClick()
        compose.runOnIdle { assertEquals("", state.value.query) }
        compose.onNode(hasSetTextAction()).performTextReplacement("  블루베리  ")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals("블루베리", submitted) }
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
        compose.onNodeWithText("‘블루베리' 검색 결과 8건").assertIsDisplayed()
        compose.onAllNodesWithText("해태 포키 블루베리")[0].assertIsDisplayed()
        // Focus clearing is synchronous, but the platform IME hide animation is not.
        // Wait until Back will reach the screen instead of dismissing the keyboard.
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        pressBack()
        compose.runOnIdle { assertEquals(2, backs) }
    }

    @Test fun sharedResultsKeepFilterAndShowEmptyErrorLoadingWithoutStaleCards() {
        val state = mutableStateOf(resultsState())
        var filters = 0
        compose.setContent {
            JjikmukTheme {
                ProductSearchResultContent(
                    searchUiState = state.value,
                    onProductClick = {},
                    onFilterClick = { filters++ },
                )
            }
        }
        compose.onNodeWithText("상세필터").performClick()
        compose.runOnIdle { assertEquals(1, filters); state.value = state.value.copy(isLoading = true) }
        compose.onAllNodesWithText("해태 포키 블루베리").fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        compose.runOnIdle { state.value = state.value.copy(products = emptyList(), isLoading = false) }
        compose.onNodeWithText("검색 결과가 없어요.").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(errorMessage = "검색에 실패했어요.") }
        compose.onNodeWithText("검색에 실패했어요.").assertIsDisplayed()
        compose.onNodeWithText("검색 결과가 없어요.").assertDoesNotExist()
    }

    private fun resultsState() = ProductSearchUiState(
        query = "포키", submittedQuery = "포키", hasSearched = true,
        products = List(8) { index ->
            ProductSearchResult(
                // Duplicate/missing IDs must not crash the LazyColumn.
                barcode = if (index % 2 == 0) null else "same-barcode",
                productName = if (index == 7) "목록 마지막 상품" else "해태 포키 블루베리",
                brandName = "해태제과",
                imageUrl = "android.resource://com.coworker.jjikmuk/${R.drawable.scanner_search_preview_product}",
                allergyLabels = listOf("우유", "땅콩"),
            )
        },
    )

    private fun saveFrame() {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "scanner-search-results.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
