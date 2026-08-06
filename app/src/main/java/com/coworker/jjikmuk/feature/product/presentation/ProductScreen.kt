package com.coworker.jjikmuk.feature.product.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.product.presentation.component.JjikmukProductCard
import com.coworker.jjikmuk.feature.product.presentation.component.JjikmukProductCardSize
import com.coworker.jjikmuk.feature.product.presentation.component.JjikmukProductCardUiModel
import com.coworker.jjikmuk.ui.component.JjikmukBackButton
import com.coworker.jjikmuk.ui.component.JjikmukBottomNavigationBar
import com.coworker.jjikmuk.ui.component.JjikmukDraggableScannerFab
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.component.MainTab
import com.coworker.jjikmuk.ui.component.ScanTargetMemberUiModel
import com.coworker.jjikmuk.ui.component.ScanTargetPopup
import com.coworker.jjikmuk.ui.component.defaultScanTargetMembers
import com.coworker.jjikmuk.ui.component.toScanTargetProfiles
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun ProductScreen(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanTargetMembers = remember {
        mutableStateListOf<ScanTargetMemberUiModel>().apply {
            addAll(defaultScanTargetMembers())
        }
    }
    val selectedProfiles = scanTargetMembers.toScanTargetProfiles(
        defaultImageResId = R.drawable.ic_launcher_foreground,
    )
    val recentSearchKeywords = remember {
        mutableStateListOf("오트밀", "아몬드브리즈", "비건 식빵", "무염버터")
    }

    var showScanTargetPopup by rememberSaveable { mutableStateOf(false) }
    var currentDestination by rememberSaveable { mutableStateOf(ProductDestination.Overview) }
    var showProductFilterSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(ProductSortOption.Recommend) }
    var selectedCategory by rememberSaveable { mutableStateOf("전체") }
    var selectedProfileFilter by rememberSaveable { mutableStateOf("비건 인증") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            when (currentDestination) {
                ProductDestination.Overview -> {
                    JjikmukTopAppBar(
                        selectedProfiles = selectedProfiles,
                        onScanTargetClick = { showScanTargetPopup = true },
                    )
                }

                ProductDestination.RecommendationList -> {
                    JjikmukTopAppBar(
                        selectedProfiles = selectedProfiles,
                        onScanTargetClick = { showScanTargetPopup = true },
                        leading = JjikmukTopAppBarLeading.Back(
                            onClick = { currentDestination = ProductDestination.Overview },
                        ),
                        centerContent = {
                            Text(
                                text = "맞춤 안심 상품 추천",
                                color = JjikmukTheme.colors.textPrimary,
                                style = JjikmukTheme.typography.labelL,
                            )
                        },
                        showBottomDivider = true,
                    )
                }

                ProductDestination.Search -> {
                    ProductSearchTopBar(
                        onBackClick = { currentDestination = ProductDestination.Overview },
                    )
                }
            }
        },
        bottomBar = {
            if (!showProductFilterSheet && currentDestination != ProductDestination.Search) {
                JjikmukBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabClick = onTabClick,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JjikmukTheme.colors.background)
                .padding(innerPadding),
        ) {
            when (currentDestination) {
                ProductDestination.Overview -> {
                    ProductContent(
                        onSearchClick = { currentDestination = ProductDestination.Search },
                        onRecommendationMoreClick = {
                            currentDestination = ProductDestination.RecommendationList
                        },
                    )

                    JjikmukDraggableScannerFab(
                        onClick = onScannerClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 21.dp, bottom = 16.dp),
                    )
                }

                ProductDestination.RecommendationList -> {
                    ProductRecommendationListContent(
                        products = recommendedProductListSamples.sortedBy(selectedSort),
                        productCount = 8,
                        onFilterClick = { showProductFilterSheet = true },
                    )

                    JjikmukDraggableScannerFab(
                        onClick = onScannerClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 21.dp, bottom = 16.dp),
                    )
                }

                ProductDestination.Search -> {
                    ProductSearchContent(
                        recentKeywords = recentSearchKeywords,
                        onDeleteRecentKeyword = { keyword -> recentSearchKeywords.remove(keyword) },
                        onClearRecentKeywords = { recentSearchKeywords.clear() },
                    )
                }
            }

            if (showScanTargetPopup) {
                ScanTargetPopup(
                    members = scanTargetMembers,
                    onMemberCheckedChange = { memberId, checked ->
                        val memberIndex = scanTargetMembers.indexOfFirst { member -> member.id == memberId }
                        if (memberIndex >= 0) {
                            scanTargetMembers[memberIndex] = scanTargetMembers[memberIndex].copy(
                                isSelected = checked,
                            )
                        }
                    },
                    onDismissRequest = { showScanTargetPopup = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 15.dp),
                )
            }

            if (showProductFilterSheet && currentDestination == ProductDestination.RecommendationList) {
                ProductFilterOverlay(
                    selectedSort = selectedSort,
                    selectedCategory = selectedCategory,
                    selectedProfileFilter = selectedProfileFilter,
                    productCount = 8,
                    onSortSelected = { selectedSort = it },
                    onCategorySelected = { selectedCategory = it },
                    onProfileFilterSelected = { selectedProfileFilter = it },
                    onResetClick = {
                        selectedSort = ProductSortOption.Recommend
                        selectedCategory = "전체"
                        selectedProfileFilter = "비건 인증"
                    },
                    onDismissRequest = { showProductFilterSheet = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ProductContent(
    onSearchClick: () -> Unit,
    onRecommendationMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ProductHeader(onSearchClick = onSearchClick)
        CategorySection()
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(JjikmukTheme.colors.surfaceSecondary),
        )
        RecommendedProductSection(
            onMoreClick = onRecommendationMoreClick,
        )
    }
}

@Composable
private fun ProductHeader(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(start = 20.dp, end = 20.dp),
    ) {
        Text(
            text = "Product",
            color = JjikmukTheme.colors.brand,
            style = JjikmukTheme.typography.section,
            modifier = Modifier.padding(start = 10.dp),
        )

        ProductSearchBar(
            onClick = onSearchClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ProductSearchBar(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .height(51.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(16.dp),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(start = 16.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_product_search),
            contentDescription = null,
            tint = JjikmukTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "어떤 안심 상품을 찾으시나요?",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyM,
            modifier = Modifier.padding(start = 9.dp),
        )
    }
}

@Composable
private fun ProductSearchTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                .height(67.dp)
                .border(
                    width = 0.5.dp,
                    color = JjikmukTheme.colors.borderSubtle,
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(start = 12.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JjikmukBackButton(onClick = onBackClick)
            ProductSearchInputField(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProductSearchInputField(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(43.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(start = 17.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "어떤 안심 상품을 찾으시나요?",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyS,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Icon(
            painter = painterResource(R.drawable.ic_product_search),
            contentDescription = null,
            tint = JjikmukTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProductSearchContent(
    recentKeywords: List<String>,
    onDeleteRecentKeyword: (String) -> Unit,
    onClearRecentKeywords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surfaceSecondary.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(465.dp)
                .background(JjikmukTheme.colors.surface)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            ProductSearchSectionHeader(
                title = "최근 검색어",
                iconResId = R.drawable.ic_recent_search,
                actionText = "전체 삭제",
                onActionClick = onClearRecentKeywords,
            )

            ProductRecentSearchChips(
                keywords = recentKeywords,
                onDeleteClick = onDeleteRecentKeyword,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            ProductSearchSectionHeader(
                title = "인기 검색어",
                iconResId = R.drawable.ic_trending_up,
                iconColor = JjikmukTheme.colors.brand,
            )

            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                popularSearchKeywords.forEachIndexed { index, keyword ->
                    ProductPopularSearchRow(
                        rank = index + 1,
                        keyword = keyword,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductSearchSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    iconText: String? = null,
    @DrawableRes iconResId: Int? = null,
    actionText: String? = null,
    iconColor: Color = JjikmukTheme.colors.textSecondary,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp),
                )
            } else if (iconText != null) {
                Text(
                    text = iconText,
                    color = iconColor,
                    style = JjikmukTheme.typography.titleL,
                )
            }
            Text(
                text = title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.titleL,
            )
        }

        if (actionText != null) {
            Text(
                text = actionText,
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyS,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductRecentSearchChips(
    keywords: List<String>,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keywords.forEach { keyword ->
            ProductRecentSearchChip(
                keyword = keyword,
                onDeleteClick = { onDeleteClick(keyword) },
            )
        }
    }
}

@Composable
private fun ProductRecentSearchChip(
    keyword: String,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(32.dp),
        color = JjikmukTheme.colors.surfaceSecondary,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = JjikmukTheme.colors.borderSubtle,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = keyword,
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyS,
                maxLines = 1,
            )
            Text(
                text = "×",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyS,
                modifier = Modifier.clickable(onClick = onDeleteClick),
            )
        }
    }
}

@Composable
private fun ProductPopularSearchRow(
    rank: Int,
    keyword: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = {}),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rank.toString(),
            color = if (rank <= 3) JjikmukTheme.colors.brand else JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.labelL.asEnglish(),
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = keyword,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.bodyM,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategorySection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(285.dp)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = "카테고리별 찾기",
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.titleL,
        )

        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ProductCategoryRow(categories = ProductCategoryUiModel.samples.take(4))
            ProductCategoryRow(categories = ProductCategoryUiModel.samples.drop(4))
        }
    }
}

@Composable
private fun ProductCategoryRow(
    categories: List<ProductCategoryUiModel>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        categories.forEach { category ->
            ProductCategoryButton(category = category)
        }
    }
}

@Composable
private fun ProductCategoryButton(
    category: ProductCategoryUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .clickable(onClick = {}),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.1f),
                )
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(category.backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.emoji,
                style = JjikmukTheme.typography.h1,
            )
        }
        Text(
            text = category.label,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecommendedProductSection(
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(370.dp)
            .padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "코워커님을 위한",
                    color = JjikmukTheme.colors.brand,
                    style = JjikmukTheme.typography.labelS,
                )
                Text(
                    text = "맞춤 안심 상품 추천",
                    color = JjikmukTheme.colors.textPrimary,
                    style = JjikmukTheme.typography.titleL,
                )
            }
            Row(
                modifier = Modifier.clickable(onClick = onMoreClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "전체보기",
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.labelM,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = JjikmukTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .padding(top = 16.dp)
                .height(262.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(recommendedProductSamples) { product ->
                JjikmukProductCard(
                    product = product,
                    size = JjikmukProductCardSize.Grid,
                )
            }
        }
    }
}

