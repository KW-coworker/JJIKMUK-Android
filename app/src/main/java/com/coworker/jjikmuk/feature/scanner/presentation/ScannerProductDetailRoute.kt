package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.feature.product.presentation.ProductAlternativeList
import com.coworker.jjikmuk.feature.product.presentation.ProductDetailDesign
import com.coworker.jjikmuk.feature.product.presentation.ProductDetailUiState
import com.coworker.jjikmuk.feature.product.presentation.ProductViewModel
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

/**
 * Product detail opened from a general-scan candidate.
 *
 * The ViewModel is scoped to this navigation entry, so popping back to the scan results cancels
 * in-flight detail work and the scan results entry keeps its own state (query, list, scroll).
 * Detail loading and the recently-viewed record reuse [ProductViewModel] unchanged.
 */
@Composable
internal fun ScannerProductDetailRoute(
    barcode: String,
    onBackClick: () -> Unit,
    onAskProduct: (String) -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val detail by viewModel.detailUiState.collectAsStateWithLifecycle()
    val likedBarcodes by viewModel.likedProductBarcodes.collectAsStateWithLifecycle()
    var showAlternatives by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(barcode) {
        if (detail.referenceBarcode == null) viewModel.loadProductDetail(barcode)
    }
    BackHandler(enabled = showAlternatives) { showAlternatives = false }

    ScannerProductDetailScreen(
        detail = detail,
        isLiked = detail.product?.barcode?.let { it in likedBarcodes } == true,
        showAlternatives = showAlternatives,
        onBackClick = { if (showAlternatives) showAlternatives = false else onBackClick() },
        onRetry = { viewModel.loadProductDetail(detail.referenceBarcode ?: barcode) },
        onFavorite = { detail.product?.let(viewModel::toggleLikedProduct) },
        onCapacity = viewModel::selectProductCapacity,
        onAsk = onAskProduct,
        onAlternative = { code ->
            if (code != null) {
                showAlternatives = false
                viewModel.loadProductDetail(code)
            }
        },
        onMoreAlternatives = { showAlternatives = true },
        onRetryAlternatives = viewModel::refreshAlternatives,
    )
}

@Composable
internal fun ScannerProductDetailScreen(
    detail: ProductDetailUiState,
    isLiked: Boolean,
    showAlternatives: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onFavorite: () -> Unit,
    onCapacity: (String) -> Unit,
    onAsk: (String) -> Unit,
    onAlternative: (String?) -> Unit,
    onMoreAlternatives: () -> Unit,
    onRetryAlternatives: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JjikmukTheme.colors.surface,
        topBar = {
            JjikmukTopAppBar(
                leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                showBottomDivider = showAlternatives,
                centerContent = {
                    if (showAlternatives) {
                        Text(
                            text = "대체 상품 추천",
                            style = JjikmukTheme.typography.labelL,
                            color = JjikmukTheme.colors.textPrimary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JjikmukTheme.colors.surface)
                .padding(padding),
        ) {
            when {
                detail.isLoading -> CircularProgressIndicator(
                    color = JjikmukTheme.colors.brand,
                    modifier = Modifier.align(Alignment.Center),
                )

                detail.errorMessage != null -> ScannerProductDetailError(
                    message = detail.errorMessage,
                    onRetry = onRetry,
                    onBack = onBackClick,
                    modifier = Modifier.align(Alignment.Center),
                )

                detail.product != null && showAlternatives -> ProductAlternativeList(
                    state = detail,
                    onProduct = onAlternative,
                    onRetry = onRetryAlternatives,
                )

                detail.product != null -> ProductDetailDesign(
                    state = detail,
                    isLiked = isLiked,
                    onFavorite = onFavorite,
                    onCapacity = onCapacity,
                    onAsk = onAsk,
                    onAlternative = onAlternative,
                    onMoreAlternatives = onMoreAlternatives,
                    onRetryAlternatives = onRetryAlternatives,
                )

                // Before the first request starts, show progress instead of an empty screen.
                else -> CircularProgressIndicator(
                    color = JjikmukTheme.colors.brand,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ScannerProductDetailError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyM,
            textAlign = TextAlign.Center,
        )
        JjikmukPrimaryButton(text = "다시 시도", onClick = onRetry)
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("스캔 결과로 돌아가기", color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelM)
        }
    }
}
