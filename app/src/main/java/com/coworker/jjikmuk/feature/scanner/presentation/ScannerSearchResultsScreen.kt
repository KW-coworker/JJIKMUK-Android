package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.ProductSearchUiState
import com.coworker.jjikmuk.feature.product.presentation.component.ProductSearchResultContent
import com.coworker.jjikmuk.feature.product.presentation.component.ProductSearchTopBar
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/**
 * Normal-scan search results (Figma 1391:16777). Receives actual search state from its owner;
 * OCR completion and product-detail navigation are deliberately callbacks, not timer-driven mocks.
 */
@Composable
fun ScannerSearchResultsScreen(
    searchUiState: ProductSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onProductClick: (ProductSearchResult) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(onBack = onBackClick)
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = JjikmukTheme.colors.surface,
        topBar = {
            ProductSearchTopBar(
                query = searchUiState.query,
                onQueryChange = onQueryChange,
                onClearClick = { onQueryChange("") },
                onSearchClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onSearchSubmit(searchUiState.query.trim())
                },
                onBackClick = onBackClick,
                scannerResult = true,
            )
        },
    ) { padding ->
        // A new submitted query starts at the first row; editing alone preserves the old results.
        key(searchUiState.submittedQuery) {
            ProductSearchResultContent(
                searchUiState = searchUiState,
                onProductClick = onProductClick,
                decoratedImages = false,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "일반 스캔 검색 결과", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerSearchResultsPreview() {
    var query by remember { mutableStateOf("포키") }
    JjikmukTheme {
        ScannerSearchResultsScreen(
            searchUiState = ProductSearchUiState(
                query = query,
                submittedQuery = "포키",
                hasSearched = true,
                products = List(8) { index ->
                    ProductSearchResult(
                        barcode = "preview-$index",
                        productName = "해태 포키 블루베리",
                        brandName = "해태제과",
                        imageUrl = "android.resource://com.coworker.jjikmuk/${R.drawable.scanner_search_preview_product}",
                        allergyLabels = listOf("우유", "땅콩"),
                    )
                },
            ),
            onQueryChange = { query = it },
            onSearchSubmit = {},
            onProductClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "일반 스캔 검색 결과 없음", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerSearchResultsEmptyPreview() {
    JjikmukTheme {
        ScannerSearchResultsScreen(
            searchUiState = ProductSearchUiState(query = "포키", submittedQuery = "포키", hasSearched = true),
            onQueryChange = {}, onSearchSubmit = {}, onProductClick = {}, onBackClick = {},
        )
    }
}
