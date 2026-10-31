package com.coworker.jjikmuk.feature.product.presentation.component

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
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
    emptyBottomPadding: Dp = 80.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surface),
    ) {
        ProductSearchResultHeader(
            query = searchUiState.submittedQuery,
            productCount = searchUiState.products.size,
            isLoading = searchUiState.isLoading,
            hasError = searchUiState.errorMessage != null,
            onFilterClick = onFilterClick.takeUnless { searchUiState.isEmptyResult },
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

            searchUiState.isEmptyResult -> {
                ProductSearchEmptyState(
                    modifier = Modifier.fillMaxSize().padding(bottom = emptyBottomPadding),
                )
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
    isLoading: Boolean,
    hasError: Boolean,    onFilterClick: (() -> Unit)? = null,
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
                when {
                    isLoading -> append("검색 중")
                    hasError -> append("검색 실패")
                    else -> {
                        append("검색 결과 ")
                        withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) {
                            append(productCount.toString())
                            append("건")
                        }
                    }
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

/** Shared by normal scanner results and Product search; failures are handled separately. */
@Composable
private fun ProductSearchEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(112.dp).background(Color(0xFFF3F4F9), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(84.dp).background(JjikmukTheme.colors.borderSubtle.copy(alpha = 0.6f), CircleShape))
            Image(painterResource(R.drawable.ic_product_search_empty), contentDescription = null,
                modifier = Modifier.size(52.dp))
        }
        Text(
            text = "검색된 상품이 존재하지 않습니다",
            style = JjikmukTheme.typography.h3,
            color = JjikmukTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "검색어를 다시 확인하거나\n다른 키워드로 검색해 보세요.",
            style = JjikmukTheme.typography.bodyM,
            color = JjikmukTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(name = "상품 검색 결과 없음", showBackground = true, widthDp = 375, heightDp = 600)
@Composable
private fun ProductSearchEmptyPreview() {
    JjikmukTheme {
        ProductSearchResultContent(
            searchUiState = ProductSearchUiState(query = "포키", submittedQuery = "포키", hasSearched = true),
            onProductClick = {},
        )
    }
}
