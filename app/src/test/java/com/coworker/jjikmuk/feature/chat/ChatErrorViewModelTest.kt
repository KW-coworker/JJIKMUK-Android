package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.feature.chat.presentation.chatErrorFor
import org.junit.Assert.*
import org.junit.Test

class ChatErrorViewModelTest {
    @Test fun `error codes map to specified copy and retry policy`() {
        data class Expected(val title: String, val description: String, val retryable: Boolean)
        val network = Expected("인터넷에 연결되지 않았어요", "연결을 확인한 뒤 다시 시도해 주세요.", true)
        val server = Expected("답변을 가져오지 못했어요", "잠시 후 다시 시도해 주세요.", true)
        val unavailable = Expected("분석 대상을 불러오지 못했어요", "다시 시도하면 가족 목록을 다시 불러와요.", true)
        val changed = Expected("분석 대상이 바뀌었어요", "새 대상 기준으로 다시 질문해 주세요.", false)
        val family = Expected("선택한 가족을 찾을 수 없어요", "분석 대상을 다시 선택해 주세요.", false)
        val login = Expected("다시 로그인해 주세요", "로그인 정보가 만료됐어요.", false)
        val cases = mapOf(
            "NETWORK_UNAVAILABLE" to network,
            "CHAT_REQUEST_FAILED" to server, "INTERNAL_SERVER_ERROR" to server, null to server,
            "ANALYSIS_SELECTION_UNAVAILABLE" to unavailable,
            "ANALYSIS_SELECTION_CHANGED" to changed,
            "FAMILY_NOT_FOUND" to family, "CHAT_ANALYSIS_SELECTION_UNSUPPORTED" to family, "ANALYSIS_SELECTION_INVALID" to family,
            "AUTH_TOKEN_EXPIRED" to login, "AUTH_TOKEN_INVALID" to login, "AUTHENTICATION_REQUIRED" to login,
            "REFRESH_TOKEN_INVALID" to login, "REFRESH_TOKEN_REUSED" to login,
        )
        cases.forEach { (code, expected) ->
            val error = chatErrorFor(code)
            assertEquals(code, expected, Expected(error.title, error.description, error.retryable))
            assertFalse(code, error.isRetrying)
        }
    }
}