@Composable
private fun ProductRecommendationListContent(
    products: List<JjikmukProductCardUiModel>,
    productCount: Int,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.background),
    ) {
        ProductRecommendationFilterHeader(
            productCount = productCount,
            onFilterClick = onFilterClick,
        )

        ProductRecommendationIntroCard(
            userName = "코워커",
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 20.dp, bottom = 111.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(products) { product ->
                JjikmukProductCard(
                    product = product,
                    size = JjikmukProductCardSize.Grid,
                )
            }
        }
    }
}

@Composable
private fun ProductRecommendationFilterHeader(
    productCount: Int,
    onFilterClick: () -> Unit,
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
                append("총 ")
                withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) {
                    append(productCount.toString())
                }
                append("개")
            },
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.labelM,
        )

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

@Composable
private fun ProductRecommendationIntroCard(
    userName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0x80D2E3C2),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE6F4E4), Color(0xFFF2F9F1)),
                    ),
                )
                .padding(17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${userName}님을 위한 추천",
                    color = Color(0xFF0C4120),
                    style = JjikmukTheme.typography.labelS,
                )
                Text(
                    text = "설정한신 프로필 기반으로\n딱 맞는 상품들을 준비했어요.",
                    color = Color(0xFF4A5565),
                    style = JjikmukTheme.typography.caption,
                )
            }

            Surface(
                modifier = Modifier.size(40.dp),
                color = JjikmukTheme.colors.surface,
                shape = CircleShape,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🧑🏻",
                        style = JjikmukTheme.typography.h3,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductFilterOverlay(
    selectedSort: ProductSortOption,
    selectedCategory: String,
    selectedProfileFilter: String,
    productCount: Int,
    onSortSelected: (ProductSortOption) -> Unit,
    onCategorySelected: (String) -> Unit,
    onProfileFilterSelected: (String) -> Unit,
    onResetClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(onClick = onDismissRequest),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(551.dp),
        color = JjikmukTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 15.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProductFilterSheetHeader(
                onDismissRequest = onDismissRequest,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp),
            ) {
                ProductFilterSectionTitle(text = "정렬 기준")
                ProductFilterChipRows(
                    labels = ProductSortOption.entries.map { it.label },
                    selectedLabel = selectedSort.label,
                    onSelected = { label ->
                        ProductSortOption.entries
                            .firstOrNull { option -> option.label == label }
                            ?.let(onSortSelected)
                    },
                )

                Spacer(modifier = Modifier.height(32.dp))

                ProductFilterSectionTitle(text = "카테고리")
                ProductFilterChipRows(
                    labels = categoryFilterOptions,
                    selectedLabel = selectedCategory,
                    onSelected = onCategorySelected,
                    showCheckWhenSelected = true,
                )

                Spacer(modifier = Modifier.height(32.dp))

                ProductFilterSectionTitle(text = "내 프로필 기반")
                ProductFilterChipRows(
                    labels = profileFilterOptions,
                    selectedLabel = selectedProfileFilter,
                    onSelected = onProfileFilterSelected,
                )
            }

            ProductFilterSheetActions(
                productCount = productCount,
                onResetClick = onResetClick,
                onApplyClick = onDismissRequest,
            )
        }
    }
}

