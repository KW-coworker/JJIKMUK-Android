package com.coworker.jjikmuk.feature.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.feature.chat.presentation.component.ProductCandidatePicker
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProductCandidateMoreTest {
    @get:Rule val rule = createComposeRule()
    private fun candidate(index: Int) = ChatProductCandidate("상품 $index", "제조사", "880$index", null, emptyList(), null)

    @Test fun twoCandidatesKeepConfirmAndDismiss() {
        var dismissed = false
        rule.setContent { JjikmukTheme { ProductCandidatePicker(listOf(candidate(1), candidate(2)), {}, { dismissed = true }) } }
        rule.onNodeWithText("더보기").assertDoesNotExist()
        rule.onNodeWithText("확인").performClick()
        rule.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun moreOpensAllCandidatesAndSelectsBeyondFirstTwo() {
        var selected: ChatProductCandidate? = null
        val candidates = (1..15).map(::candidate)
        rule.setContent { JjikmukTheme { ProductCandidatePicker(candidates, { selected = it }, {}) } }
        rule.onNodeWithText("상품 3").assertDoesNotExist()
        rule.onNodeWithText("더보기").performClick()
        rule.onNodeWithText("상품 선택").assertIsDisplayed()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("상품 15"))
        rule.onNodeWithText("상품 15").performClick()
        rule.runOnIdle { assertEquals(candidates.last(), selected) }
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.onNodeWithText("더보기").assertIsDisplayed()
    }
}
