package com.coworker.jjikmuk.feature.product.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.ProductSearchUiState
import com.coworker.jjikmuk.ui.component.JjikmukBackButton
import com.coworker.jjikmuk.ui.component.JjikmukProductListCard
import com.coworker.jjikmuk.ui.component.JjikmukProductListCardUiModel
import com.coworker.jjikmuk.ui.component.JjikmukSearchField
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
internal fun ProductSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    scannerResult: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (scannerResult) 68.dp else 67.dp)
                .border(
                    width = 0.5.dp,
                    color = JjikmukTheme.colors.borderSubtle,
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(start = if (scannerResult) 8.dp else 12.dp, end = if (scannerResult) 22.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JjikmukBackButton(
                onClick = onBackClick,
                iconResId = if (scannerResult) R.drawable.ic_scan_search_back else R.drawable.ic_arrow_back,
                iconSize = if (scannerResult) 24.dp else 30.dp,
            )
            JjikmukSearchField(
                value = query,
                placeholder = "어떤 안심 상품을 찾으시나요?",
                autoFocus = !scannerResult,
                clearIconResId = if (scannerResult) R.drawable.ic_scan_search_clear else null,
                onValueChange = onQueryChange,
                onClearClick = onClearClick,
                onSearchClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ProductSearchResultContent(
    searchUiState: ProductSearchUiState,
    onFilterClick: (() -> Unit)? = null,
    onProductClick: (ProductSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    decoratedImages: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surface),
    ) {
        ProductSearchResultHeader(
            query = searchUiState.submittedQuery,
            productCount = searchUiState.products.size,
            onFilterClick = onFilterClick,
        )

        when {
            searchUiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = JjikmukTheme.colors.brand)
                }
            }

            searchUiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = searchUiState.errorMessage,
                        color = JjikmukTheme.colors.textSecondary,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            searchUiState.products.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "검색 결과가 없어요.",
                        color = JjikmukTheme.colors.textSecondary,
                        style = JjikmukTheme.typography.bodyM,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = searchUiState.products,
                        key = { index, product -> "${product.barcode ?: product.productName}:$index" },
                    ) { _, product ->
                        JjikmukProductListCard(
                            product = product.toProductListCardUiModel(),
                            onClick = { onProductClick(product) },
                            decoratedImage = decoratedImages,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSearchResultHeader(
    query: String,
    productCount: Int,
    onFilterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(53.dp)
            .background(JjikmukTheme.colors.surface)
            .border(
                width = 0.5.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(0.dp),
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = JjikmukTheme.colors.textPrimary)) {
                    append("‘")
                    append(query)
                    append("' ")
                }
                append("검색 결과 ")
                withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) {
                    append(productCount.toString())
                    append("건")
                }
            },
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.labelM,
        )

        if (onFilterClick != null) {
            Surface(
                modifier = Modifier
                    .height(32.dp)
                    .clickable(onClick = onFilterClick),
                color = JjikmukTheme.colors.disabled,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter_sliders),
                        contentDescription = null,
                        tint = JjikmukTheme.colors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "상세필터",
                        color = JjikmukTheme.colors.textSecondary,
                        style = JjikmukTheme.typography.labelS,
                    )
                }
            }
        }
    }
}

private fun ProductSearchResult.toProductListCardUiModel(): JjikmukProductListCardUiModel =
    JjikmukProductListCardUiModel(
        brandName = brandName,
        productName = productName,
        barcode = barcode,
        imageUrl = imageUrl,
        allergyLabels = allergyLabels,
    )