@Composable
private fun ProductFilterSheetHeader(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 0.5.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(0.dp),
            )
            .padding(start = 20.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "상세 필터",
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.h3,
        )

        Surface(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onDismissRequest),
            color = JjikmukTheme.colors.surfaceSecondary,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "×",
                    color = JjikmukTheme.colors.textTertiary,
                    style = JjikmukTheme.typography.h3,
                )
            }
        }
    }
}

@Composable
private fun ProductFilterSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = JjikmukTheme.colors.textPrimary,
        style = JjikmukTheme.typography.titleM,
        modifier = modifier.padding(bottom = 12.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductFilterChipRows(
    labels: List<String>,
    selectedLabel: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showCheckWhenSelected: Boolean = false,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        labels.forEach { label ->
            ProductFilterChip(
                label = label,
                selected = label == selectedLabel,
                showCheck = showCheckWhenSelected && label == selectedLabel,
                onClick = { onSelected(label) },
            )
        }
    }
}

@Composable
private fun ProductFilterChip(
    label: String,
    selected: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSortStyle = selected && !showCheck
    val backgroundColor = when {
        selectedSortStyle -> JjikmukTheme.colors.brandPressed
        selected -> JjikmukTheme.colors.brandSubtlest
        else -> JjikmukTheme.colors.surface
    }
    val borderColor = when {
        selectedSortStyle -> JjikmukTheme.colors.brandPressed
        selected -> JjikmukTheme.colors.brand
        else -> JjikmukTheme.colors.borderSubtle
    }
    val textColor = when {
        selectedSortStyle -> JjikmukTheme.colors.surface
        selected -> JjikmukTheme.colors.brandPressed
        else -> JjikmukTheme.colors.textSecondary
    }

    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
        shadowElevation = if (selectedSortStyle) 3.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showCheck) {
                Text(
                    text = "✓",
                    color = textColor,
                    style = JjikmukTheme.typography.labelS,
                )
            }
            Text(
                text = label,
                color = textColor,
                style = JjikmukTheme.typography.labelS,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

@Composable
private fun ProductFilterSheetActions(
    productCount: Int,
    onResetClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(81.dp)
            .border(
                width = 0.5.dp,
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(0.dp),
            )
            .padding(start = 16.dp, top = 17.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .width(100.dp)
                .height(47.dp)
                .clickable(onClick = onResetClick),
            color = JjikmukTheme.colors.surface,
            shape = RoundedCornerShape(17.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = JjikmukTheme.colors.border,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "초기화",
                    color = JjikmukTheme.colors.textPrimary,
                    style = JjikmukTheme.typography.labelM,
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(47.dp)
                .clickable(onClick = onApplyClick),
            color = JjikmukTheme.colors.brandStrong,
            shape = RoundedCornerShape(17.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${productCount}개 상품 보기",
                    color = JjikmukTheme.colors.surface,
                    style = JjikmukTheme.typography.labelM,
                )
            }
        }
    }
}

