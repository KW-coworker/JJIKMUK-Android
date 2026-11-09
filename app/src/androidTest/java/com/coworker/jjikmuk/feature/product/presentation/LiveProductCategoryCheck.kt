package com.coworker.jjikmuk.feature.product.presentation

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.MainActivity
import java.io.File
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in only: runs the installed app against its configured server. Credentials are never in the APK. */
class LiveProductCategoryCheck {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun exists(text:String, substring:Boolean=false)=rule.onAllNodesWithText(text,substring=substring).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(check:()->Boolean)=rule.waitUntil(60000,check)
    private fun shot(name:String) {
        val dir=File(rule.activity.filesDir,"category-review").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun actualServerCategorySearchAndDetail() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveProductCategory")=="true")
        waitFor { exists("로그인") || rule.onAllNodesWithContentDescription("MY").fetchSemanticsNodes().isNotEmpty() }
        if (exists("로그인")) {
            if (rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size < 2) {
                rule.onNodeWithText("로그인").performClick()
                waitFor { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2 }
            }
            val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat /data/local/tmp/jjikmuk-category-login.json")
            val json=ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { JSONObject(it.readText()) }
            rule.onAllNodes(hasSetTextAction())[0].performTextClearance()
            rule.onAllNodes(hasSetTextAction())[0].performTextInput(json.getString("email"))
            rule.onAllNodes(hasSetTextAction())[1].performTextClearance()
            rule.onAllNodes(hasSetTextAction())[1].performTextInput(json.getString("password"))
            closeSoftKeyboard(); rule.onNodeWithText("로그인").performClick()
        }
        waitFor { rule.onAllNodesWithContentDescription("Product").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Product").performClick()
        waitFor { exists("과자·스낵") }
        shot("live-page-1")
        val args=InstrumentationRegistry.getArguments()
        val categoryLabel=args.getString("categoryLabel") ?: "유제품"
        val query=args.getString("query") ?: "우유"
        if (runCatching { rule.onNodeWithText(categoryLabel).assertIsDisplayed() }.isFailure)
            rule.onNodeWithTag("category-pager").performTouchInput { swipeLeft() }
        rule.onNodeWithText(categoryLabel).assertIsDisplayed()
        shot("live-category-page")
        rule.onNodeWithText(categoryLabel).performClick()
        val card=SemanticsMatcher("catalog product card") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("catalog-product-") == true
        }
        waitFor { rule.onAllNodes(card).fetchSemanticsNodes().isNotEmpty() }
        shot("live-dairy-results")
        if (query.isNotEmpty()) {
            rule.onNode(hasSetTextAction()).performTextInput(query)
            rule.onNode(hasSetTextAction()).performImeAction(); closeSoftKeyboard()
            waitFor { exists("$categoryLabel · $query",true) && !exists("검색 중",true) }
            waitFor { rule.onAllNodes(card).fetchSemanticsNodes().isNotEmpty() }
            shot("live-category-keyword-results")
        }
        rule.onAllNodes(card).onFirst().performClick()
        waitFor { exists("이 상품 물어보기") }
        shot("live-product-detail")
    }
}
