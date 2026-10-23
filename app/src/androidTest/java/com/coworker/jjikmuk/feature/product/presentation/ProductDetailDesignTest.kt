package com.coworker.jjikmuk.feature.product.presentation

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.data.repository.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.component.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File

/** Design-only fixtures never enter the runtime repositories. */
class ProductDetailDesignTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private fun product(status:ProductSafetyStatus=ProductSafetyStatus.Pass,weight:String="41g",energy:Double=229.8)=ProductDetail(
        barcode=weight,productName="해태 포키 블루베리",brandName="해태제과",
        imageUrl="android.resource://com.coworker.jjikmuk/${R.drawable.figma_product_reference}",
        allergyLabels=emptyList(),allergyWarning=null,
        rawMaterials="소맥분(미국산), 혼합식용유[팜올레인유(말레이시아산), 해바라기유(외국산)], 새우(국산, 미국산), 전분, 조미염, 미강유, 향미증진제, 땅콩, 우유 함유",
        totalWeight=weight,nutrition=ProductNutrition(energy,26.7,12.7,6.3,null,null,null),
        macroPercents=ProductMacroPercents(55.0,15.0,30.0),safetyStatus=status,
        dangerousIngredients=if(status==ProductSafetyStatus.Danger) listOf("새우") else emptyList(),analysisMessage=null,isDangerous=status==ProductSafetyStatus.Danger)
    private fun shot(name:String) {
        rule.waitForIdle()
        val dir=File(rule.activity.filesDir,"detail-design-review").apply{mkdirs()}
        File(dir,"$name.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun capacitySelectionUpdatesEnergyAndSafety() {
        val small=product(weight="25g",energy=140.0)
        val large=product(ProductSafetyStatus.Danger)
        var selected="large"
        rule.setContent {
            var state by remember { mutableStateOf(ProductDetailUiState(referenceBarcode="fixture",product=large,selectedCapacityKey="large",
                capacities=listOf(ProductCapacity("small","25g",25.0,"g",small),ProductCapacity("large","41g",41.0,"g",large)))) }
            JjikmukTheme { ProductDetailDesign(state,false,{},onCapacity={key->
                selected=key;state=state.copy(selectedCapacityKey=key,product=state.capacities.first{it.key==key}.product)
            },onAsk={},onAlternative={},onMoreAlternatives={},onRetryAlternatives={}) }
        }
        rule.onNodeWithContentDescription("중량 선택 펼치기").performScrollTo().performClick()
        rule.onNodeWithText("25g").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("small",selected) }
        rule.onAllNodesWithText("열량 약 140.0kcal").onFirst().assertExists()
        shot("capacity-expanded")
    }
    @Test fun safe()=render(ProductSafetyStatus.Pass,"safe")
    @Test fun unknown()=render(ProductSafetyStatus.Unknown,"unknown")
    @Test fun danger()=render(ProductSafetyStatus.Danger,"danger")
    private fun render(status:ProductSafetyStatus,name:String) {
        val p=product(status)
        rule.setContent { JjikmukTheme {
            Scaffold(topBar={JjikmukTopAppBar(leading=JjikmukTopAppBarLeading.Back({}))},
                bottomBar={JjikmukBottomNavigationBar(MainTab.Product,{})}) { padding ->
                ProductDetailDesign(ProductDetailUiState(product=p,referenceBarcode="fixture"),true,{}, {}, {}, {}, {}, {},Modifier.padding(padding))
            }
        } }
        rule.onNodeWithText("해태 포키 블루베리").assertExists()
        shot(name)
    }
    @Test fun alternateGridUsesRealItemCountAndOpensSelectedBarcode() {
        var clicked:String?=null
        val p=product(ProductSafetyStatus.Danger)
        val items=listOf(ProductAlternative(ProductSearchResult("a","글루텐프리 유기농 쌀과자","아이얌",null,emptyList()),false),
            ProductAlternative(ProductSearchResult("b","비건 초코 아몬드 쿠키","널담",null,emptyList()),true))
        rule.setContent { JjikmukTheme {
            ProductAlternativeList(ProductDetailUiState(product=p,alternatives=items),{clicked=it},{})
        } }
        rule.onNodeWithText("대체 상품 2개").assertExists()
        rule.onNodeWithText("표시 확인 필요").assertExists()
        rule.onNodeWithText("글루텐프리 유기농 쌀과자").performClick()
        rule.runOnIdle { assertEquals("a",clicked) }
        shot("alternatives")
    }
}