private data class ProductCategoryUiModel(
    val label: String,
    val emoji: String,
    val backgroundColor: Color,
) {
    companion object {
        val samples = listOf(
            ProductCategoryUiModel("과자/디저트", "🍪", Color(0xFFFFF7ED)),
            ProductCategoryUiModel("간편/즉석식품", "🍱", Color(0xFFFEF2F2)),
            ProductCategoryUiModel("음료/유제품", "🥛", Color(0xFFEFF6FF)),
            ProductCategoryUiModel("베이커리/떡", "🥐", Color(0xFFFFFBEB)),
            ProductCategoryUiModel("면류", "🍜", Color(0xFFFEFCE8)),
            ProductCategoryUiModel("정육/가공육", "🥓", Color(0xFFFFF1F2)),
            ProductCategoryUiModel("소스/조미료", "🥫", Color(0xFFECFDF5)),
            ProductCategoryUiModel("특수/건강식", "🥗", Color(0xFFF0FDF4)),
        )
    }
}

private val recommendedProductSamples = listOf(
    JjikmukProductCardUiModel(
        brand = "아이얌",
        name = "글루텐프리 유기농 쌀과자",
        badge = "밀가루 무첨가",
        imageResId = R.drawable.img_gluten_free_rice_cookie,
    ),
    JjikmukProductCardUiModel(
        brand = "널담",
        name = "비건 초코 아몬드 쿠키",
        badge = "유제품 무첨가",
        imageResId = R.drawable.img_vegan_choco_cookie,
    ),
)

