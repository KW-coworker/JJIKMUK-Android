package com.coworker.jjikmuk.feature.product.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.data.repository.ProductCategory
import com.coworker.jjikmuk.feature.product.presentation.CategoryUiState
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

private data class CategoryStyle(val id: String, val emoji: String, val background: Long, val caption: String? = null)
private val styles = listOf(
    CategoryStyle("snack", "🍪", 0xFFFFF7ED), CategoryStyle("convenience", "🍱", 0xFFFEF2F2),
    CategoryStyle("noodles", "🍜", 0xFFFEFCE8), CategoryStyle("beverage", "🥤", 0xFFEFF6FF),
    CategoryStyle("meat", "🥩", 0xFFFFF1F2), CategoryStyle("seafood", "🐟", 0xFFECFEFF),
    CategoryStyle("plant", "🌾", 0xFFF0FDF4), CategoryStyle("sides", "🥬", 0xFFFEF2F2),
    CategoryStyle("dairy", "🥛", 0xFFE0F2FE), CategoryStyle("dessert", "🍨", 0xFFFDF2F8),
    CategoryStyle("bakery", "🥐", 0xFFFEFCE8), CategoryStyle("condiments", "🧂", 0xFFECFDF5, "소스·조미료·식용유"),
    CategoryStyle("special", "💊", 0xFFF5F3FF, "특수영양·건강기능식품"), CategoryStyle("unclassified", "📦", 0xFFF3F4F6, "기타"),
)

@Composable
internal fun ProductCategorySection(state: CategoryUiState, onSelect: (ProductCategory) -> Unit,
    onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val categories = state.categories.sortedBy { category -> styles.indexOfFirst { it.id == category.id }.takeIf { it >= 0 } ?: Int.MAX_VALUE }
    val pages = categories.chunked(8)
    val pager = rememberPagerState { pages.size }
    Column(modifier.fillMaxWidth().height(300.dp).padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 0.dp)) {
        Text("카테고리별 찾기", style = JjikmukTheme.typography.titleL, color = JjikmukTheme.colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        when {
            state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null || categories.isEmpty() -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
                Text(state.error ?: "등록된 카테고리가 없어요.", style = JjikmukTheme.typography.bodyS)
                Text("다시 시도", Modifier.clickable(onClick = onRetry).padding(vertical = 12.dp), color = JjikmukTheme.colors.brand)
            }
            else -> {
                HorizontalPager(pager, modifier = Modifier.fillMaxWidth().height(208.dp).testTag("category-pager"),
                    verticalAlignment = Alignment.Top) { page ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages[page].chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth()) {
                                row.forEach { category ->
                                    val style = styles.firstOrNull { it.id == category.id }
                                    Column(Modifier.weight(1f).height(100.dp).clickable { onSelect(category) }
                                        .semantics { contentDescription = "${category.label} 카테고리" },
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(64.dp).shadow(2.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                                            .background(Color(style?.background ?: 0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                                            Text(style?.emoji ?: "📦", fontSize = 28.sp, lineHeight = 42.sp)
                                        }
                                        Text(style?.caption ?: category.label, fontSize = 11.sp, lineHeight = 14.sp,
                                            color = JjikmukTheme.colors.textPrimary, textAlign = TextAlign.Center, maxLines = 2)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 20.dp).semantics {
                    contentDescription = "카테고리 ${pager.currentPage + 1}/${pages.size} 페이지"
                }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    repeat(pages.size) { page ->
                        Box(Modifier.size(width = if (page == pager.currentPage) 18.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(6.dp)).background(if (page == pager.currentPage) JjikmukTheme.colors.brand else Color(0xFFD0D5DD)))
                    }
                }
            }
        }
    }
}
