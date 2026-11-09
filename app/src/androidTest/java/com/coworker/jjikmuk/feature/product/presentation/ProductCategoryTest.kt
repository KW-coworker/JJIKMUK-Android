package com.coworker.jjikmuk.feature.product.presentation

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.coworker.jjikmuk.data.repository.ProductCategory
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.component.ProductCategorySection
import com.coworker.jjikmuk.feature.product.presentation.component.ProductSearchResultContent
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class ProductCategoryTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val categories=listOf("snack" to "과자·스낵", "convenience" to "간편·즉석식품", "noodles" to "면류", "beverage" to "음료",
        "meat" to "육류·가공육", "seafood" to "수산물·해조류", "plant" to "농산·곡물·두부류", "sides" to "김치·절임·반찬",
        "dairy" to "유제품", "dessert" to "디저트·빙과", "bakery" to "베이커리·떡", "condiments" to "소스·조미료·식용유",
        "special" to "특수영양·건강기능식품", "unclassified" to "미분류").map { ProductCategory(it.first,it.second) }
    private fun shot(name:String) {
        val dir=File(rule.activity.filesDir,"category-review").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun swipeShowsSecondPageAndUsesServerId() {
        var selected:ProductCategory?=null
        rule.setContent { JjikmukTheme { ProductCategorySection(CategoryUiState(categories),{ selected=it },{}) } }
        rule.onNodeWithContentDescription("카테고리 1/2 페이지").assertExists()
        shot("page-1")
        rule.onNodeWithTag("category-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("카테고리 2/2 페이지").assertExists()
        rule.onNodeWithText("기타").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("unclassified",selected?.id) }
        shot("page-2")
        rule.onNodeWithTag("category-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText("과자·스낵").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("snack",selected?.id) }
    }
    @Test fun metadataFailureCanBeRetriedWithoutFakeCategories() {
        var retries=0
        rule.setContent { JjikmukTheme { ProductCategorySection(CategoryUiState(error="카테고리 조회 실패"),{}, { retries++ }) } }
        rule.onNodeWithText("과자·스낵").assertDoesNotExist()
        rule.onNodeWithText("다시 시도").performClick()
        rule.runOnIdle { assertEquals(1,retries) }
    }
    @Test fun categoryResultsKeepProductSelectionAndRequestNextPage() {
        var selected:String?=null; var next=0
        val product=ProductSearchResult("880","테스트 과자","제조사",null,emptyList())
        rule.setContent { JjikmukTheme { ProductSearchResultContent(ProductSearchUiState(
            categoryId="snack",categoryLabel="과자·스낵",products=listOf(product),hasSearched=true,totalCount=21,hasNext=true),
            onProductClick={selected=it.barcode},onMoreProducts={next++}) } }
        rule.onNodeWithText("테스트 과자").performClick()
        rule.runOnIdle { assertEquals("880",selected) }
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("더보기"))
        rule.onNodeWithText("더보기").performClick()
        rule.runOnIdle { assertEquals(1,next) }
    }
}