private val recommendedProductListSamples = recommendedProductSamples + listOf(
    JjikmukProductCardUiModel(
        brand = "거버",
        name = "유기농 사과 퓨레 100%",
        badge = "알레르겐 프리",
        imageResId = R.drawable.img_organic_apple_puree,
    ),
    JjikmukProductCardUiModel(
        brand = "잇츠베러",
        name = "비건 마요네즈",
        badge = "계란 무첨가",
        imageResId = R.drawable.img_vegan_mayonnaise,
    ),
)

private val categoryFilterOptions = listOf(
    "전체",
    "스낵/과자류",
    "음료/유제품",
    "소스/조미료",
    "베이커리",
    "건강식품",
)

private val profileFilterOptions = listOf(
    "비건 인증",
    "글루텐프리",
    "해썹(HACCP)",
    "글루텐프리",
    "무설탕/저당",
)

private val popularSearchKeywords = listOf(
    "글루텐프리 과자",
    "무첨가 두유",
    "단백질바",
    "어린이 안심 간식",
    "알러지프리 빵",
)

private enum class ProductSortOption(val label: String) {
    Recommend("추천순"),
    Popular("인기순"),
    Review("리뷰 많은 순"),
    LowPrice("낮은 가격순"),
    HighPrice("높은 가격순"),
}

private fun List<JjikmukProductCardUiModel>.sortedBy(
    sortOption: ProductSortOption,
): List<JjikmukProductCardUiModel> =
    when (sortOption) {
        ProductSortOption.Recommend -> this
        ProductSortOption.Popular -> sortedBy { product -> product.brand }
        ProductSortOption.Review -> sortedByDescending { product -> product.name.length }
        ProductSortOption.LowPrice -> sortedBy { product -> product.name }
        ProductSortOption.HighPrice -> sortedByDescending { product -> product.name }
    }

private enum class ProductDestination {
    Overview,
    RecommendationList,
    Search,
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ProductScreenPreview() {
    JjikmukTheme {
        ProductScreen(
            selectedTab = MainTab.Product,
            onTabClick = {},
            onScannerClick = {},
        )
    }
}
