package com.coworker.jjikmuk.livecheck

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.coworker.jjikmuk.MainActivity
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileInputStream
import android.os.ParcelFileDescriptor

/** Temporary device-only check against the real app and real AWS; no fake UI or repositories. */
class LiveChatTargetsUiCheck {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val args get() = InstrumentationRegistry.getArguments()
    private fun exists(text:String, substring:Boolean=false) = rule.onAllNodesWithText(text,substring=substring).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(timeout:Long=45000, check:()->Boolean) { rule.waitUntil(timeout,check) }
    private fun texts(node:SemanticsNode):List<String> = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + node.children.flatMap { texts(it) }
    private fun shot(name:String) {
        val dir=File(rule.activity.filesDir,"live-chat-check").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        File(dir,"$name.txt").writeText(texts(rule.onRoot(useUnmergedTree=true).fetchSemanticsNode()).joinToString("\n\n"))
    }
    private fun question() {
        rule.onNode(hasSetTextAction()).performTextInput("포키 먹어도 돼?")
        rule.onNode(hasSetTextAction()).performImeAction()
        closeSoftKeyboard()
        waitFor(90000) { !exists("답변을 준비 중이에요.") }
        if (exists("포키")) {
            rule.onAllNodesWithText("포키").onFirst().performClick()
            waitFor(90000) { !exists("답변을 준비 중이에요.") && rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        }
        waitFor(90000) { !exists("답변을 준비 중이에요.") && rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        assertFalse("Server request failed",exists("요청을 처리하지 못했어요",true))
        assertFalse("Targets unavailable",exists("분석 대상을 불러오지 못했어요",true))
        assertTrue("No visible product answer",exists("우유",true) || exists("알레르기",true))
    }
    @Test fun actualLoginFamilyToggleChatAndNone() {
        org.junit.Assume.assumeTrue(args.getString("liveChatTargets") == "true")
        val milk=args.getString("milkName")!!; val peanut=args.getString("peanutName")!!
        waitFor { exists("로그인") || rule.onAllNodesWithContentDescription("MY").fetchSemanticsNodes().isNotEmpty() }
        shot("00-auth-entry")
        if (exists("로그인")) {
            if (rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size < 2) {
                rule.onNodeWithText("로그인").performClick()
                waitFor { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2 }
            }
            val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat /data/local/tmp/jjikmuk-ui-login.json")
            val json=ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { JSONObject(it.readText()) }
            rule.onAllNodes(hasSetTextAction())[0].performTextClearance()
            rule.onAllNodes(hasSetTextAction())[0].performTextInput(json.getString("email"))
            rule.onAllNodes(hasSetTextAction())[1].performTextClearance()
            rule.onAllNodes(hasSetTextAction())[1].performTextInput(json.getString("password"))
            closeSoftKeyboard()
            rule.onNodeWithText("로그인").performClick()
        }
        waitFor { rule.onAllNodesWithContentDescription("MY").fetchSemanticsNodes().isNotEmpty() }
        shot("01-home-authenticated")
        rule.onNodeWithContentDescription("MY").performClick()
        rule.onNodeWithText("설정").performClick()
        waitFor { exists(milk) && exists(peanut) }
        shot("02-server-family-list")
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.onNodeWithContentDescription("Chat").performClick()
        rule.onNodeWithContentDescription("새 대화").performClick()
        waitFor { exists("메세지를 입력하세요") }
        rule.onNodeWithContentDescription("분석 대상 선택").performClick()
        rule.onNodeWithText(milk).performScrollTo().performClick()
        rule.onNodeWithText(peanut).performScrollTo().performClick()
        shot("03-selected-families")
        rule.onNodeWithText("×").performClick()
        waitFor { exists(milk,true) && exists(peanut,true) }
        question()
        assertTrue("Milk profile missing from response", exists("[$milk]", true))
        assertTrue("Allergy conflict missing", exists("직접 충돌", true))
        shot("04-personalized-answer")
        rule.onNodeWithContentDescription("분석 대상 선택").performClick()
        rule.onNodeWithText("나").performScrollTo().performClick()
        rule.onNodeWithText(milk).performScrollTo().performClick()
        rule.onNodeWithText(peanut).performScrollTo().performClick()
        rule.onNodeWithText("×").performClick()
        waitFor { exists("분석 대상 없음 · 개인 조건 없이 조회해요.") }
        question()
        shot("05-none-answer")
        rule.onNodeWithContentDescription("분석 대상 선택").performClick()
        rule.onNodeWithText("나").performScrollTo().performClick()
        rule.onNodeWithText("×").performClick()
        waitFor { !exists("분석 대상 없음 · 개인 조건 없이 조회해요.") }
    }
}
